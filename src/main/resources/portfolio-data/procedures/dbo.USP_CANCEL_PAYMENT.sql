-- Procedure: dbo.USP_CANCEL_PAYMENT
-- Description: 미완료 결제를 취소한다
-- Tables: TB_PAYMENT
CREATE PROCEDURE dbo.USP_CANCEL_PAYMENT
    @PAYMENT_NO varchar(24)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.TB_PAYMENT
    SET PAYMENT_STATUS = 'CANCELLED'
    WHERE PAYMENT_NO = @PAYMENT_NO
      AND PAYMENT_STATUS = 'PENDING';

    IF @@ROWCOUNT = 0
        THROW 50013, 'Pending payment cannot be cancelled.', 1;

    DECLARE @RESULT int;
    EXEC @RESULT = dbo.USP_WRITE_INTERFACE_LOG
        @LOG_NO = 1002,
        @MESSAGE_TYPE = 'PAYMENT_CANCELLED',
        @CORRELATION_ID = @PAYMENT_NO,
        @PROCESS_STATUS = 'DONE',
        @PAYLOAD_SUMMARY = N'Payment cancelled';
END;
