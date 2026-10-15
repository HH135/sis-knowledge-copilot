# Expected specification differences

`metadata/tables.json` is the fictional operational metadata. `spec/table-spec.json` is the fictional table specification. Exactly four intentional differences are present:

| # | Difference | Location | Operational metadata (`tables.json`) | Specification (`table-spec.json`) |
|---|---|---|---|---|
| 1 | Specification-only column | `dbo.TB_CUSTOMER.EMAIL_OPT_IN` | Column is absent | `char(1)`, required, “이메일 수신 동의” |
| 2 | Metadata-only column | `dbo.TB_VEHICLE.COLOR_CODE` | `varchar(12)`, nullable, “차량 색상 코드” | Column is absent |
| 3 | Data type mismatch | `dbo.TB_CUSTOMER.PHONE_NO` | `varchar(20)` | `varchar(30)` |
| 4 | Description mismatch | `dbo.TB_REPAIR_ORDER.ORDER_STATUS` | “정비 주문 상태” | “정비 진행 상태” |

No other table or column differences are intentional.
