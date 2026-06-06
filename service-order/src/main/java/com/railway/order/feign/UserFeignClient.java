package com.railway.order.feign;

import com.railway.common.model.R;
import com.railway.order.feign.dto.PassengerVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign 调 service-user（按 ids 批量查乘车人）。
 * <p>走 service-user 的 {@code /passengers}，通过 query 传 ids。
 * 实际 service-user 的 PassengerController 用 {@code PageQuery} 接收；demo 阶段简化为
 * 直接传 list。
 */
@FeignClient(name = "service-user", path = "/passengers",
        fallbackFactory = UserFeignClientFallback.class)
public interface UserFeignClient {

    /**
     * 按 ids 批量查乘车人。
     * <p>service-user 端没有按 ids 查的接口，demo 阶段 fallback：返回空 list，
     * 上层需自行处理（从 service-user /passengers 拉所有再 filter）。
     */
    @GetMapping
    R<List<PassengerVO>> listByIds(@RequestParam("ids") List<Long> ids);
}
