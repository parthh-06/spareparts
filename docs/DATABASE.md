# Database — Schema, Seeds & Relationships

> Four logical MySQL databases on `localhost:3306`, credentials `root/root` (`Main.kt:24`). DDL files: `database/stock_db.sql`, `database/suppliers_db.sql`, `database/insurance_db.sql`, `database/migration.sql`. The `customers` DB DDL is **missing from the repo** — reconstructed below from queries in `Main.kt:76-80`.

---

## 1. Overview

| Database | Tables | File | Rows (seed) | Owner screen |
|----------|--------|------|-------------|--------------|
| `stock` | `products` | `stock_db.sql` | 15 | Product |
| `suppliers` | `supplier`, `supplier_receipts`, `supplier_receipt_items` | `suppliers_db.sql` | 5 + 5 + 8 | Supplier |
| `customers`† | `customer`, `bills`, `bill_items` | *inferred* | — | Customer |
| `insurance` | `insurance` | `insurance_db.sql` | 10 | Insurance |

† `customers` schema is not checked in. Run the CREATE below to recreate it.

Cross-DB logical links (not enforced by FK): `supplier_receipt_items.stock_id → stock.products.id` and `bills/bill_items` rely on `products.product_name` string matching.

---

## 2. Entity-Relationship Diagram

```
stock.products ─┐
  PK id         │        suppliers.supplier
  product_name  │         PK id ◄──┐
  type          │         name     │    suppliers.supplier_receipts
  quantity      │         phone    │     PK id
  supplied_by   │                  └───── FK supplier_id (CASCADE)
  warranty      │                        receipt_date
  rack 1-30     │◄── FK stock_id ──────── paid
  cost_price    │     suppliers.supplier_receipt_items
  selling_price │      PK id
                │      FK receipt_id → supplier_receipts.id (CASCADE)
                │      stock_id (→ stock.products.id, app-enforced)
                │      quantity CHECK >0
                │      cost_price
                │
                │        customers.customer
                │         PK id ◄──┐
                │         name     │    customers.bills
                │         phone    │     PK bill_id
                │                  └───── FK customer_id (CASCADE)
                │                        bill_date, paid
                │                              │
                │                              │  customers.bill_items
                │                              │   PK id
                │                              └── FK bill_id (CASCADE)
                │                                   item_name (string)
                │                                   quantity CHECK >0
                │                                   price
                │
                └──── (NAME match, not FK) ─────────┘

insurance.insurance (standalone, no FKs)
  PK id, customer_name, vehicle_number, driving_license, pan_number,
  rc_number, insurance_company, policy_name, coverage, expiry_date
```

---

## 3. DDL

### 3.1 `stock` — `database/stock_db.sql:4-14`

```sql
CREATE DATABASE IF NOT EXISTS stock;
USE stock;

CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    type VARCHAR(50) DEFAULT NULL,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    supplied_by VARCHAR(100) DEFAULT NULL,
    warranty VARCHAR(50) DEFAULT NULL,
    rack INT NOT NULL CHECK (rack BETWEEN 1 AND 30),
    cost_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00
);
```

**Seed** `stock_db.sql:16-31` — 15 rows:

| id | product | type | qty | supplier | rack | cost | sell |
|----|---------|------|-----|----------|------|------|------|
| 1 | Brake Pad Set | Brakes | 50 | Rajesh Auto Parts | 5 | 450 | 650 |
| 2 | Oil Filter | Filters | 100 | Sharma Motors | 2 | 120 | 200 |
| … | AC Compressor | AC | 10 | Gupta Traders | 25 | 3500 | 5200 |

### 3.2 `suppliers` — `database/suppliers_db.sql:4-26`

```sql
CREATE DATABASE IF NOT EXISTS suppliers;
USE suppliers;

CREATE TABLE IF NOT EXISTS supplier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL
);

CREATE TABLE IF NOT EXISTS supplier_receipts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT NOT NULL,
    receipt_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(10, 2) DEFAULT 0.00,   -- legacy, unused by app (computed in UI)
    paid BOOLEAN DEFAULT FALSE,           -- added via migration.sql:5
    FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS supplier_receipt_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    receipt_id INT NOT NULL,
    stock_id INT NOT NULL,                -- logically → stock.products.id (no cross-DB FK)
    quantity INT NOT NULL CHECK (quantity > 0),
    cost_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (receipt_id) REFERENCES supplier_receipts(id) ON DELETE CASCADE
);
```

Seeds: 5 suppliers (`suppliers_db.sql:28-33`), 5 receipts (`:35-40`), 8 items (`:42-50`). Example: Receipt #1 (Rajesh, 2026-07-01) has Brake Pad Set ×10 @450 + stock_id 2 ×5 @1200.

### 3.3 `customers` — **Reconstructed** (missing file)

Inferred from `Main.kt:76-80`, `Main.kt:157`, `Main.kt:263`, `Main.kt:272-279`:

```sql
CREATE DATABASE IF NOT EXISTS customers;
USE customers;

CREATE TABLE IF NOT EXISTS customer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL
);

CREATE TABLE IF NOT EXISTS bills (
    bill_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid BOOLEAN DEFAULT FALSE,              -- database/migration.sql:2
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bill_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bill_id INT NOT NULL,
    item_name VARCHAR(100) NOT NULL,        -- denormalized product_name from stock
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL,          -- selling_price snapshot at sale time
    FOREIGN KEY (bill_id) REFERENCES bills(bill_id) ON DELETE CASCADE
);
```

> **Check:** Save `database/customers_db.sql` with the DDL above and commit it — the current repo has no customers seed data.

### 3.4 `insurance` — `database/insurance_db.sql:4-15`

