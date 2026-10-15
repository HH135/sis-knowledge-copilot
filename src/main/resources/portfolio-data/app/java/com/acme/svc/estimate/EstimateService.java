package com.acme.svc.estimate;

import org.springframework.stereotype.Service;

@Service
public class EstimateService {
    private final EstimateDao estimateDao;

    public EstimateService(EstimateDao estimateDao) {
        this.estimateDao = estimateDao;
    }

    public void create(String estimateNo, String orderNo, String laborAmount, String partAmount) {
        estimateDao.create(estimateNo, orderNo, laborAmount, partAmount);
    }

    public void approve(String estimateNo) {
        estimateDao.approve(estimateNo);
    }
}
