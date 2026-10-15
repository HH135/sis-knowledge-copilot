package com.acme.svc.payment;

import org.springframework.stereotype.Service;

@Service
public class CouponService {
    private final PaymentDao paymentDao;

    public CouponService(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
    }

    public void cancel(String redemptionNo) {
        paymentDao.cancelCouponRedemption(redemptionNo);
    }
}
