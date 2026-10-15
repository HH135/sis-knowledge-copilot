package com.acme.svc.payment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class PaymentController {
    private final PaymentService paymentService;
    private final CouponService couponService;

    public PaymentController(PaymentService paymentService, CouponService couponService) {
        this.paymentService = paymentService;
        this.couponService = couponService;
    }

    @RequestMapping(value = "/payment/register-and-confirm", method = RequestMethod.POST)
    public String registerAndConfirm(String paymentNo, String orderNo, String method, String amount,
                                     String couponNo, String customerNo, String redemptionNo) {
        paymentService.registerAndConfirm(paymentNo, orderNo, method, amount, couponNo, customerNo, redemptionNo);
        return "payment/form";
    }

    @RequestMapping(value = "/payment/coupon/cancel", method = RequestMethod.POST)
    public String cancelCoupon(String redemptionNo) {
        couponService.cancel(redemptionNo);
        return "payment/coupon";
    }
}
