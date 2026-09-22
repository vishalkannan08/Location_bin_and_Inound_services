# location-bin-service

WMS Increment 1 — DEV-03 and DEV-04 from the requirement doc. Owns **locations** and **bins** only.

No Kafka. PostgreSQL + Spring Boot + Postman, as requested.

---

## Stack

| | |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Persistence | Spring Data JPA + Hibernate, Flyway migrations |
| Database | PostgreSQL |
| Docs | springdoc-openapi → `/swagger-ui.html` |
| Port | 8082 |

No Lombok. Explicit getters so there's no annotation-processor setup to get wrong in STS/IntelliJ.

---

## Run it

```bash
# 1. Postgres
docker compose up -d

# 2. Service (Flyway creates the schema on first boot)
mvn spring-boot:run
```

Or point it at an existing database:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/wms_location_bin
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

Swagger UI: http://localhost:8082/swagger-ui.html
Health: http://localhost:8082/actuator/health

### In STS / Spring Tool Suite

`File → Import → Maven → Existing Maven Projects` → pick this folder. Then run
`LocationBinServiceApplication` as a Java Application.

---

## Postman

Import `postman/location-bin-service.postman_collection.json`. Run the folder top
to bottom — request 01 writes `locationId` into a collection variable and 06 writes
`binId`, so the later requests need no manual copy-pasting. Requests 02, 03, 07, 12
and 13 assert the failure paths (409 / 400 / 422 / 404).

---

## API

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/locations` | 201 + `Location` header |
| GET | `/api/v1/locations/{id}` | |
| GET | `/api/v1/warehouses/{warehouseId}/locations` | paged, `?status=ACTIVE` |
| PUT | `/api/v1/locations/{id}` | name + type only |
| PATCH | `/api/v1/locations/{id}/status` | |
| POST | `/api/v1/bins` | 201 + `Location` header |
| GET | `/api/v1/bins/{id}` | returns `acceptsPutAway` |
| GET | `/api/v1/locations/{locationId}/bins` | paged, `?status=AVAILABLE` |
| PUT | `/api/v1/bins/{id}` | capacity only |
| PATCH | `/api/v1/bins/{id}/status` | |

The doc's section 7 only lists create + list. Get-by-id, update and status change
were added because put-away cannot validate a target bin without them.

### Quick curl

```bash
BASE=http://localhost:8082
WH=11111111-1111-1111-1111-111111111111

LOC=$(curl -s -X POST $BASE/api/v1/locations -H 'Content-Type: application/json' \
  -d "{\"warehouseId\":\"$WH\",\"locationCode\":\"A1-RACK-01\",\"name\":\"Aisle 1 Rack 1\",\"type\":\"RACK\"}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')

curl -s -X POST $BASE/api/v1/bins -H 'Content-Type: application/json' \
  -d "{\"locationId\":\"$LOC\",\"binCode\":\"BIN-001\",\"capacity\":120}"
```

---

## Design decisions worth reviewing

**`warehouse_id` has no foreign key.** This service does not own the `warehouse`
table, and section 9 of the requirement forbids cross-service DB access. The column
is an unvalidated logical reference. When `warehouse-service` exists, add a
`WarehouseClient` HTTP call in `LocationService.create` — there's a marked comment
at the spot. Until then, **a location can be created against a warehouse id that
does not exist.** Raise this at the architecture review; it's a deliberate gap,
not an oversight.

**Codes are normalised to uppercase and are immutable.** `a1-rack-01` and
`A1-RACK-01` are the same location. Changing a code after bins reference it would
break every downstream printed label, so `PUT` does not accept it.

**Uniqueness is scoped, not global.** `location_code` is unique per warehouse;
`bin_code` is unique per location. Enforced by DB constraints *and* pre-checked in
the service so the client gets a readable 409 instead of a constraint error. The DB
constraint is the real guard — the pre-check loses a concurrent-insert race, which
`GlobalExceptionHandler` catches and turns into a 409.

**`acceptsPutAway` is computed here, not by the caller.** A bin is a valid put-away
target only if the bin *and* its parent location are usable. Putting that rule in
`putaway-service` means two services can disagree about it.

**Bins have an `@Version` column.** Put-away will contend on the same bin row from
concurrent workers; without optimistic locking you get lost updates on status.
Returns 409 `CONCURRENT_MODIFICATION`.

**`open-in-view: false`.** Lazy loads outside a transaction fail loudly instead of
silently issuing queries from the view layer. This is why `BinRepository` has
`findByIdWithLocation` with a `join fetch`.

---

## Error shape

Every non-2xx returns the same body:

```json
{
  "timestamp": "2026-09-22T12:00:00.000+05:30",
  "status": 409,
  "error": "Conflict",
  "code": "LOCATION_CODE_ALREADY_EXISTS",
  "message": "Location code 'A1-RACK-01' already exists in warehouse 1111...",
  "path": "/api/v1/locations",
  "correlationId": "0b0c...",
  "fieldErrors": null
}
```

| Status | When |
|---|---|
| 400 | bean validation failure, bad enum, malformed UUID, unparseable JSON |
| 404 | location / bin not found |
| 409 | duplicate code, optimistic lock conflict |
| 422 | business rule violation (bin under non-ACTIVE location, deactivating a location that still has live bins) |
| 500 | anything unhandled — logged with the correlation id |

`X-Correlation-Id` is read from the request or generated, echoed on the response,
and put in the MDC so it appears in every log line (DEV-09).

---

## Tests

```bash
mvn test
```

- `LocationServiceTest`, `BinServiceTest` — business rules, Mockito, no DB.
- `LocationControllerTest` — `@WebMvcTest`, error contract and status codes.

**Not covered yet:** repository and end-to-end tests against a real PostgreSQL.
Add Testcontainers (`spring-boot-testcontainers` + `org.testcontainers:postgresql`)
for those — an H2 fallback would not exercise the Flyway migration or the DB
constraints, which is most of what's worth testing here.

---

## Not in this increment

Security (DEV-09), Kafka events (DEV-08), and the warehouse-existence check.
All three are deliberate. `SecurityFilterChain` would currently permit everything,
so there is no security config at all rather than a misleading one.

---

## Known risk

`springdoc-openapi` is pinned to `2.8.0`. If dependency resolution fails or Swagger
UI misbehaves against Boot 3.5.16, bump `<springdoc.version>` to the current
release — check https://springdoc.org. The dependency is optional; deleting it and
`OpenApiConfig.java` removes only Swagger UI and changes nothing else.
