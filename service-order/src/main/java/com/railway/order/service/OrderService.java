package com.railway.order.service;

import com.github.pagehelper.PageInfo;
import com.railway.order.dto.request.CancelOrderDTO;
import com.railway.order.dto.request.CreateOrderDTO;
import com.railway.order.vo.CreateOrderResultVO;
import com.railway.order.vo.OrderDetailVO;
import com.railway.order.vo.OrderVO;

/**
 * 订单服务：创建 / 取消 / 查询。
 */
public interface OrderService {

    /**
     * 创建订单（**事务边界外**做 Feign 调用，里面仅做 DB 写）。
     * <p>主流程：
     * <ol>
     *   <li>校验 num ≤ 5、用户已登录。</li>
     *   <li>幂等（由 {@code @Idempotent} 切面处理）。</li>
     *   <li>生成 orderNo（雪花）。</li>
     *   <li>Feign 拿乘客信息（OrderUserManager）。</li>
     *   <li>Feign stock.occupy → 失败抛 STOCK_NOT_ENOUGH。</li>
     *   <li>DB 写 orders（status=0, expireTime=now+15min）。</li>
     *   <li>发 MQ order.delay（带 orderNo，TTL 15min）。</li>
     *   <li>Feign ticket.issue（失败 → 补偿 stock.release）。</li>
     *   <li>Feign payment.createPay（失败 → 不回滚，由前端用 payUrl 重试）。</li>
     *   <li>返回 {orderNo, payUrl, amount, expireTime}。</li>
     * </ol>
     */
    CreateOrderResultVO create(CreateOrderDTO dto, Long userId);

    /**
     * 取消订单（仅自己）。仅 status=0（待支付）可取消 → 状态 2。
     */
    void cancel(CancelOrderDTO dto, Long userId);

    /** 按 orderNo 查（仅自己）。 */
    OrderVO getByOrderNo(String orderNo, Long userId);

    /** 订单详情（order + 票列表）。 */
    OrderDetailVO getDetail(String orderNo, Long userId);

    /** 当前用户订单分页。 */
    PageInfo<OrderVO> listByUserId(Long userId, int pageNum, int pageSize);
}
