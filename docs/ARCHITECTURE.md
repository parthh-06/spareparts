# Architecture — SparePartsApp

> Source of truth: `src/main/kotlin/Main.kt` (758 LOC). This document traces the actual code structure, data flow, and constraints before proposing improvements.

---

## 1. High-Level View

```
┌─────────────────────────────────────────────────────────────┐
│  Compose Desktop Window (Main.kt:34)                        │
│  App() — string router  screen ∈ {home,supplier,customer,   │
│          product,insurance}  (Main.kt:38-47)                 │
│                                                             │
│  ┌──────────┐  ┌───────────┐  ┌──────────┐  ┌───────────┐  │
│  │ Customer │  │ Supplier  │  │ Product  │  │ Insurance │  │
│  │ Screen   │  │ Screen    │  │ Screen   │  │ Screen    │  │
│  │  :64     │  │  :294     │  │  :542    │  │  :653     │  │
│  └────┬─────┘  └────┬──────┘  └────┬─────┘  └────┬──────┘  │
│       │             │              │              │          │
│       └──────┬──────┴──────┬───────┴──────┬───────┘          │
│              │             │              │                  │
│   DriverManager.getConnection("jdbc:mysql://localhost:3306/*")│
│   DB_USER=root DB_PASS=root (Main.kt:24)                    │
└──────────────┼─────────────┼──────────────┼─────────────────┘
               │             │              │
        ┌──────┴──────┐ ┌───┴─────┐  ┌─────┴─────┐ ┌──────────┐
        │ customers   │ │suppliers│  │  stock    │ │insurance │
        │ customer    │ │supplier │  │ products  │ │insurance │
        │ bills       │ │supplier_│  └───────────┘ └──────────┘
        │ bill_items  │ │ receipts│
        └─────────────┘ │ receipt_│
                        │ items   │
                        └─────────┘
```

* No layered architecture. Every screen owns its queries, transactions, and UI directly.
* No shared data layer, no repository, no ViewModel.

---

## 2. Module Breakdown

### 2.1 Entry & Navigation

- `fun main() = application { Window { App() } }` at `Main.kt:33` — single Compose Desktop window.
- `App:38` is a manual router via `var screen by remember { mutableStateOf("home") }` + `when(screen)`. No navigation library; back is just `screen = "home"` callback.

### 2.2 HomeScreen (`Main.kt:50`)

- Static branding `Text("WELCOME", girassol, 50.sp)` + 4 `HomeBtn:61` (250×132 dp, gold border `0xFFD5C875` on `0xFF353535`).
- Loads custom font `Font("fonts/girassol.ttf")` at `Main.kt:23`.

### 2.3 CustomerScreen (`Main.kt:64-174`) + AddCustomerBillDialog (`Main.kt:177-291`)

**State:**
`refresh: Int` (bump to re-query), `search: String`, `selectedCustomer`, `editingCustomer`, `deletingCustomer`, `showAdd`, `errorMsg`.

**Data fetch:**
`val allRows = remember(refresh) { DriverManager.getConnection("jdbc:mysql://localhost:3306/customers") ... JOIN customer/bills/bill_items ORDER BY bill_date DESC }` at `Main.kt:72-85`. Runs synchronously during composition — blocks UI thread.

**Derived state:**
`filtered = allRows.filter name/phone` at `Main.kt:86`, `grouped = filtered.groupBy name` at `Main.kt:87`, rendered inside `verticalScroll` (`Main.kt:97`).

**Actions per grouped customer:**
- `Bills` → `AlertDialog:118` grouping bills by `billId`, showing line items, total, `Paid/Unpaid` toggle that executes `UPDATE bills SET paid = NOT paid WHERE bill_id=?` then `refresh++` at `Main.kt:128-131`.
- `Edit` → `AlertDialog:148` updating `customer` row (`Main.kt:157`).
- `Del` → `DELETE FROM customer WHERE id=?` cascade to bills/items (`Main.kt:169`).

**AddCustomerBillDialog** — 3-step wizard (`step` int):
- `0` Check: `SELECT id FROM customer WHERE name=?` — if found `step=2`, else `step=1` (`Main.kt:252-255`).
- `1` Create: `INSERT INTO customer (name, phone)` (`Main.kt:263`).
- `2` Cart: autocomplete from `stock.products` loaded via `LaunchedEffect(Unit)` (`Main.kt:183-191`), suggestions clickable at `Main.kt:225`. `+ Add Item` validates `qty>0 && price!=null` then appends to `mutableStateListOf<LineItem>` (`Main.kt:233`). `Save Bill` runs single DB transaction `Main.kt:269-285`:
  ```
  BEGIN
    INSERT bills (customer_id, NOW(), FALSE) → billId
    FOR each LineItem:
      INSERT bill_items (bill_id, item_name, qty, price)
      UPDATE stock.products SET quantity = quantity - qty WHERE id=? AND quantity >= qty
        -- if affectedRows==0 → throw Insufficient stock
  COMMIT else ROLLBACK
  ```

