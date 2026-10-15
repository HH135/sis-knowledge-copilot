package com.acme.svc.payment;

import com.acme.svc.common.InterfaceLogger;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final PaymentDao paymentDao;
    private final InterfaceLogger interfaceLogger;

    public PaymentService(PaymentDao paymentDao, InterfaceLogger interfaceLogger) {
        this.paymentDao = paymentDao;
        this.interfaceLogger = interfaceLogger;
    }

    public void registerAndConfirm(String paymentNo, String orderNo, String method, String amount,
                                   String couponNo, String customerNo, String redemptionNo) {
        // 결제 등록과 확정을 차례대로 호출하는 레거시 흐름 예시.
        paymentDao.register(paymentNo, orderNo, method, amount);
        paymentDao.confirm(paymentNo, couponNo, customerNo, redemptionNo);
        interfaceLogger.write("PAYMENT_CONFIRMED", orderNo);
    }
}
