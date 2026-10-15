package com.acme.svc.repairorder;

public class RepairOrderPolicy {
    public void requireOrderNumber(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("주문번호가 필요합니다.");
        }
    }
}
