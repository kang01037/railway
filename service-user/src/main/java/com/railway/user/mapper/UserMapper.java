package com.railway.user.mapper;

import com.railway.user.entity.UserDO;
import org.apache.ibatis.annotations.Param;

/**
 * 用户表 Mapper（注解 + XML 混合）。
 *
 * <p>基础 CRUD 用 XML（见 {@code UserMapper.xml}），便于处理动态 SQL。
 * <p>Mapper 扫描由 common 模块的 {@code @MapperScan("com.railway.**.mapper")} 统一处理，不需要 @Mapper 注解。
 */
public interface UserMapper {

    /** 插入（雪花 ID 由调用方生成） */
    int insert(UserDO user);

    /** 按主键查询 */
    UserDO selectById(@Param("id") Long id);

    /** 按用户名查询 */
    UserDO selectByUsername(@Param("username") String username);

    /** 按主键更新（动态 SQL，空字段不更新） */
    int updateById(UserDO user);

    /** 按主键删除（实际场景禁用，仅供测试） */
    int deleteById(@Param("id") Long id);

    /** 按用户名计数（注册时查重） */
    int countByUsername(@Param("username") String username);
}
