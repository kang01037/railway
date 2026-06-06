package com.railway.trainstock.mapper;

import com.railway.trainstock.entity.StockFlowDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockFlowMapper {

    int insert(StockFlowDO flow);

    List<StockFlowDO> listByOrderNo(@Param("orderNo") String orderNo);
}
