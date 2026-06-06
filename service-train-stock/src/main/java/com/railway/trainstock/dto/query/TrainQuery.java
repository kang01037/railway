package com.railway.trainstock.dto.query;

import lombok.Data;

/**
 * 车次查询条件。所有字段可空。
 */
@Data
public class TrainQuery {

    private String startStation;
    private String endStation;

    /** 1=正常 0=停运；null=不过滤 */
    private Integer status;
}
