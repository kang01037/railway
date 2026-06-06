package com.railway.search.controller;

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
import java.util.Map;

/**
 * 检索接口。网关路径：{@code /api/search/search/**}（gateway StripPrefix=2 → /search/**）。
 *
 * <ul>
 *   <li>{@code GET /search/trains?from=&to=&date=}</li>
 *   <li>{@code GET /search/trains/{trainNo}/seats?date=}</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/trains")
    public R<List<TrainIndex>> searchTrains(@RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(searchService.searchTrains(from, to, date));
    }

    @GetMapping("/trains/{trainNo}/seats")
    public R<Map<String, SeatRemainVO>> getSeats(@PathVariable String trainNo,
                                                 @RequestParam
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(searchService.getSeats(trainNo, date));
    }
}
