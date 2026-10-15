package com.acme.svc.appointment;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppointmentDao {
    int register(@Param("appointmentNo") String appointmentNo, @Param("customerNo") String customerNo,
                 @Param("vin") String vin, @Param("appointmentAt") String appointmentAt);

    int confirm(@Param("appointmentNo") String appointmentNo, @Param("bayNo") String bayNo);
}
