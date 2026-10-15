-- Procedure: dbo.USP_REGISTER_APPOINTMENT
-- Description: 고객 차량의 정비 예약을 등록한다
-- Tables: TB_CUSTOMER, TB_VEHICLE, TB_APPOINTMENT
CREATE PROCEDURE dbo.USP_REGISTER_APPOINTMENT
    @APPT_NO varchar(20),
    @CUST_NO varchar(20),
    @VIN varchar(32),
    @BAY_NO varchar(12) = NULL,
    @APPT_AT datetime2,
    @REQUEST_NOTE nvarchar(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (SELECT 1 FROM dbo.TB_CUSTOMER WHERE CUST_NO = @CUST_NO)
        THROW 50001, 'Customer was not found.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.TB_VEHICLE WHERE VIN = @VIN AND CUST_NO = @CUST_NO)
        THROW 50002, 'Vehicle does not belong to the customer.', 1;

    INSERT INTO dbo.TB_APPOINTMENT
        (APPT_NO, CUST_NO, VIN, BAY_NO, APPT_AT, APPT_STATUS, REQUEST_NOTE)
    VALUES
        (@APPT_NO, @CUST_NO, @VIN, @BAY_NO, @APPT_AT, 'REQUESTED', @REQUEST_NOTE);
END;
