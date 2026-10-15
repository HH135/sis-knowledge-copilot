-- Procedure: dbo.USP_APPROVE_ESTIMATE
-- Description: 작성된 정비 견적을 승인한다
-- Tables: TB_ESTIMATE
CREATE PROCEDURE dbo.USP_APPROVE_ESTIMATE
    @ESTIMATE_NO varchar(20)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.TB_ESTIMATE
    SET ESTIMATE_STATUS = 'APPROVED'
    WHERE ESTIMATE_NO = @ESTIMATE_NO
      AND ESTIMATE_STATUS = 'DRAFT';

    IF @@ROWCOUNT = 0
        THROW 50009, 'Draft estimate was not found.', 1;
END;
