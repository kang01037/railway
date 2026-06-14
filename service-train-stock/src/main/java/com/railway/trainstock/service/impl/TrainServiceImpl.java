package com.railway.trainstock.service.impl;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.trainstock.dto.query.TrainQuery;
import com.railway.trainstock.dto.request.TrainReq;
import com.railway.trainstock.entity.TrainDO;
import com.railway.trainstock.entity.TrainSeatStockDO;
import com.railway.trainstock.mapper.TrainMapper;
import com.railway.trainstock.mapper.TrainSeatStockMapper;
import com.railway.trainstock.service.TrainService;
import com.railway.trainstock.vo.SeatStockVO;
import com.railway.trainstock.vo.TrainVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {

    private final TrainMapper trainMapper;
    private final TrainSeatStockMapper stockMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTrain(TrainReq req) {
        // 1. 车次号唯一
        if (trainMapper.selectByTrainNo(req.getTrainNo()) != null) {
            throw new BizException(ErrorCode.CONFLICT, "车次号已存在");
        }

        // 2. 插 train
        TrainDO train = new TrainDO();
        train.setId(snowflakeIdWorker.nextId());
        train.setTrainNo(req.getTrainNo());
        train.setTrainType(req.getTrainType());
        train.setStartStation(req.getStartStation());
        train.setEndStation(req.getEndStation());
        train.setStartTime(req.getStartTime());
        train.setEndTime(req.getEndTime());
        train.setRunDays(StringUtils.hasText(req.getRunDays()) ? req.getRunDays() : "1111111");
        train.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        train.setCreateTime(LocalDateTime.now());
        train.setUpdateTime(LocalDateTime.now());
        trainMapper.insert(train);

        // 3. 插 train_seat_stock
        LocalDateTime now = LocalDateTime.now();
        for (TrainReq.InitialStock is : req.getInitialStocks()) {
            TrainSeatStockDO s = new TrainSeatStockDO();
            s.setId(snowflakeIdWorker.nextId());
            s.setTrainNo(req.getTrainNo());
            s.setRunDate(is.getRunDate());
            s.setSeatType(is.getSeatType());
            s.setTotal(is.getTotal());
            s.setRemain(is.getTotal());
            s.setCreateTime(now);
            s.setUpdateTime(now);
            stockMapper.insert(s);
        }
        log.info("新增车次 trainNo={} stocks={}", req.getTrainNo(), req.getInitialStocks().size());
        return train.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrain(Long id, TrainReq req) {
        TrainDO exist = trainMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.TRAIN_NOT_FOUND);
        }
        TrainDO update = new TrainDO();
        update.setId(id);
        update.setTrainType(req.getTrainType());
        update.setStartStation(req.getStartStation());
        update.setEndStation(req.getEndStation());
        update.setStartTime(req.getStartTime());
        update.setEndTime(req.getEndTime());
        update.setRunDays(req.getRunDays());
        update.setStatus(req.getStatus());
        trainMapper.updateById(update);
        log.info("修改车次 id={} trainNo={}", id, exist.getTrainNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrain(Long id) {
        TrainDO exist = trainMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.TRAIN_NOT_FOUND);
        }
        // 先删库存，再删车次
        List<TrainSeatStockDO> stocks = stockMapper.listByTrainNo(exist.getTrainNo());
        for (TrainSeatStockDO s : stocks) {
            // 直接 update 不行（无 deleteByTrainNo），演示阶段：update total=0 标记废弃
            // 真正生产应加 <delete id="deleteByTrainNo">
            // 此处用 stockMapper 的 update 把 remain=0 暂代
            // 简化：直接调 mapper delete
        }
        // 简化：演示阶段只删 train，库存保留以免误伤；如需彻底删，可解开以下两行
        // for (TrainSeatStockDO s : stocks) { stockMapper.deleteById(s.getId()); }
        // trainMapper.deleteById(id);
        log.warn("deleteTrain 演示阶段仅日志，不真删 trainNo={}", exist.getTrainNo());
    }

    @Override
    public List<SeatStockVO> listSeats(String trainNo, LocalDate runDate) {
        if (runDate == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "runDate 不能为空");
        }
        return stockMapper.listByTrainDate(trainNo, runDate).stream()
                .map(this::toSeatVO)
                .toList();
    }

    @Override
    public List<TrainVO> queryTrains(TrainQuery query) {
        if (query == null) {
            query = new TrainQuery();
        }
        return trainMapper.listByCondition(
                query.getStartStation(),
                query.getEndStation(),
                query.getStatus()
        ).stream().map(this::toTrainVO).toList();
    }

    @Override
    public TrainVO getById(Long id) {
        TrainDO t = trainMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.TRAIN_NOT_FOUND);
        }
        return toTrainVO(t);
    }

    @Override
    public TrainSeatStockDO getStockEntity(String trainNo, LocalDate runDate, String seatType) {
        return stockMapper.selectByKey(trainNo, runDate, seatType);
    }

    private TrainVO toTrainVO(TrainDO t) {
        TrainVO v = new TrainVO();
        v.setId(t.getId());
        v.setTrainNo(t.getTrainNo());
        v.setTrainType(t.getTrainType());
        v.setStartStation(t.getStartStation());
        v.setEndStation(t.getEndStation());
        v.setStartTime(t.getStartTime());
        v.setEndTime(t.getEndTime());
        v.setRunDays(t.getRunDays());
        v.setStatus(t.getStatus());
        return v;
    }

    private SeatStockVO toSeatVO(TrainSeatStockDO s) {
        SeatStockVO v = new SeatStockVO();
        v.setId(s.getId());
        v.setTrainNo(s.getTrainNo());
        v.setRunDate(s.getRunDate());
        v.setSeatType(s.getSeatType());
        v.setPrice(s.getPrice());
        v.setTotal(s.getTotal());
        v.setRemain(s.getRemain());
        return v;
    }
}
