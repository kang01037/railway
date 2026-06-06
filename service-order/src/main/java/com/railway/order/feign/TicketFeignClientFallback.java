package com.railway.order.feign;

import com.railway.common.exception.ErrorCode;
import com.railway.common.model.R;
import com.railway.order.feign.dto.CancelTicketDTO;
import com.railway.order.feign.dto.ConfirmTicketDTO;
import com.railway.order.feign.dto.IssueTicketDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicketFeignClientFallback implements FallbackFactory<TicketFeignClient> {

    @Override
    public TicketFeignClient create(Throwable cause) {
        log.error("TicketFeignClient 降级 cause={}", cause.getMessage(), cause);
        return new TicketFeignClient() {
            @Override
            public R<Void> issue(IssueTicketDTO dto) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "票务服务暂不可用");
            }
            @Override
            public R<Void> confirm(ConfirmTicketDTO dto) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "票务服务暂不可用");
            }
            @Override
            public R<Void> cancel(CancelTicketDTO dto) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "票务服务暂不可用");
            }
        };
    }
}
