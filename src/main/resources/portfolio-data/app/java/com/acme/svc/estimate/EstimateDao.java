package com.acme.svc.estimate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EstimateDao {
    int create(@Param("estimateNo") String estimateNo, @Param("orderNo") String orderNo,
               @Param("laborAmount") String laborAmount, @Param("partAmount") String partAmount);

    int approve(@Param("estimateNo") String estimateNo);
}
