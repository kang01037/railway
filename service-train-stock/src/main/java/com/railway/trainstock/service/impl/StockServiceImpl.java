package com.railway.trainstock.service.impl;

import com.railway.common.constant.RedisKeyConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.trainstock.dto.request.ConfirmDTO;
import com.railway.trainstock.dto.request.OccupyDTO;
import com.railway.trainstock.dto.request.ReleaseDTO;
import com.railway.trainstock.entity.StockFlowDO;
import com.railway.trainstock.entity.TrainSeatStockDO;
import com.railway.trainstock.mapper.StockFlowMapper;
import com.railway.trainstock.mapper.TrainSeatStockMapper;
import com.railway.trainstock.manager.StockManager;
import com.railway.trainstock.service.StockService;
import com.railway.trainstock.vo.OccupyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 库存服务实现。
 *
 * <h3>并发控制方案：Redis 分布式锁 + Lua 脚本</h3>
 * <p>Lua 脚本内置 SETNX 分布式锁 + 库存原子操作，保证同一座位类型同一时刻只有一个请求在操作库存。
 * DB 只做无条件落库（remain 增减），不再依赖乐观锁 version 校验。
 *
 * <h3>预占流程</h3>
 * <ol>
 *   <li>懒加载 Redis key：不存在则从 DB 加载 remain 并 SETNX（1 天 TTL）。</li>
 *   <li>Lua 脚本（内置分布式锁 + 原子扣减）→ 失败抛对应 BizException。</li>
 *   <li>写 stock_flow（bizType=1, delta=-num）。</li>
 *   <li>DB 无条件扣减（remain &gt;= num），分布式锁已保证互斥，无需 version。</li>
 * </ol>
 *
 * <h3>释放流程</h3>
 * <ol>
 *   <li>Lua 脚本（内置分布式锁 + 原子回滚 INCRBY，幂等：key 不存在视为成功）。</li>
 *   <li>写 stock_flow（bizType=3, delta=+num）。</li>
 *   <li>DB 无条件回滚（remain + num）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockManager stockManager;
    private final TrainSeatStockMapper stockMapper;
    private final StockFlowMapper stockFlowMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdWorker snowflakeIdWorker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OccupyVO occupy(OccupyDTO dto) {
        // 1. 懒加载 Redis（从 DB 初始化库存 key）
        TrainSeatStockDO dbStock = stockMapper.selectByKey(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
        if (dbStock == null) {
            throw new BizException(ErrorCode.STOCK_NOT_FOUND, "座位类型库存不存在");
        }
        stockManager.ensureStockKey(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dbStock.getRemain());

        // 2. Lua 脚本（分布式锁 + 原子预占）
        long luaResult = stockManager.occupy(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum());
        if (luaResult == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        if (luaResult == -1) {
            throw new BizException(ErrorCode.STOCK_NOT_FOUND, "Redis 库存 key 不存在");
        }
        if (luaResult == -2) {
            throw new BizException(ErrorCode.STOCK_VERSION_CONFLICT, "获取分布式锁超时，请重试");
        }

        // 3. 写流水
        saveFlow(dto.getOrderNo(), dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(),
                -dto.getNum(), (byte) 1);

        // 4. DB 无条件扣减（分布式锁已保证互斥，无需乐观锁）
        int affected = stockMapper.decreaseRemain(
                dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum());
        if (affected == 0) {
            // 极端情况：DB 与 Redis 数据不一致，回滚 Redis
            stockManager.release(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum());
            log.warn("DB 扣减失败（remain 不足）trainNo={} seat={} num={}", dto.getTrainNo(), dto.getSeatType(), dto.getNum());
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "DB 库存不足，数据可能不一致");
        }

        Long remain = stockManager.getCurrent(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
        log.info("预占成功 trainNo={} seat={} num={} remain={}", dto.getTrainNo(), dto.getSeatType(), dto.getNum(), remain);
        return new OccupyVO(
                RedisKeyConstant.stockKey(dto.getTrainNo(), dto.getRunDate().toString(), dto.getSeatType()),
                remain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(ConfirmDTO dto) {
        // 1. 写流水
        saveFlow(dto.getOrderNo(), dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(),
                -dto.getNum(), (byte) 2);
        // 2. Redis / DB 均已在 occupy 阶段扣减，不再变动
        log.info("确认扣减 orderNo={} trainNo={} seat={} num={}",
                dto.getOrderNo(), dto.getTrainNo(), dto.getSeatType(), dto.getNum());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(ReleaseDTO dto) {
        // 1. 查 DB 确认库存记录存在
        TrainSeatStockDO dbStock = stockMapper.selectByKey(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
        if (dbStock == null) {
            throw new BizException(ErrorCode.STOCK_NOT_FOUND, "座位类型库存不存在");
        }
        // 2. Redis 懒加载
        stockManager.ensureStockKey(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dbStock.getRemain());

        // 3. Lua 脚本（分布式锁 + 原子释放）
        long luaResult = stockManager.release(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum());
        // key 不存在也视为成功（幂等）
        if (luaResult == -1) {
            log.warn("Redis key 不存在，视为幂等成功 key=STOCK:{}:{}:{}",
                    dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
        }
        if (luaResult == -2) {
            throw new BizException(ErrorCode.STOCK_VERSION_CONFLICT, "获取分布式锁超时，请重试");
        }

        // 4. 写流水
        saveFlow(dto.getOrderNo(), dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(),
                +dto.getNum(), (byte) 3);

        // 5. DB 无条件回滚（分布式锁已保证互斥，无需乐观锁）
        int affected = stockMapper.increaseRemain(
                dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum());
        if (affected == 0) {
            log.warn("DB 回滚失败（记录不存在）trainNo={} seat={}", dto.getTrainNo(), dto.getSeatType());
        }
        log.info("释放成功 orderNo={} trainNo={} seat={} num={}",
                dto.getOrderNo(), dto.getTrainNo(), dto.getSeatType(), dto.getNum());
    }

    private void saveFlow(String orderNo, String trainNo, java.time.LocalDate runDate,
                          String seatType, int delta, byte bizType) {
        StockFlowDO flow = new StockFlowDO();
        flow.setId(snowflakeIdWorker.nextId());
        flow.setOrderNo(orderNo);
        flow.setTrainNo(trainNo);
        flow.setRunDate(runDate);
        flow.setSeatType(seatType);
        flow.setDelta(delta);
        flow.setBizType((int) bizType);
        flow.setCreateTime(LocalDateTime.now());
        stockFlowMapper.insert(flow);
    }
}
