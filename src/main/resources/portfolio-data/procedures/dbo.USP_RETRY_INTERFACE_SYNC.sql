-- Procedure: dbo.USP_RETRY_INTERFACE_SYNC
-- Description: 실패한 가상 연동 기록의 재처리를 표시한다
-- Tables: TB_INTERFACE_LOG
CREATE PROCEDURE dbo.USP_RETRY_INTERFACE_SYNC
    @LOG_NO bigint,
    @CORRELATION_ID varchar(40)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @SQL nvarchar(500) =
        N'UPDATE dbo.TB_INTERFACE_LOG
          SET PROCESS_STATUS = @STATUS
          WHERE LOG_NO = @LOG_NO AND CORRELATION_ID = @CORRELATION_ID';

    EXEC sys.sp_executesql
        @SQL,
        N'@STATUS varchar(16), @LOG_NO bigint, @CORRELATION_ID varchar(40)',
        @STATUS = 'RETRY',
        @LOG_NO = @LOG_NO,
        @CORRELATION_ID = @CORRELATION_ID;
END;
