package com.railway.search.controller;

import com.railway.common.annotation.AuthIgnore;
import com.railway.common.model.R;
import com.railway.search.feign.dto.SeatRemainVO;
import com.railway.search.index.TrainIndex;
import com.railway.search.service.SearchService;
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
 * 检索接口。网关路径：{@code /api/search/search/**}（gateway StripPrefix=2 → /search/**）。
 *
 * <ul>
 *   <li>{@code GET /search/trains?from=&to=&date=}</li>
 *   <li>{@code GET /search/trains/{trainNo}/seats?date=}</li>
 * </ul>
 *
 * <p>{@code @AuthIgnore}：gateway 层已做鉴权，本服务接口允许匿名访问（适用于 service 间 Feign / 直连调试）。
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @AuthIgnore
    @GetMapping("/trains")
    public R<List<TrainIndex>> searchTrains(@RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(searchService.searchTrains(from, to, date));
    }

    @AuthIgnore
    @GetMapping("/trains/{trainNo}/seats")
    public R<List<SeatRemainVO>> getSeats(@PathVariable String trainNo,
                                          @RequestParam
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(searchService.getSeats(trainNo, date));
    }
}
