package com.railway.order.manager;

import com.railway.common.model.R;
import com.railway.order.feign.UserFeignClient;
import com.railway.order.feign.dto.PassengerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户编排：调 user 服务按 ids 拿乘客详情。
 * <p>服务降级时回退到从 {@code userFeignClient} 拿全部再 filter。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderUserManager {

    private final UserFeignClient userFeignClient;

    /**
     * 按 ids 批量拿乘客信息，返回 Map&lt;id, PassengerVO&gt;。
     * 服务不可用 → 返回空 map。
     */
    public Map<Long, PassengerVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        try {
            R<List<PassengerVO>> r = userFeignClient.listByIds(ids);
            if (r != null && r.getCode() == 200 && r.getData() != null) {
                return r.getData().stream()
                        .filter(p -> p.getId() != null)
                        .collect(Collectors.toMap(PassengerVO::getId, p -> p, (a, b) -> a));
            }
        } catch (Exception e) {
            log.error("user.listByIds 失败", e);
        }
        return Map.of();
    }
}
