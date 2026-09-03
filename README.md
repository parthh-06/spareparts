# SparePartsApp — Desktop Inventory & Billing System

> Kotlin + Compose for Desktop + MySQL. Single-window app for managing spare-parts stock, customers/bills, suppliers/receipts, and vehicle insurance policies.

**Entry point:** `src/main/kotlin/Main.kt:33` (`MainKt`) | **Build:** `build.gradle.kts:1` | **Package:** `compose.desktop` → `SparePartsApp/SparePartsApp.exe`

---

## 1. Overview

SparePartsApp is a CRUD + transactional desktop app for an auto-spare-parts shop. All business flows are in one file (`Main.kt`, ~758 LOC) using Jetpack Compose Desktop for UI and direct JDBC (`mysql:mysql-connector-java:8.0.33`) to four MySQL databases.

### Core modules (Home screen `Main.kt:50`)

| Tile | Screen fn | DB(s) | Purpose |
|------|-----------|-------|---------|
| **CUSTOMER** | `CustomerScreen:64` | `customers` + `stock` | Customers, bills, bill items, stock deduction |
| **SUPPLIER** | `SupplierScreen:294` | `suppliers` + `stock` | Suppliers, receipts, receipt items, stock increment |
| **PRODUCT** | `productScreen:542` | `stock` (+ `suppliers` for auto-receipt) | Product catalogue, rack/price/quantity |
| **INSURANCE** | `insuranceScreen:653` | `insurance` | Vehicle insurance policies by expiry |

Home navigation is state-driven: `App:38` holds `screen` string and switches composables.

---

## 2. Tech Stack

- **Language:** Kotlin 1.9.20 (`build.gradle.kts:2`)
- **UI:** `org.jetbrains.compose` 1.5.10 Desktop (`build.gradle.kts:3`), Material (Compose)
- **Font:** `Girassol` — `src/main/resources/fonts/girassol.ttf` loaded as `FontFamily` at `Main.kt:23`
- **DB Driver:** `mysql:mysql-connector-java:8.0.33` (`build.gradle.kts:14`)
- **Build:** Gradle (Foojay toolchains), fat JAR (`tasks.jar:26`) + Compose `runtime`/`app` distribution → `SparePartsApp/` (custom `compose.desktop.application.mainClass = MainKt`)
- **DB Engine:** MySQL, four logical databases (see `database/`), hardcoded creds `root/root` at `Main.kt:24-25`

---

## 3. Project Layout

```
spareparts/
├── build.gradle.kts          # plugins, dependencies, jar manifest
├── settings.gradle.kts       # rootProject.name = "spareparts"
├── src/main/kotlin/Main.kt   # ALL UI + DB logic (monolith)
├── src/main/resources/fonts/girassol.ttf
├── database/
│   ├── stock_db.sql          # products (15 seed rows)
│   ├── suppliers_db.sql      # supplier / supplier_receipts / supplier_receipt_items
│   ├── insurance_db.sql      # insurance (10 seed rows)
│   └── migration.sql         # ADD COLUMN paid
├── SparePartsApp/            # Compose distributable (exe + runtime + app)
│   ├── SparePartsApp.exe
│   ├── app/
│   └── runtime/              # bundled JRE
└── build/ libs/              # fat JAR output
```

No test sources (`src/test/` empty), no `customers_db.sql` checked in (inferred — see `docs/DATABASE.md`).

---

## 4. Quick Start

### Prerequisites
- JDK 17+ (Foojay resolver configured)
- MySQL 8.x running on `localhost:3306`, user `root` / `root`
- Gradle wrapper (`./gradlew`)

### 4.1 Restore databases

```powershell
mysql -u root -proot < database/stock_db.sql
mysql -u root -proot < database/suppliers_db.sql
mysql -u root -proot < database/insurance_db.sql

# Reconstruct missing customers DB (not in repo):
mysql -u root -proot -e "CREATE DATABASE customers;"
mysql -u root -proot customers -e "
CREATE TABLE customer (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, phone VARCHAR(15) NOT NULL);
CREATE TABLE bills (bill_id INT AUTO_INCREMENT PRIMARY KEY, customer_id INT NOT NULL, bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, paid BOOLEAN DEFAULT FALSE, FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE);
CREATE TABLE bill_items (id INT AUTO_INCREMENT PRIMARY KEY, bill_id INT NOT NULL, item_name VARCHAR(100) NOT NULL, quantity INT NOT NULL CHECK (quantity>0), price DECIMAL(10,2) NOT NULL, FOREIGN KEY (bill_id) REFERENCES bills(bill_id) ON DELETE CASCADE);
"

# If upgrading existing DBs:
mysql -u root -proot < database/migration.sql
```

