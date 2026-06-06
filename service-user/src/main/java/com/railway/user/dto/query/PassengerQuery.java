package com.railway.user.dto.query;

import com.railway.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 乘车人分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PassengerQuery extends PageQuery {

    /** 可选：按 passengerType 过滤 */
    private Integer passengerType;
}
