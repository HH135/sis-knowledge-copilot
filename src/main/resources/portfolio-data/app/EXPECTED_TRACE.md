# Expected legacy source trace

All paths and code below are fictional Acme Motors Service portfolio fixtures.

| Screen (JSP) | URL | Controller.method | Service.method | DAO.method | Mapper id | Procedure |
|---|---|---|---|---|---|---|
| `webapp/WEB-INF/views/appointment/list.jsp` | `/appointment/register` | `AppointmentController.register` | `AppointmentService.register` | `AppointmentDao.register` | `AppointmentMapper.register` | `dbo.USP_REGISTER_APPOINTMENT` |
| `webapp/WEB-INF/views/appointment/list.jsp` | `/appointment/confirm` | `AppointmentController.confirm` | `AppointmentService.confirm` | `AppointmentDao.confirm` | `AppointmentMapper.confirm` | `dbo.USP_CONFIRM_APPOINTMENT` |
| `webapp/WEB-INF/views/appointment/register.jsp` | `/appointment/register` | `AppointmentController.register` | `AppointmentService.register` | `AppointmentDao.register` | `AppointmentMapper.register` | `dbo.USP_REGISTER_APPOINTMENT` |
| `webapp/WEB-INF/views/repairorder/list.jsp` | `/repair-order/create` | `RepairOrderController.create` | `RepairOrderService.create` | `RepairOrderDao.create` | `RepairOrderMapper.create` | `dbo.USP_CREATE_REPAIR_ORDER` |
| `webapp/WEB-INF/views/repairorder/detail.jsp` | `/repair-order/complete` | `RepairOrderController.complete` | `RepairOrderService.complete` | `RepairOrderDao.changeStatus` → `RepairOrderDao.complete` | `RepairOrderMapper.changeStatus` → `RepairOrderMapper.complete` | `dbo.USP_CHANGE_ORDER_STATUS` → `dbo.USP_COMPLETE_REPAIR_ORDER` |
| `webapp/WEB-INF/views/estimate/form.jsp` | `/estimate/create` | `EstimateController.create` | `EstimateService.create` | `EstimateDao.create` | `EstimateMapper.create` | `dbo.USP_CREATE_ESTIMATE` |
| `webapp/WEB-INF/views/estimate/form.jsp` | `/estimate/approve` | `EstimateController.approve` | `EstimateService.approve` | `EstimateDao.approve` | `EstimateMapper.approve` | `dbo.USP_APPROVE_ESTIMATE` |
| `webapp/WEB-INF/views/payment/form.jsp` | `/payment/register-and-confirm` | `PaymentController.registerAndConfirm` | `PaymentService.registerAndConfirm` | `PaymentDao.register` → `PaymentDao.confirm` | `PaymentMapper.register` → `PaymentMapper.confirm` | `dbo.USP_REGISTER_PAYMENT` → `dbo.USP_CONFIRM_PAYMENT` |
| `webapp/WEB-INF/views/payment/coupon.jsp` | `/payment/coupon/cancel` | `PaymentController.cancelCoupon` | `CouponService.cancel` | `PaymentDao.cancelCouponRedemption` | `PaymentMapper.cancelCouponRedemption` | `dbo.USP_CANCEL_COUPON_REDEMPTION` |
| `webapp/WEB-INF/views/inventory/issue.jsp` | `/inventory/issue` | `InventoryController.issue` | `InventoryService.issue` | `InventoryDao.issue` | `InventoryMapper.issue` | `dbo.USP_ISSUE_PART` |

## Shared interface logging

Appointment registration/confirmation, repair-order creation, payment confirmation, and part issue call `InterfaceLogger.write`. The shared path is `InterfaceLogger.write` → `CommonDao.writeLog` → `CommonMapper.writeLog` → `dbo.USP_WRITE_INTERFACE_LOG`.

## Unused mapper statement

`CommonMapper.retryInterfaceLog` is defined in `resources/mapper/CommonMapper.xml`, delegates to `CommonDao.retryInterfaceLog`, and calls `dbo.USP_RETRY_INTERFACE_SYNC`. No service or controller invokes this mapper statement; it is intentionally retained as an unused-code detection fixture.
