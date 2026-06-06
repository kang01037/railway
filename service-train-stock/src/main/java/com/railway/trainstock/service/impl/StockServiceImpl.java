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
 * <h3>预占流程</h3>
 * <ol>
 *   <li>懒加载 Redis key：不存在则从 DB 加载 remain 并 SETNX（1 天 TTL）。</li>
 *   <li>Lua 原子扣减 → 失败抛对应 BizException。</li>
 *   <li>写 stock_flow（bizType=1, delta=-num）。</li>
 *   <li>DB 乐观锁扣减（remain &gt;= num &amp;&amp; version 一致），失败回滚 Redis 并抛 STOCK_VERSION_CONFLICT。</li>
 * </ol>
 *
 * <h3>释放流程</h3>
 * <ol>
 *   <li>Lua 原子回滚（INCRBY，幂等：key 不存在视为成功）。</li>
 *   <li>写 stock_flow（bizType=3, delta=+num）。</li>
 *   <li>DB 乐观锁回滚。</li>
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
        // 1. 懒加载 Redis
        TrainSeatStockDO dbStock = stockMapper.selectByKey(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
        if (dbStock == null) {
            throw new BizException(ErrorCode.STOCK_NOT_FOUND, "座位类型库存不存在");
        }
        stockManager.ensureStockKey(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dbStock.getRemain());

        // 2. Lua 原子预占
        long luaResult = stockManager.occupy(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum());
        if (luaResult == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        if (luaResult == -1) {
            throw new BizException(ErrorCode.STOCK_NOT_FOUND, "Redis 库存 key 不存在");
        }

        // 3. 写流水
        saveFlow(dto.getOrderNo(), dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(),
                -dto.getNum(), (byte) 1);

        // 4. DB 乐观锁扣减
        int affected = stockMapper.decreaseRemainByVersion(
                dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum(), dbStock.getVersion());
        if (affected == 0) {
            // 失败：回滚 Redis（释放同样数量）
            stockManager.release(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum());
            log.warn("DB 乐观锁扣减冲突 trainNo={} seat={} num={}", dto.getTrainNo(), dto.getSeatType(), dto.getNum());
            throw new BizException(ErrorCode.STOCK_VERSION_CONFLICT, "DB 乐观锁冲突，请重试");
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
        // 1. 查 DB 拿 version
        TrainSeatStockDO dbStock = stockMapper.selectByKey(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
        if (dbStock == null) {
            throw new BizException(ErrorCode.STOCK_NOT_FOUND, "座位类型库存不存在");
        }
        // 2. Redis 懒加载
        stockManager.ensureStockKey(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dbStock.getRemain());
        // 3. Lua 原子释放
        long luaResult = stockManager.release(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum());
        // key 不存在也视为成功（幂等）
        if (luaResult == -1) {
            log.warn("Redis key 不存在，视为幂等成功 key=STOCK:{}:{}:{}",
                    dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
        }
        // 4. 写流水
        saveFlow(dto.getOrderNo(), dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(),
                +dto.getNum(), (byte) 3);
        // 5. DB 乐观锁回滚
        int affected = stockMapper.increaseRemainByVersion(
                dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), dto.getNum(), dbStock.getVersion());
        if (affected == 0) {
            log.warn("DB 乐观锁回滚失败（version 已变化）trainNo={} seat={}", dto.getTrainNo(), dto.getSeatType());
            // 不抛异常：Redis 已回滚成功，DB 用 SQL 直接补救（演示阶段简单处理）
            // 生产环境应该异步重试或对账
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
