package com.railway.user.mapper;

import com.railway.user.entity.PassengerDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface PassengerMapper {

    int insert(PassengerDO passenger);

    PassengerDO selectById(@Param("id") Long id);

    /** 当前用户下的所有乘车人（配合 PageHelper 分页） */
    List<PassengerDO> listByUserId(@Param("userId") Long userId);

    /** 把某用户的所有默认乘车人改为非默认（新增/更新默认乘车人时调用） */
    int clearDefaultByUserId(@Param("userId") Long userId);

    int updateById(PassengerDO passenger);

    int deleteById(@Param("id") Long id);

    /** 按 ID 列表批量查询（订单服务后续调用） */
    List<PassengerDO> listByIds(@Param("ids") List<Long> ids);
}
