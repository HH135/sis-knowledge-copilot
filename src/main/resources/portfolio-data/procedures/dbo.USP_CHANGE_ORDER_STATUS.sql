-- Procedure: dbo.USP_CHANGE_ORDER_STATUS
-- Description: 정비 주문의 진행 상태를 변경한다
-- Tables: TB_REPAIR_ORDER
CREATE PROCEDURE dbo.USP_CHANGE_ORDER_STATUS
    @ORDER_NO varchar(20),
    @ORDER_STATUS varchar(16)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.TB_REPAIR_ORDER
    SET ORDER_STATUS = @ORDER_STATUS
    WHERE ORDER_NO = @ORDER_NO
      AND CLOSED_AT IS NULL;

    IF @@ROWCOUNT = 0
        THROW 50006, 'Open repair order was not found.', 1;
END;
