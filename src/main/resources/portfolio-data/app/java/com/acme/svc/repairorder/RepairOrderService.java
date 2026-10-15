package com.acme.svc.repairorder;

import com.acme.svc.common.InterfaceLogger;
import org.springframework.stereotype.Service;

@Service
public class RepairOrderService {
    private final RepairOrderDao repairOrderDao;
    private final RepairOrderPolicy repairOrderPolicy;
    private final InterfaceLogger interfaceLogger;

    public RepairOrderService(RepairOrderDao repairOrderDao, RepairOrderPolicy repairOrderPolicy,
                              InterfaceLogger interfaceLogger) {
        this.repairOrderDao = repairOrderDao;
        this.repairOrderPolicy = repairOrderPolicy;
        this.interfaceLogger = interfaceLogger;
    }

    public void create(String orderNo, String appointmentNo) {
        repairOrderDao.create(orderNo, appointmentNo);
        interfaceLogger.write("ORDER_CREATED", orderNo);
    }

    public void complete(String orderNo, String historyNo, String serviceDate, String summary,
                         String paymentNo, String couponNo, String customerNo, String redemptionNo,
                         String discountAmount) {
        repairOrderPolicy.requireOrderNumber(orderNo);
        // 상태 변경 후 완료 처리 프로시저를 순서대로 실행한다.
        repairOrderDao.changeStatus(orderNo, "IN_PROGRESS");
        repairOrderDao.complete(orderNo, historyNo, serviceDate, summary, paymentNo, couponNo, customerNo,
                redemptionNo, discountAmount);
    }
}
