package com.acme.svc.inventory;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryDao {
    int issue(@Param("transactionNo") String transactionNo, @Param("partNo") String partNo,
              @Param("branchNo") String branchNo, @Param("orderNo") String orderNo,
              @Param("quantity") String quantity);
}