### 4.2 Run from source

```powershell
.\gradlew run
# or
.\gradlew jar  # → build/libs/spareparts-*.jar (fat jar with runtime classpath)
java -jar build/libs/spareparts-*.jar
```

### 4.3 Run distributable

```powershell
.\SparePartsApp\SparePartsApp.exe
```

> The distributable bundles a JRE in `SparePartsApp/runtime/` — no separate Java install needed for end users.

---

## 5. Data Model (summary)

```kotlin
// Main.kt:27-31
data class CustomerRow(customerId, name, phone, billId, billDate, paid, itemName, quantity, price)
data class SupplierRow(supplierId, name, phone, receiptId, receiptDate, paid, productName, quantity, costPrice)
data class ProductRow(id, productName, type, quantity, suppliedBy, warranty, rack, costPrice, sellingPrice)
data class InsuranceRow(id, customerName, vehicleNumber, drivingLicense, panNumber, rcNumber, insuranceCompany, policyName, coverage, expiryDate)
data class LineItem(stockId, productName, qty, price) // transient cart line
```

Full DDL + ER diagram: `docs/DATABASE.md`.

---

## 6. Key Business Rules

- **Customer bill creation** (`AddCustomerBillDialog:177`): 3-step wizard (find/create customer → add line items → transactional `INSERT bills + bill_items + UPDATE stock.products quantity = quantity - ?` with rollback on `Insufficient stock` at `Main.kt:281`).
- **Supplier receipt creation** (`AddSupplierReceiptDialog:407`): similar wizard; can **auto-create product** if not in stock at `Main.kt:478`; transactional `stock quantity + ?` at `Main.kt:526`.
- **Direct product insert** (`productScreen:609-633`): inserting a product also auto-creates a `suppliers` receipt marked `paid=TRUE` to keep ledgers in sync.
- **Paid toggle:** bills (`Main.kt:128`) and receipts (`Main.kt:358`) flip `paid = NOT paid`.
- **Stock guard:** `products.rack BETWEEN 1 AND 30`, `quantity >= 0` (DB CHECK constraints).

---

## 7. UI Theme

- Background `0xFF2E2E2E`, cards `0xFF3A3A3A`, accent gold `0xFFD5C875`, buttons dark `0xFF353535` / green `0xFF1C4A1E` / red `0xFF760E03`.
- Reusable primitives: `ScreenHeader:740`, `SearchBar:747`, `DataCard:753`, `Btn:756`, `ErrorCard:750`, `HomeBtn:61`.

---

## 8. Known Limitations / Tech Debt

1. **Monolith** — All 4 domains in one file; no ViewModel/Repository layer, no DI, hard to test.
2. **Per-event JDBC** — New `DriverManager.getConnection` per query/remember block; no pool (`HikariCP`), no singleton. Connection leaks if exception before `close()` (only closed on happy path in many places).
3. **Hardcoded credentials** at `Main.kt:24-25`.
4. **Cross-DB FK impossible** — `supplier_receipt_items.stock_id` logically references `stock.products.id` (`suppliers_db.sql:22`) but MySQL FK can't span databases; referential integrity enforced only in app code (`Main.kt:526`, `Main.kt:279`).
5. **Missing `customers_db.sql`** — must reconstruct from `Main.kt:76-80` queries.
6. **`remember(refresh)` + `DriverManager` on main thread** — blocks composition; should use `LaunchedEffect`/coroutine + `withContext(Dispatchers.IO)`.
7. No input validation (phone format, rack range in UI, duplicate name handling).
8. No pagination — `verticalScroll` loads full table.

See `docs/ARCHITECTURE.md` for remediation roadmap.

---

## 9. Further Documentation

- **Architecture deep-dive:** `docs/ARCHITECTURE.md`
- **Database schema & seed data:** `docs/DATABASE.md`
- **User guide (screenshots/flow):** `docs/USER_GUIDE.md`

---

## 10. License & Contribution

Internal shop tool — add a `LICENSE` file if you plan to distribute. For contributions: keep `Main.kt` formatting with `kotlin.code.style=official` (`gradle.properties:1`) and test against all four DBs before PR.
