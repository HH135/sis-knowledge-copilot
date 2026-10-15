package com.acme.svc.repairorder;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class RepairOrderController {
    private final RepairOrderService repairOrderService;

    public RepairOrderController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    @RequestMapping(value = "/repair-order/create", method = RequestMethod.POST)
    public String create(String orderNo, String appointmentNo) {
        repairOrderService.create(orderNo, appointmentNo);
        return "repairorder/list";
    }

    @RequestMapping(value = "/repair-order/complete", method = RequestMethod.POST)
    public String complete(String orderNo, String historyNo, String serviceDate, String summary,
                           String paymentNo, String couponNo, String customerNo, String redemptionNo,
                           String discountAmount) {
        repairOrderService.complete(orderNo, historyNo, serviceDate, summary, paymentNo, couponNo, customerNo,
                redemptionNo, discountAmount);
        return "repairorder/detail";
    }
}
