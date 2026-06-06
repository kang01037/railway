package com.railway.payment.mapper;

import com.railway.payment.entity.PayRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PayRecordMapper {

    int insert(PayRecordDO record);

    PayRecordDO selectById(@Param("id") Long id);

    PayRecordDO selectByPayNo(@Param("payNo") String payNo);

    PayRecordDO selectByOrderNo(@Param("orderNo") String orderNo);

    List<PayRecordDO> listByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 状态机更新：{@code WHERE pay_no = #{payNo} AND status = 0} → 强制从 0 改成目标状态。
     * 返回受影响行数（0 = 已处理过，幂等）。
     */
    int updateStatusFromPending(@Param("payNo") String payNo,
                                @Param("newStatus") int newStatus);
}