```sql
CREATE DATABASE IF NOT EXISTS insurance;
USE insurance;

CREATE TABLE IF NOT EXISTS insurance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    vehicle_number VARCHAR(20) NOT NULL,
    driving_license VARCHAR(50) DEFAULT NULL,
    pan_number VARCHAR(20) DEFAULT NULL,
    rc_number VARCHAR(50) DEFAULT NULL,
    insurance_company VARCHAR(100) NOT NULL,
    policy_name VARCHAR(100) NOT NULL,
    coverage DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    expiry_date DATE NOT NULL
);
```

Seed `insurance_db.sql:17-26` — 10 policies (Amit Verma / New India Assurance Comprehensive 5L exp 2027-06-15 … Pooja Desai / Tata AIG Third Party 2.75L exp 2026-11-12). Ordered by `expiry_date` in `Main.kt:662`.

### 3.5 Migrations — `database/migration.sql`

```sql
ALTER TABLE bills ADD COLUMN paid BOOLEAN DEFAULT FALSE;              -- customers
ALTER TABLE supplier_receipts ADD COLUMN paid BOOLEAN DEFAULT FALSE;   -- suppliers
```

Run only if the DB was created before the `paid` feature. Current `CREATE TABLE` statements already include `paid`, so fresh installs don't need this.

---

## 4. Key Queries (as used in app)

| Screen | Query | Location |
|--------|-------|----------|
| Customer list | `SELECT c.id,c.name,c.phone,b.bill_id,b.bill_date,b.paid,bi.item_name,bi.quantity,bi.price FROM customer c JOIN bills b ON c.id=b.customer_id JOIN bill_items bi ON b.bill_id=bi.bill_id ORDER BY b.bill_date DESC` | `Main.kt:76` |
| Customer check | `SELECT id,phone FROM customer WHERE name=?` | `Main.kt:253` |
| Bill paid toggle | `UPDATE bills SET paid = NOT paid WHERE bill_id=?` | `Main.kt:130` |
| Stock deduct | `UPDATE stock.products SET quantity = quantity - ? WHERE id=? AND quantity >= ?` | `Main.kt:277` |
| Supplier list | `SELECT s.id,s.name,s.phone,r.id AS receipt_id,r.receipt_date,r.paid,p.product_name,ri.quantity,ri.cost_price FROM supplier s JOIN supplier_receipts r ON s.id=r.supplier_id JOIN supplier_receipt_items ri ON r.id=ri.receipt_id JOIN stock.products p ON ri.stock_id=p.id ORDER BY r.receipt_date DESC` | `Main.kt:306` |
| Receipt paid toggle | `UPDATE supplier_receipts SET paid = NOT paid WHERE id=?` | `Main.kt:360` |
| Stock increment | `UPDATE stock.products SET quantity = quantity + ? WHERE id=?` | `Main.kt:527` |
| Product list | `SELECT * FROM products ORDER BY product_name` | `Main.kt:551` |
| Product insert + side-effect receipt | `INSERT INTO products ...` then `INSERT INTO supplier + supplier_receipts + supplier_receipt_items` | `Main.kt:615-631` |
| Insurance list | `SELECT * FROM insurance ORDER BY expiry_date` | `Main.kt:662` |

---

## 5. Constraints & Gotchas

- **`supplier_receipt_items.stock_id` has no FK to `stock.products`** — MySQL doesn't support cross-database foreign keys. Integrity is app-enforced; orphan `stock_id` can exist if a product is deleted. Mitigation: use `ON DELETE RESTRICT` logic in app or merge DBs into one with schemas.
- **`bill_items.item_name` is a string snapshot**, not `stock_id` — renaming a product won't affect past bills (intentional), but the customer receipt dialog can't re-join to `products` for current pricing.
- **`supplier_receipts.total` column exists but is never written by `Main.kt`** — receipts compute totals in Kotlin (`sumOf { qty*price }`). Column stays `0.00` for new rows.
- **`quantity` CHECKs** are present in DDL but MySQL <8.0.16 silently ignores `CHECK`; verify server version if you rely on it.
- **Charset:** DDL doesn't specify `utf8mb4`; defaults to server charset — may affect ₹ symbol storage (prices are `DECIMAL`, so display-side only).

---

## 6. Setup & Teardown Scripts

**Fresh install (PowerShell):**
```powershell
mysql -u root -proot < database/stock_db.sql
mysql -u root -proot < database/suppliers_db.sql
mysql -u root -proot < database/insurance_db.sql
# Create customers (see DDL §3.3) — save as database/customers_db.sql first
mysql -u root -proot < database/customers_db.sql
```

**Reset during development:**
```powershell
mysql -u root -proot -e "DROP DATABASE IF EXISTS stock; DROP DATABASE IF EXISTS suppliers; DROP DATABASE IF EXISTS customers; DROP DATABASE IF EXISTS insurance;"
```

**Ad-hoc inspection:**
```sql
USE stock; SELECT product_name, quantity, selling_price FROM products WHERE quantity < 10;
USE suppliers; SELECT s.name, r.receipt_date, r.paid, SUM(ri.quantity*ri.cost_price) AS total
  FROM supplier s JOIN supplier_receipts r ON s.id=r.supplier_id
  JOIN supplier_receipt_items ri ON r.id=ri.receipt_id GROUP BY r.id;
USE customers; SELECT c.name, COUNT(b.bill_id) AS bills, SUM(bi.quantity*bi.price) AS lifetime_value
  FROM customer c JOIN bills b ON c.id=b.customer_id JOIN bill_items bi ON b.bill_id=bi.bill_id GROUP BY c.id;
USE insurance; SELECT customer_name, vehicle_number, policy_name, expiry_date FROM insurance WHERE expiry_date < CURDATE() + INTERVAL 30 DAY;
```
