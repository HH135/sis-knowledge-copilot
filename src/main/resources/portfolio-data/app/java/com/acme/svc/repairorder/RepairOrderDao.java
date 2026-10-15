package com.acme.svc.repairorder;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RepairOrderDao {
    int create(@Param("orderNo") String orderNo, @Param("appointmentNo") String appointmentNo);

    int changeStatus(@Param("orderNo") String orderNo, @Param("status") String status);

    int complete(@Param("orderNo") String orderNo, @Param("historyNo") String historyNo,
                 @Param("serviceDate") String serviceDate, @Param("summary") String summary,
                 @Param("paymentNo") String paymentNo, @Param("couponNo") String couponNo,
                 @Param("customerNo") String customerNo, @Param("redemptionNo") String redemptionNo,
                 @Param("discountAmount") String discountAmount);
}
