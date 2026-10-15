package com.acme.svc.inventory;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @RequestMapping(value = "/inventory/issue", method = RequestMethod.POST)
    public String issue(String transactionNo, String partNo, String branchNo, String orderNo, String quantity) {
        inventoryService.issue(transactionNo, partNo, branchNo, orderNo, quantity);
        return "inventory/issue";
    }
}
