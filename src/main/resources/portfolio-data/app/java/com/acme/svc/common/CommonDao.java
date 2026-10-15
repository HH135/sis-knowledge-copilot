package com.acme.svc.common;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommonDao {
    int writeLog(@Param("messageType") String messageType, @Param("correlationId") String correlationId,
                 @Param("status") String status, @Param("summary") String summary);

    int retryInterfaceLog(@Param("logNo") String logNo, @Param("correlationId") String correlationId);
}
