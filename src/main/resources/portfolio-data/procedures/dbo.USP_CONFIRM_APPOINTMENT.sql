-- Procedure: dbo.USP_CONFIRM_APPOINTMENT
-- Description: 요청된 정비 예약을 확정한다
-- Tables: TB_APPOINTMENT, TB_SERVICE_BAY
CREATE PROCEDURE dbo.USP_CONFIRM_APPOINTMENT
    @APPT_NO varchar(20),
    @BAY_NO varchar(12)
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.TB_SERVICE_BAY
        WHERE BAY_NO = @BAY_NO AND ACTIVE_YN = 'Y'
    )
        THROW 50003, 'Service bay is unavailable.', 1;

    UPDATE dbo.TB_APPOINTMENT
    SET BAY_NO = @BAY_NO,
        APPT_STATUS = 'CONFIRMED'
    WHERE APPT_NO = @APPT_NO
      AND APPT_STATUS = 'REQUESTED';

    IF @@ROWCOUNT = 0
        THROW 50004, 'Appointment cannot be confirmed.', 1;
END;
