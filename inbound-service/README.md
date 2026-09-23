# inbound-service

WMS Increment 1 — DEV-05. Owns **goods receipts** and **receipt lines** only.

No Kafka. Separate project, separate database, separate port from `location-bin-service`.

| | |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Database | PostgreSQL — `wms_inbound` |
| Port | **8083** |

---

## Run it

```sql
CREATE DATABASE wms_inbound;
```

Then run `InboundServiceApplication` in STS. Flyway creates the tables on first boot.

It runs alongside `location-bin-service` (8082) without conflict — different port, different database. The two services do **not** talk to each other; a goods receipt doesn't need bins.

Swagger UI: http://localhost:8083/swagger-ui.html

---

## The model

A **goods receipt** is the header: one delivery, against one purchase order, into one warehouse. A **receipt line** is one SKU on that delivery.

```
GR-000001  (PO-10001, warehouse 1111...)   status: RECEIVED
  ├─ line 1  SKU-1001  expected 100  received 100  put away 60   PARTIALLY_PUT_AWAY
  └─ line 2  SKU-1002  expected  50  received  48  put away  0   RECEIVED  ← discrepancy
```

Three quantities per line, and the difference between them is the whole point:

| Quantity | Meaning |
|---|---|
| `expectedQuantity` | What the supplier said they sent |
| `receivedQuantity` | What the worker actually counted |
| `putAwayQuantity` | How much has been moved into a bin |

`expected ≠ received` is a **discrepancy** — the delivery was short or over. Flagged, not rejected; the business decides what to do.

`received − putAway` is `remainingQuantity` — stock still sitting on the dock, waiting for put-away.

---

## Status is derived, never set

You cannot PATCH a status on this service. Both statuses are recalculated from the quantities every time anything changes:

**Line:** `PENDING` (nothing counted) → `RECEIVED` (counted, none put away) → `PARTIALLY_PUT_AWAY` → `PUT_AWAY`

**Receipt:** `DRAFT` (nothing counted on any line) → `RECEIVED` → `COMPLETED` (every live line fully put away)

This is deliberate. A status column that can be set independently of the numbers it describes will eventually disagree with them, and then nobody trusts either.

---

## Two ways to create a receipt

**One step** — the requirement doc's sample payload. Supply `receivedQuantity` at creation; the receipt lands as `RECEIVED` immediately.

**Two steps** — omit `receivedQuantity`. The receipt is `DRAFT` with `PENDING` lines, and you count stock in later with `POST /goods-receipts/{id}/lines/{lineId}/receive`.

Both work. The two-step flow is what a real dock uses — the PO is known before the truck arrives.

---

## API

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/goods-receipts` | 201, receipt number auto-generated |
| GET | `/api/v1/goods-receipts/{id}` | with lines |
| GET | `/api/v1/goods-receipts/by-number/{receiptNumber}` | e.g. `GR-000001` |
| GET | `/api/v1/warehouses/{warehouseId}/goods-receipts` | paged, `?status=RECEIVED` |
| POST | `/api/v1/goods-receipts/{id}/lines/{lineId}/receive` | record the physical count |
| POST | `/api/v1/goods-receipts/{id}/lines/{lineId}/cancel` | |
| POST | `/api/v1/goods-receipts/{id}/cancel` | |

### The putaway-service seam

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/goods-receipt-lines/{lineId}` | one line + `remainingQuantity` |
| GET | `/api/v1/warehouses/{warehouseId}/goods-receipt-lines/awaiting-put-away` | the work queue |
| POST | `/api/v1/goods-receipt-lines/{lineId}/put-away-allocation` | record a put-away |

Without these, section 9's rule — *"Put-Away quantity cannot exceed eligible received quantity"* — is unimplementable anywhere. `putaway-service` needs to ask this service how much is left and tell it when stock moves.

With Kafka in scope this would be a `PutAwayCompleted` event instead of a POST. You said no Kafka, so it's a synchronous call. Worth flagging at the review: this makes `putaway-service` depend on `inbound-service` being up.

---

## Receipt numbers

Generated from a PostgreSQL sequence: `GR-000001`, `GR-000002`, …

A sequence, not `COUNT(*) + 1`. Two receipts created at the same moment would otherwise both compute the same number and one insert would fail on the unique constraint.

---

## Decisions to review

**`skuId` is a String, not a UUID.** There is no product or SKU table anywhere in section 6 of the requirement doc, so there is nothing to hold a reference to. `"SKU-1001"` is stored as a code. This is inconsistent with `warehouseId` being a UUID — that inconsistency is in the doc, not resolvable here.

**Quantities are `BigDecimal(15,3)`, not `int`.** The doc has a `uom` column. `12.5 KG` and `3.75 L` are ordinary warehouse receipts. Integers would force a migration the first time someone receives by weight.

**No warehouse validation.** Same gap as `location-bin-service` — `warehouseId` is accepted unchecked because `warehouse-service` doesn't exist. Marked with a comment in `GoodsReceiptService.create`.

**`GoodsReceipt` is an aggregate root.** Lines are only ever changed through it, so the receipt status is recalculated in the same transaction. `allocateToPutAway` goes through the receipt rather than straight to the line for exactly this reason — otherwise a receipt stays `RECEIVED` forever after its last line is put away.

**`@Version` on both entities.** Concurrent put-away workers hitting the same line is the realistic failure. Without optimistic locking, two workers each allocating 60 against a 100-unit line both succeed and you have put away 120 units that do not exist. Returns 409.

**The DB enforces the quantity rules too.** `CHECK (put_away_quantity <= received_quantity)` and the non-negative checks are in the migration, not just in Java. Application code is not the last line of defence when other services will eventually write here.

---

## Duplication with location-bin-service

`ApiError`, `PageResponse`, `CorrelationIdFilter`, `GlobalExceptionHandler` and the three exception classes are **copied verbatim** between the two services.

That's fine at two services. At four it isn't. The usual fix is a small shared `wms-common` library — but it couples deployments, so raise it at the architecture review rather than deciding alone.

---

## Tests

```bash
mvn test
```

`GoodsReceiptTest` — pure domain, no Spring, no database. Covers partial put-away, over-allocation, discrepancies, cancellation rules and decimal quantities.

**Not covered:** integration tests against a real PostgreSQL. The CHECK constraints and the sequence are untested until those exist.

---

## Not in this increment

Security (DEV-09), Kafka `GoodsReceived` event (DEV-08), warehouse and SKU validation.
