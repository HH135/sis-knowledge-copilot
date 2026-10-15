package com.acme.svc.inventory;

import com.acme.svc.common.InterfaceLogger;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    private final InventoryDao inventoryDao;
    private final InterfaceLogger interfaceLogger;

    public InventoryService(InventoryDao inventoryDao, InterfaceLogger interfaceLogger) {
        this.inventoryDao = inventoryDao;
        this.interfaceLogger = interfaceLogger;
    }

    public void issue(String transactionNo, String partNo, String branchNo, String orderNo, String quantity) {
        inventoryDao.issue(transactionNo, partNo, branchNo, orderNo, quantity);
        interfaceLogger.write("PART_ISSUED", orderNo);
    }
}