**Cross-DB note:** transaction spans `customers` (bills) but stock update targets `stock.products` via `DriverManager.getConnection("jdbc:mysql://localhost:3306/stock")` separated — actually inside same connection to `customers` DB it prefixes `stock.products` (`Main.kt:277`), which works only if MySQL user has both DB grants and uses qualified table name `stock.products`.

### 2.4 SupplierScreen (`Main.kt:294-404`) + AddSupplierReceiptDialog (`Main.kt:407-539`)

Mirrors CustomerScreen but for inbound inventory.

- Query at `Main.kt:302-315`: `supplier JOIN supplier_receipts JOIN supplier_receipt_items JOIN stock.products` — note the last JOIN is cross-DB (`stock.products`) so connection is to `suppliers` DB but references `stock.products`.
- `View` dialog `Main.kt:348` groups by `receiptId`, shows receipt totals, paid toggle `UPDATE supplier_receipts SET paid = NOT paid`.
- **New product path** `Main.kt:468-485`: if `prodName` not in `stock`, UI shows `New product details` fields (type, rack) and `Create Product & Add Item` inserts into `stock.products` immediately (`quantity=0` seed) before adding to `lineItems`.
- Save transaction `Main.kt:518-531`:
  ```
  INSERT supplier_receipts (supplier_id, NOW(), FALSE) → receiptId
  FOR each LineItem:
    INSERT supplier_receipt_items (receipt_id, stock_id, qty, cost_price)
    UPDATE stock.products SET quantity = quantity + qty WHERE id=?
  COMMIT
  ```

### 2.5 productScreen (`Main.kt:542-650`)

- Loads `SELECT * FROM products ORDER BY product_name` at `Main.kt:548-555`.
- Grouped by `type` (`Main.kt:558`), card row shows `name/qty/cost/sell/rack` (`Main.kt:573-577`).
- Add/Edit dialog `Main.kt:592-638` handles 8 fields. **Side effect on INSERT** `Main.kt:618-632`: after inserting product, it auto-creates/locates a supplier by `suppliedBy` name and inserts `supplier_receipts (paid=TRUE) + supplier_receipt_items` to reconcile ledgers — silent cross-DB write.

### 2.6 insuranceScreen (`Main.kt:653-738`)

- Simplest CRUD: `SELECT * FROM insurance ORDER BY expiry_date` (`Main.kt:658-666`), filter by `customerName/vehicleNumber/policyName` (`Main.kt:668`), cards show coverage/expiry, add/edit validates 9 fields, delete by `id`.

### 2.7 Shared UI Primitives (`Main.kt:740-758`)

```
ScreenHeader(title, onBack, onAdd)  — back arrow + gold title + green +Add
SearchBar(q, onChange, hint)         — OutlinedTextField with gold focus border
ErrorCard(msg)                       — red 0xFF5C2E2E background
DataCard(content)                    — 0xFF3A3A3A rounded 8dp, elevation 3dp
Btn(text, color, action)             — 30dp height, 11sp bold white text
```

All screens compose them inside `Box(Modifier.background(0xFF2E2E2E))`.

---

## 3. Data Flow & State Management

- **State holder:** Compose `remember { mutableStateOf }` + `mutableStateListOf` per screen. No hoisting beyond screen.
- **Refresh pattern:** `var refresh by remember { mutableStateOf(0) }` passed as `remember(refresh) { query }` key — incrementing `refresh` re-runs the SQL query and recomposes the list. This is the entire cache invalidation strategy.
- **Search:** pure in-memory filter on the fetched list — no SQL `WHERE`, no debounce.
- **Dialogs:** nullable `selected*` / `editing*` vars drive `AlertDialog` visibility via `?.let {}` (`Main.kt:118`, `Main.kt:148`).

Sequence: `User types search → filtered recomputes → verticalScroll Column recomposes filtered DataCards`.

---

## 4. Persistence Details

### 4.1 Connection management

Every `remember` block, button click, and dialog does:
```kotlin
val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/<db>", DB_USER, DB_PASS)
... execute ...
c.close()
```
No try-with-resources, no finally in most paths (leak if exception before `close()`). No connection pool.

