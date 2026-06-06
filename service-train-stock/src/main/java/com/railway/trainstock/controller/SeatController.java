package com.railway.trainstock.controller;

import com.railway.common.model.R;
import com.railway.trainstock.service.TrainService;
import com.railway.trainstock.vo.SeatStockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 座位库存查询（任意用户）。网关路径：{@code /api/train/trains/{no}/seats}。
 */
@Slf4j
@RestController
@RequestMapping("/trains")
@RequiredArgsConstructor
public class SeatController {

    private final TrainService trainService;

    @GetMapping("/{trainNo}/seats")
    public R<List<SeatStockVO>> listSeats(
            @PathVariable String trainNo,
            @RequestParam("runDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate runDate) {
        return R.ok(trainService.listSeats(trainNo, runDate));
    }
}
