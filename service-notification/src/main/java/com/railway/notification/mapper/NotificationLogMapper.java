package com.railway.notification.mapper;

import com.railway.notification.entity.NotificationLogDO;
import org.apache.ibatis.annotations.Param;


public interface NotificationLogMapper {

    int insert(NotificationLogDO entity);

    NotificationLogDO selectById(@Param("id") Long id);
}
