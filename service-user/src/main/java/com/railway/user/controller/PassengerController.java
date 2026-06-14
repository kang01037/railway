package com.railway.user.controller;

import com.github.pagehelper.PageInfo;
import com.railway.common.annotation.AuthIgnore;
import com.railway.common.model.R;
import com.railway.user.dto.query.PassengerQuery;
import com.railway.user.dto.request.PassengerReq;
import com.railway.user.service.PassengerService;
import com.railway.user.vo.PassengerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 乘车人 CRUD。**所有接口需要登录**（无 @AuthIgnore）。
 */
@Slf4j
@RestController
@RequestMapping("/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping
    public R<Long> create(@Valid @RequestBody PassengerReq req) {
        return R.ok(passengerService.create(req));
    }

    @GetMapping
    public R<PageInfo<PassengerVO>> page(PassengerQuery query) {
        return R.ok(passengerService.page(query));
    }

    @GetMapping("/{id}")
    public R<PassengerVO> getById(@PathVariable Long id) {
        return R.ok(passengerService.getById(id));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PassengerReq req) {
        passengerService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        passengerService.delete(id);
        return R.ok();
    }

    /**
     * 内部接口：按 ids 批量查乘车人（身份证号不脱敏，仅供服务间 Feign 调用）。
     */
    @AuthIgnore
    @GetMapping("/internal/list-by-ids")
    public R<List<PassengerVO>> listByIdsInternal(@RequestParam("ids") List<Long> ids) {
        return R.ok(passengerService.listByIdsInternal(ids));
    }
}
