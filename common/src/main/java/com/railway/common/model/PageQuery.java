package com.railway.common.model;

import lombok.Data;

/**
 * 通用分页查询条件。
 *
 * <p>业务模块可继承本类添加业务过滤字段；与 PageHelper.startPage 配合使用。
 */
@Data
public class PageQuery {

    private int pageNum = 1;
    private int pageSize = 10;
    private String orderBy;
    private String orderDirection = "desc";
}
