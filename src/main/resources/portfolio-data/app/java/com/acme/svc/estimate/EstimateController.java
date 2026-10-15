package com.acme.svc.estimate;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class EstimateController {
    private final EstimateService estimateService;

    public EstimateController(EstimateService estimateService) {
        this.estimateService = estimateService;
    }

    @RequestMapping(value = "/estimate/create", method = RequestMethod.POST)
    public String create(String estimateNo, String orderNo, String laborAmount, String partAmount) {
        estimateService.create(estimateNo, orderNo, laborAmount, partAmount);
        return "estimate/form";
    }

    @RequestMapping(value = "/estimate/approve", method = RequestMethod.POST)
    public String approve(String estimateNo) {
        estimateService.approve(estimateNo);
        return "estimate/form";
    }
}
