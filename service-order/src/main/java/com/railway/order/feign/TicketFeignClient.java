package com.railway.order.feign;

import com.railway.common.model.R;
import com.railway.order.feign.dto.CancelTicketDTO;
import com.railway.order.feign.dto.ConfirmTicketDTO;
import com.railway.order.feign.dto.IssueTicketDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign 调 service-ticket（出票 / 确认 / 取消）。
 */
@FeignClient(name = "service-ticket", path = "/tickets",
        fallbackFactory = TicketFeignClientFallback.class)
public interface TicketFeignClient {

    @PostMapping("/issue")
    R<Void> issue(@RequestBody IssueTicketDTO dto);

    @PostMapping("/confirm")
    R<Void> confirm(@RequestBody ConfirmTicketDTO dto);

    @PostMapping("/cancel")
    R<Void> cancel(@RequestBody CancelTicketDTO dto);
}
