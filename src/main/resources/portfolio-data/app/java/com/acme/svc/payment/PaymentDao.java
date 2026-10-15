package com.acme.svc.payment;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentDao {
    int register(@Param("paymentNo") String paymentNo, @Param("orderNo") String orderNo,
                 @Param("method") String method, @Param("amount") String amount);

    int confirm(@Param("paymentNo") String paymentNo, @Param("couponNo") String couponNo,
                @Param("customerNo") String customerNo, @Param("redemptionNo") String redemptionNo);

    int cancel(@Param("paymentNo") String paymentNo);

    int cancelCouponRedemption(@Param("redemptionNo") String redemptionNo);
}