### 4.2 Transaction boundaries

Only two places use `autoCommit=false` + `commit/rollback`:
- Customer bill save `Main.kt:271`
- Supplier receipt save `Main.kt:520`

Others are single-statement auto-committed.

### 4.3 Cross-database writes

- Customer bill → updates `stock.products` via qualified name `stock.products` from a `customers` connection.
- Supplier receipt → updates `stock.products` from `suppliers` connection.
- Product insert → opens a *second* connection to `suppliers` to insert receipt side-effect.
  These require the MySQL user to have privileges on multiple DBs and rely on string-qualified tables rather than FK constraints.

---

## 5. Build & Packaging

- `build.gradle.kts:1-5` — `kotlin("jvm")` + `org.jetbrains.compose`.
- `compose.desktop.application { mainClass = "MainKt" }` (`build.gradle.kts:21-25`).
- `tasks.jar:26` builds a fat JAR merging `runtimeClasspath` via `zipTree`, manifest `Main-Class=MainKt`.
- `gradle.properties:1` sets `kotlin.code.style=official`.
- Gradle wrapper present (`gradlew`, `gradlew.bat`).

Compose Desktop distribution (`SparePartsApp/`) is the deliverable: native launcher + bundled `runtime/` (auto-provisioned JRE). Rebuild with `./gradlew createDistributable` or `packageExe` (Compose plugin tasks).

---

## 6. Constraints & Risks

| Risk | Location | Impact |
|------|----------|--------|
| JDBC on composition thread | `Main.kt:72`, `Main.kt:302`, `Main.kt:548`, `Main.kt:658` | UI freeze on slow DB |
| Unclosed connections on exception | throughout | Connection exhaustion |
| No prepared-statement reuse | everywhere | Extra parse overhead |
| `quantity >= qty` check only in `UPDATE WHERE` | `Main.kt:277` | Race if concurrent bills |
| `rack` UI allows any string, DB CHECK 1-30 | `Main.kt:604` vs `stock_db.sql:11` | SQL error surfaces as raw `errorMsg` |
| `price` stored as `item_name` string in `bill_items` | `Main.kt:279` | Loses FK to `products` |
| Supplier new product `selling_price=0` | `Main.kt:478` | Product unsellable until manual edit |

---

## 7. Recommended Refactor Roadmap

**Phase 1 — Safety (no API change):**
- Centralize `Database.getConnection()` / `use {}` pattern; add `HikariCP` or at least `try { } finally { close() }`.
- Extract `DB_USER/PASS` to `local.properties` or env var, not hardcoded.
- Add `customers_db.sql` to repo; commit inferred schema.
- Move blocking queries off main thread: `LaunchedEffect` + `withContext(Dispatchers.IO)` + `produceState`.

**Phase 2 — Structure:**
- Split `Main.kt` into `ui/customer/`, `ui/supplier/`, `ui/product/`, `ui/insurance/`, `data/db/`, `data/model/`.
- Introduce `Repository` per DB + `ViewModel` (e.g., `CustomerViewModel` holding `StateFlow<List<CustomerRow>>`).
- Replace string router with `enum Screen` or Compose Navigation.

**Phase 3 — Quality:**
- Add input validation (phone regex, rack 1-30 slider, non-empty name).
- Pagination / `LazyColumn` instead of `Column+verticalScroll`.
- Unit tests for transaction logic (use Testcontainers MySQL).
- KDoc on each data class and screen function.

---

## 8. Diagram: Bill Creation Sequence

```
User                      AddCustomerBillDialog                DB (customers+stock)
 |  Enter "Amit Verma"            |                                 |
 |  Click Check  ───────────────> | SELECT id FROM customer WHERE name="Amit Verma"
 |                               |────────────────────────────────> |
 |                               | <── found (id=7) ─────────────── |
 |  Add items: "Oil Filter x2"    |  (suggestions from stock)       |
 |  Click + Add Item             |  lineItems += LineItem(2,…)      |
 |  Click Save Bill ───────────> | BEGIN                             |
 |                               | INSERT bills (customer_id=7) ──> |
 |                               | <── billId=42 ─────────────────── |
 |                               | INSERT bill_items (42, Oil…) ──> |
 |                               | UPDATE stock.products qty-2 ───> |
 |                               | COMMIT ───────────────────────> |
 |  Dialog closes, refresh++      |                                 |
 | <── CustomerScreen recomposes VIEW ── SELECT JOIN ─────────────> |
```
