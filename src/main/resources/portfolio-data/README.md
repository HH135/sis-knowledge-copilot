# Fictional portfolio data

All data in this directory is fictional and was created solely for the Acme Motors Service portfolio demo. It does not represent a real company, production system, database, or customer.

## Domain

Acme Motors Service manages vehicle maintenance from appointment booking through repair completion and payment. The metadata fixture describes customers and their vehicles, repair orders and labor tasks, estimates, parts and inventory, purchasing, coupons, technicians, service bays, service history, and outbound/inbound interface events.

## Entity relationships

- A customer (`CUST_NO`) can own multiple vehicles (`VIN`) and book appointments.
- A vehicle can have multiple repair orders (`ORDER_NO`) and service-history entries.
- A repair order can contain repair tasks, estimates, payments, and coupon redemptions.
- Repair tasks can be assigned to technicians and reference parts.
- Parts are tracked in inventory, inventory transactions, and supplier purchase orders/lines.
- Appointments reserve a service bay and may later be associated with a repair order.
- Interface logs record fictional integration events and are not connected to any external system.

The operational metadata is in `metadata/tables.json`. `spec/table-spec.json` uses the same JSON format as a fictional specification document. Four deliberate differences are documented in `spec/EXPECTED_DIFF.md` for later comparison exercises.
