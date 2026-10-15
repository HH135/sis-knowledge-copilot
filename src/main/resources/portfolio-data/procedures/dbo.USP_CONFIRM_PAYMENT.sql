-- Procedure: dbo.USP_CONFIRM_PAYMENT
-- Description: 결제를 확정하고 쿠폰 사용 처리를 요청한다
-- Tables: TB_PAYMENT
CREATE PROCEDURE dbo.USP_CONFIRM_PAYMENT
    @PAYMENT_NO varchar(24),
    @COUPON_NO varchar(20),
    @CUST_NO varchar(20),
    @REDEMPTION_NO varchar(24),
    @DISCOUNT_AMOUNT decimal(12,2)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @ORDER_NO varchar(20);
    SELECT @ORDER_NO = ORDER_NO
    FROM dbo.TB_PAYMENT
    WHERE PAYMENT_NO = @PAYMENT_NO;

    IF @ORDER_NO IS NULL
        THROW 50024, 'Payment was not found.', 1;

    UPDATE dbo.TB_PAYMENT
    SET PAYMENT_STATUS = 'PAID',
        PAID_AT = SYSUTCDATETIME()
    WHERE PAYMENT_NO = @PAYMENT_NO
      AND PAYMENT_STATUS = 'PENDING';

    IF @@ROWCOUNT = 0
        THROW 50012, 'Pending payment was not found.', 1;

    EXECUTE dbo.USP_REDEEM_COUPON
        @REDEMPTION_NO = @REDEMPTION_NO,
        @COUPON_NO = @COUPON_NO,
        @ORDER_NO = @ORDER_NO,
        @CUST_NO = @CUST_NO,
        @DISCOUNT_AMOUNT = @DISCOUNT_AMOUNT;
END;
