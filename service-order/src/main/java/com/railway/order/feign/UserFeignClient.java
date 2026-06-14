package com.railway.order.feign;

import com.railway.common.model.R;
import com.railway.order.feign.dto.PassengerVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign 调 service-user（按 ids 批量查乘车人）。
 * <p>走 service-user 的内部接口，身份证号不脱敏，供出票使用。
 */
@FeignClient(name = "service-user", path = "/passengers",
        fallbackFactory = UserFeignClientFallback.class)
public interface UserFeignClient {

    /**
     * 按 ids 批量查乘车人（内部接口，身份证号不脱敏）。
     */
    @GetMapping("/internal/list-by-ids")
    R<List<PassengerVO>> listByIds(@RequestParam("ids") List<Long> ids);
}
