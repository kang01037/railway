package com.railway.order.feign;

import com.railway.common.exception.ErrorCode;
import com.railway.common.model.R;
import com.railway.order.feign.dto.PassengerVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UserFeignClientFallback implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("UserFeignClient 降级 cause={}", cause.getMessage(), cause);
        return new UserFeignClient() {
            @Override
            public R<List<PassengerVO>> listByIds(List<Long> ids) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "用户服务暂不可用");
            }
        };
    }
}
