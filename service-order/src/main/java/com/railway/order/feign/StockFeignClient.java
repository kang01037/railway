package com.railway.order.feign;

import com.railway.common.model.R;
import com.railway.order.feign.dto.ConfirmStockDTO;
import com.railway.order.feign.dto.OccupyStockDTO;
import com.railway.order.feign.dto.ReleaseStockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign 调 service-train-stock（库存预占/确认/释放）。
 * <p>走直连（不经过 gateway），由各服务端口访问。
 */
@FeignClient(name = "service-train-stock", path = "/stock",
        fallbackFactory = StockFeignClientFallback.class)
public interface StockFeignClient {

    @PostMapping("/occupy")
    R<Void> occupy(@RequestBody OccupyStockDTO dto);

    @PostMapping("/confirm")
    R<Void> confirm(@RequestBody ConfirmStockDTO dto);

    @PostMapping("/release")
    R<Void> release(@RequestBody ReleaseStockDTO dto);
}
