package com.railway.order.feign;

import com.railway.common.model.R;
import com.railway.order.feign.dto.CancelTicketDTO;
import com.railway.order.feign.dto.IssueByOrderNoDTO;
import com.railway.order.feign.dto.IssueTicketDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign 调 service-ticket（预占座 / 出票 / 取消）。
 */
@FeignClient(name = "service-ticket", path = "/tickets",
        fallbackFactory = TicketFeignClientFallback.class)
public interface TicketFeignClient {

    @PostMapping("/preAllocate")
    R<?> preAllocate(@RequestBody IssueTicketDTO dto);

    @PostMapping("/issue")
    R<?> issue(@RequestBody IssueByOrderNoDTO dto);

    @PostMapping("/cancel")
    R<?> cancel(@RequestBody CancelTicketDTO dto);
}
