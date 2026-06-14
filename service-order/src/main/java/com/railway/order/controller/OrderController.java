package com.railway.order.controller;

import com.github.pagehelper.PageInfo;
import com.railway.common.annotation.AuthIgnore;
import com.railway.common.model.R;
import com.railway.common.util.UserContext;
import com.railway.order.aspect.Idempotent;
import com.railway.order.dto.request.CancelOrderDTO;
import com.railway.order.dto.request.CreateOrderDTO;
import com.railway.order.service.OrderService;
import com.railway.order.vo.CreateOrderResultVO;
import com.railway.order.vo.OrderDetailVO;
import com.railway.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。网关路径：{@code /api/order/orders/**}。
 *
 * <ul>
 *   <li>{@code POST /orders} 创建订单（需登录 + 幂等头 {@code X-Idempotent-Key}）</li>
 *   <li>{@code POST /orders/{orderNo}/cancel} 取消订单（需登录）</li>
 *   <li>{@code GET /orders/{orderNo}} 查订单</li>
 *   <li>{@code GET /orders} 查当前用户订单（分页）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Idempotent(ttlSeconds = 60)
    @PostMapping
    public R<CreateOrderResultVO> create(@Valid @RequestBody CreateOrderDTO dto) {
        Long userId = UserContext.mustCurrentUserId();
        return R.ok(orderService.create(dto, userId));
    }

    @PostMapping("/{orderNo}/cancel")
    public R<Void> cancel(@PathVariable String orderNo, @Valid @RequestBody CancelOrderDTO body) {
        Long userId = UserContext.mustCurrentUserId();
        body.setOrderNo(orderNo);
        orderService.cancel(body, userId);
        return R.ok();
    }

    @GetMapping("/{orderNo}")
    public R<OrderDetailVO> detail(@PathVariable String orderNo) {
        Long userId = UserContext.mustCurrentUserId();
        return R.ok(orderService.getDetail(orderNo, userId));
    }

    @GetMapping
    public R<PageInfo<OrderVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = UserContext.mustCurrentUserId();
        return R.ok(orderService.listByUserId(userId, pageNum, pageSize));
    }

    /**
     * 内部接口：支付成功后同步确认订单（0→1）。
     */
    @AuthIgnore
    @PostMapping("/internal/confirm")
    public R<Integer> confirm(@RequestParam String orderNo) {
        return R.ok(orderService.confirmByOrderNo(orderNo));
    }
}
