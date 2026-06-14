package com.railway.user.service;

import com.github.pagehelper.PageInfo;
import com.railway.user.dto.query.PassengerQuery;
import com.railway.user.dto.request.PassengerReq;
import com.railway.user.vo.PassengerVO;

import java.util.List;

/**
 * 乘车人服务：CRUD。当前用户的乘车人列表 / 详情 / 新增 / 修改 / 删除。
 */
public interface PassengerService {

    /**
     * 新增乘车人，自动绑定到当前登录用户。
     */
    Long create(PassengerReq req);

    /**
     * 分页查询当前用户的乘车人。
     */
    PageInfo<PassengerVO> page(PassengerQuery query);

    /**
     * 查询单个乘车人（限当前用户）。
     */
    PassengerVO getById(Long id);

    /**
     * 修改（限当前用户）。
     */
    void update(Long id, PassengerReq req);

    /**
     * 删除（限当前用户）。
     */
    void delete(Long id);

    /**
     * 内部接口：按 ids 批量查乘车人（身份证号不脱敏，仅供服务间调用）。
     */
    List<PassengerVO> listByIdsInternal(List<Long> ids);
}
