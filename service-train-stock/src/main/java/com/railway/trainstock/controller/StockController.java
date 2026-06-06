package com.railway.trainstock.controller;

import com.railway.common.annotation.AuthIgnore;
import com.railway.common.model.R;
import com.railway.trainstock.dto.request.ConfirmDTO;
import com.railway.trainstock.dto.request.OccupyDTO;
import com.railway.trainstock.dto.request.ReleaseDTO;
import com.railway.trainstock.service.StockService;
import com.railway.trainstock.vo.OccupyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存内部接口（服务间调用）。
 * <p><b>全部 {@code @AuthIgnore}</b>：service-order 通过 Feign 调本服务时，
 * 不走 gateway 鉴权，由业务层（IP 白名单 / 内网）保证安全（demo 阶段省略）。
 *
 * <p>网关路径：{@code /api/train/stock/occupy|confirm|release}。
 */
@Slf4j
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @AuthIgnore
    @PostMapping("/occupy")
    public R<OccupyVO> occupy(@Valid @RequestBody OccupyDTO dto) {
        return R.ok(stockService.occupy(dto));
    }

    @AuthIgnore
    @PostMapping("/confirm")
    public R<Void> confirm(@Valid @RequestBody ConfirmDTO dto) {
        stockService.confirm(dto);
        return R.ok();
    }

    @AuthIgnore
    @PostMapping("/release")
    public R<Void> release(@Valid @RequestBody ReleaseDTO dto) {
        stockService.release(dto);
        return R.ok();
    }
}
