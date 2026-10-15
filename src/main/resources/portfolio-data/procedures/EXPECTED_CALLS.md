# Expected procedure call relationships

All procedure names below refer to the fictional Acme Motors Service portfolio fixture. Only statically resolvable procedure-to-procedure calls are listed as resolved edges.

## Resolved caller-to-callee edges

| Caller | Callee | Resolution |
|---|---|---|
| `dbo.USP_CREATE_REPAIR_ORDER` | `dbo.USP_WRITE_INTERFACE_LOG` | Resolved |
| `dbo.USP_COMPLETE_REPAIR_ORDER` | `dbo.USP_LOAD_SERVICE_HISTORY` | Resolved |
| `dbo.USP_COMPLETE_REPAIR_ORDER` | `dbo.USP_CONFIRM_PAYMENT` | Resolved |
| `dbo.USP_CONFIRM_PAYMENT` | `dbo.USP_REDEEM_COUPON` | Resolved |
| `dbo.USP_CANCEL_PAYMENT` | `dbo.USP_WRITE_INTERFACE_LOG` | Resolved |
| `dbo.USP_REDEEM_COUPON` | `dbo.USP_WRITE_INTERFACE_LOG` | Resolved |
| `dbo.USP_LOAD_SERVICE_HISTORY` | `dbo.USP_WRITE_INTERFACE_LOG` | Resolved |
| `dbo.USP_CREATE_PART_PURCHASE_ORDER` | `dbo.USP_WRITE_INTERFACE_LOG` | Resolved |

## Longest call chain

The longest designed chain contains four procedures:

`dbo.USP_COMPLETE_REPAIR_ORDER`
→ `dbo.USP_CONFIRM_PAYMENT`
→ `dbo.USP_REDEEM_COUPON`
→ `dbo.USP_WRITE_INTERFACE_LOG`

`dbo.USP_COMPLETE_REPAIR_ORDER` also calls `dbo.USP_LOAD_SERVICE_HISTORY` as a separate branch.

## Unresolved or ignored cases

- **Dynamic SQL:** `dbo.USP_RETRY_INTERFACE_SYNC` invokes `sys.sp_executesql` with an SQL string. The string's contents must not be treated as a statically resolved procedure call.
- **Missing procedure:** `dbo.USP_CREATE_REPAIR_ORDER` contains a direct call to `dbo.USP_LEGACY_SYNC`. There is intentionally no matching fixture file, so this edge is unresolved.
- **Comment only:** `dbo.USP_CREATE_REPAIR_ORDER` includes `-- EXEC dbo.USP_CONFIRM_APPOINTMENT ...` as a comment. A call graph parser must ignore it; it is not a resolved edge.

`EXEC`, `EXECUTE`, return-code assignment, and named parameter calls are represented in the fixture to exercise different call syntax forms.
