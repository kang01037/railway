package com.railway.trainstock.task;

import com.railway.trainstock.entity.TrainSeatStockDO;
import com.railway.trainstock.manager.StockManager;
import com.railway.trainstock.mapper.TrainSeatStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 库存缓存预热任务。
 * <p>定时将库存数据加载到 Redis，避免首次查询穿透到数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockWarmupTask {

    private final TrainSeatStockMapper stockMapper;
    private final StockManager stockManager;

    /**
     * 服务启动完成后预热一次。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupOnStartup() {
        log.info("服务启动完成，开始预热库存缓存...");
        warmup();
    }

    /**
     * 每天凌晨 2 点定时预热。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void warmupScheduled() {
        log.info("定时任务开始，预热库存缓存...");
        warmup();
    }

    /**
     * 预热逻辑：查询所有库存数据，初始化到 Redis。
     */
    private void warmup() {
        try {
            // 查询未来 7 天的所有库存
            LocalDate today = LocalDate.now();
            int warmupDays = 7;
            int totalWarmed = 0;

            for (int i = 0; i < warmupDays; i++) {
                LocalDate runDate = today.plusDays(i);
                List<TrainSeatStockDO> stocks = stockMapper.listByRunDate(runDate);

                for (TrainSeatStockDO stock : stocks) {
                    boolean warmed = stockManager.ensureStockKey(
                            stock.getTrainNo(),
                            stock.getRunDate(),
                            stock.getSeatType(),
                            stock.getRemain()
                    );
                    if (warmed) {
                        totalWarmed++;
                        log.debug("预热库存成功 trainNo={} runDate={} seatType={} remain={}",
                                stock.getTrainNo(), stock.getRunDate(),
                                stock.getSeatType(), stock.getRemain());
                    }
                }
            }

            log.info("库存缓存预热完成，共预热 {} 条记录", totalWarmed);
        } catch (Exception e) {
            log.error("库存缓存预热失败", e);
        }
    }
}
