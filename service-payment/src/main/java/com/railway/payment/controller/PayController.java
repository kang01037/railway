package com.railway.payment.controller;

import com.railway.common.annotation.AuthIgnore;
import com.railway.common.model.R;
import com.railway.payment.dto.request.CallbackDTO;
import com.railway.payment.dto.request.CreatePayDTO;
import com.railway.payment.service.PayService;
import com.railway.payment.vo.PayVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付接口。网关路径：{@code /api/payment/pay/**}。
 *
 * <ul>
 *   <li>{@code POST /pay/create} — 需登录（用户下单后调）</li>
 *   <li>{@code POST /pay/callback/sim} — {@code @AuthIgnore}（模拟支付回调，gateway 白名单）</li>
 *   <li>{@code GET /pay/{payNo}} — 需登录</li>
 *   <li>{@code GET /pay/order/{orderNo}} — 需登录</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    @AuthIgnore
    @PostMapping("/create")
    public R<PayVO> create(@Valid @RequestBody CreatePayDTO dto) {
        return R.ok(payService.createPay(dto));
    }

    /**
     * 模拟支付回调。生产应换成支付宝 / 微信的回调（验签后回调此结构）。
     * 幂等：已处理过会返回 affected=0。
     */
    @AuthIgnore
    @PostMapping("/callback/sim")
    public R<Map<String, Object>> callbackSim(@Valid @RequestBody CallbackDTO dto) {
        boolean processed = payService.callback(dto);
        Map<String, Object> data = new HashMap<>(2);
        data.put("payNo", dto.getPayNo());
        data.put("processed", processed);
        return R.ok(data);
    }

    @GetMapping("/{payNo}")
    public R<PayVO> getByPayNo(@PathVariable String payNo) {
        return R.ok(payService.getByPayNo(payNo));
    }

    @GetMapping("/order/{orderNo}")
    public R<List<PayVO>> listByOrderNo(@PathVariable String orderNo) {
        return R.ok(payService.listByOrderNo(orderNo));
    }
}
