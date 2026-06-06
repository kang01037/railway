package com.railway.trainstock.controller;

import com.railway.common.annotation.AuthIgnore;
import com.railway.common.annotation.RequireRole;
import com.railway.common.model.R;
import com.railway.trainstock.dto.query.TrainQuery;
import com.railway.trainstock.dto.request.TrainReq;
import com.railway.trainstock.entity.TrainSeatStockDO;
import com.railway.trainstock.mapper.TrainSeatStockMapper;
import com.railway.trainstock.service.TrainService;
import com.railway.trainstock.vo.TrainVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 车次管理（管理员接口）。网关路径：{@code /api/train/trains/**}。
 */
@Slf4j
@RestController
@RequestMapping("/trains")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;
    private final TrainSeatStockMapper trainSeatStockMapper;

    @RequireRole("ADMIN")
    @PostMapping
    public R<Long> add(@Valid @RequestBody TrainReq req) {
        return R.ok(trainService.addTrain(req));
    }

    @RequireRole("ADMIN")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TrainReq req) {
        trainService.updateTrain(id, req);
        return R.ok();
    }

    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        trainService.deleteTrain(id);
        return R.ok();
    }

    /** 按 ID 查车次（任意用户可看） */
    @GetMapping("/{id}")
    public R<TrainVO> getById(@PathVariable Long id) {
        return R.ok(trainService.getById(id));
    }

    /** 条件查询车次（任意用户） */
    @GetMapping
    public R<List<TrainVO>> query(TrainQuery query) {
        return R.ok(trainService.queryTrains(query));
    }

    /**
     * 查某车次某天的座位余票明细（**service-search Feign 内部调用**）。
     * <p>返回 {seatType: {seatType, total, remain}}。
     * <p>{@code @AuthIgnore}：Feign 直连不经过 gateway，service-search 也无 token 透传。
     */
    @AuthIgnore
    @GetMapping("/{trainNo}/seats")
    public R<Map<String, Object>> getSeats(
            @PathVariable String trainNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate) {
        List<TrainSeatStockDO> list = trainSeatStockMapper.listByTrainDate(trainNo, runDate);
        Map<String, Object> result = new LinkedHashMap<>();
        for (TrainSeatStockDO s : list) {
            Map<String, Object> m = new LinkedHashMap<>(3);
            m.put("seatType", s.getSeatType());
            m.put("total", s.getTotal());
            m.put("remain", s.getRemain());
            result.put(s.getSeatType(), m);
        }
        return R.ok(result);
    }
}
