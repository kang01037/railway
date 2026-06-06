package com.railway.notification.service.impl;

import com.railway.common.util.SnowflakeIdWorker;
import com.railway.notification.entity.NotificationLogDO;
import com.railway.notification.mapper.NotificationLogMapper;
import com.railway.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知服务实现（demo 简化）。
 *
 * <p>两个 consumer 的差别只是文案 + type。**不调远程短信/邮件服务**，仅打日志 + 落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogMapper notificationLogMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    @Override
    public void onOrderPaid(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        Object amount  = msg.get("amount");
        Boolean success = (Boolean) msg.get("success");

        if (orderNo == null) {
            log.warn("order.paid 消息无 orderNo 字段 msg={}", msg);
            return;
        }
        if (!Boolean.TRUE.equals(success)) {
            log.info("支付失败 / 取消，order.paid 不发送通知 orderNo={}", orderNo);
            return;
        }

        String content = String.format("【铁路购票】您订单 %s 支付成功，金额 ¥%s，请提前到站取票。", orderNo, amount);
        log.info("📩 [SMS/EMAIL] {}", content);

        saveLog(toLog(0, orderNo, content));
    }

    @Override
    public void onOrderCancel(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        if (orderNo == null) {
            log.warn("order.cancel 消息无 orderNo 字段 msg={}", msg);
            return;
        }
        String content = String.format("【铁路购票】您订单 %s 已取消（超时未支付 / 主动取消），已释放库存。", orderNo);
        log.info("📩 [SMS/EMAIL] {}", content);

        saveLog(toLog(1, orderNo, content));
    }

    @Override
    public void saveLog(NotificationLogDO entity) {
        try {
            notificationLogMapper.insert(entity);
        } catch (Exception e) {
            // 落库失败不影响主流程（消息已被消费）
            log.error("通知日志落库失败 orderNo={} type={}", entity.getOrderNo(), entity.getType(), e);
        }
    }

    private NotificationLogDO toLog(int type, String orderNo, String content) {
        NotificationLogDO log = new NotificationLogDO();
        log.setId(snowflakeIdWorker.nextId());
        log.setOrderNo(orderNo);
        log.setType(type);
        log.setChannel("LOG");
        log.setReceiver("demo-user");
        log.setContent(content);
        log.setStatus(0);
        log.setRetryCount(0);
        log.setCreateTime(LocalDateTime.now());
        return log;
    }
}
