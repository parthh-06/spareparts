# User Guide — SparePartsApp

> For shop operators. Covers every button that actually exists in `Main.kt`. Pair with `README.md` (setup) and `docs/DATABASE.md` (data model).

---

## 1. Launch

- Double-click `SparePartsApp/SparePartsApp.exe` (bundled JRE — no install needed).
- Or run `.\gradlew run` from source.
- If a red error card says `Communications link failure` / `Unknown database`, see `README.md §4.1` to restore the four MySQL DBs. All screens show `ErrorCard:750` with the raw SQL exception message.

---

## 2. Home Screen (`Main.kt:50`)

```
WELCOME                                   (Girassol 50sp, 0x99FFFFFF)
┌──────────────┐  ┌──────────────┐
│  SUPPLIER    │  │  CUSTOMER    │  (250×132 dp, 0xFF353535, gold border)
└──────────────┘  └──────────────┘
┌──────────────┐  ┌──────────────┐
│  PRODUCT     │  │  INSURANCE   │
└──────────────┘  └──────────────┘
```
Click a tile to navigate; each module has `←` back to Home (`ScreenHeader:740`).

---

## 3. CUSTOMER (`Main.kt:64`)

**List:** `Name | Phone | [Bills] [Edit] [Del]` grouped by customer name (`Main.kt:98-113`). Search box filters by name or phone substring (`Main.kt:86`).

### 3.1 View Bills

- **Bills** button → Dialog `Bills for <name>` (`Main.kt:118`).
- Each bill card: `Bill #<id> — <date>` with `Paid/Unpaid` toggle button (`Main.kt:128`). Clicking it flips `bills.paid` and bumps `refresh` so the list updates. Cards enumerate `itemName xQty @ ₹price = ₹lineTotal` plus `Total: ₹<sum>`.

### 3.2 Edit / Delete Customer

- **Edit** (`Main.kt:148`): dialog with Name / Phone → `UPDATE customer SET name=?, phone=? WHERE id=?` (`Main.kt:157`).
- **Del** (`Main.kt:164`): confirmation `Delete <name> and all their bills?` → `DELETE FROM customer WHERE id=?` cascade deletes bills + items.

### 3.3 New Bill — `+ Add` → `AddCustomerBillDialog:177`

A 3-step wizard:

**Step 0 — Check**
- Enter `Customer Name` → **Check** button.
- App runs `SELECT id FROM customer WHERE name=?` (`Main.kt:253`). If found: shows `Found: <name> ✓` in green and jumps to Step 2. If not found: goes to Step 1.

**Step 1 — Create**
- Text: `Customer '<name>' not found. Enter phone to create:` → enter phone → **Create & Continue** → `INSERT INTO customer (name, phone)` (`Main.kt:263`), captures `foundId` via `generatedKeys`.

**Step 2 — Add Items**
- Header: `Adding items for: <name>`.
- Row: `Product | Qty | ₹` text fields (`Main.kt:214-219`).
- As you type product, **Suggestions** appear (up to 5 matches from `stock.products.product_name` containing substring, `Main.kt:222-228`). Clicking a suggestion fills product + price and sets Qty 1.
- **+ Add Item** (`Main.kt:231`): validates existing product, `qty>0`, `price` is double → appends `LineItem` to cart. Clears fields keeping price for next item.
- Cart section shows `Items (n):` lines + `Bill Total: ₹<sum>` (`Main.kt:238-242`).
- **Save Bill (₹<total>)** (`Main.kt:268`): transaction (see `docs/ARCHITECTURE.md §2.3`). On `Insufficient stock: <product>` error, a red card appears and nothing is saved (rollback). On success, dialog closes and customer list refreshes.

> **Stock rule:** Selling deducts from `stock.products.quantity`; if any line has insufficient stock, the entire bill is rejected.

---

## 4. SUPPLIER (`Main.kt:294`)

Mirrors Customer but for inbound inventory.

**List:** `Name | Phone | [View] [Edit] [Del]` (`Main.kt:328-341`). Search by name/phone. **View** opens `Receipts from <name>` dialog (`Main.kt:348`) grouped by `receiptId`: `Receipt #<id> — <date>` with same Paid toggle `UPDATE supplier_receipts` (`Main.kt:360`).

**Edit / Del** same as Customer but on `supplier` table (`Main.kt:378-403`).

### 4.1 New Receipt — `+ Add` → `AddSupplierReceiptDialog:407`

Same Step 0/1 flow on `supplier` table (`Main.kt:498-514`).

**Step 2 — Add Items** differs:

- Suggestions from `stock` (`Main.kt:454-460`), click to fill.
- If typed product **exists**: **+ Add Item** enabled only when `qty>0 && cost valid` (`Main.kt:464`).
- If typed product **does not exist**: warning `⚠ Product not in stock` (`Main.kt:469`), then `New product details:` fields `Type` + `Rack (1-30)` (`Main.kt:472`). **Create Product & Add Item** (`Main.kt:474`) immediately inserts `stock.products` with `quantity=0, supplied_by=<supplier name>, warranty='', cost_price=<cost>, selling_price=0, rack=<rack>` (`Main.kt:478`), captures new `id`, appends to cart, and adds to local `prodList` cache.
- Cart shows `Receipt Total: ₹<sum>`.
- **Save Receipt** (`Main.kt:517`): transaction — `INSERT supplier_receipts` → per line `INSERT supplier_receipt_items` + `UPDATE stock.products SET quantity = quantity + qty` (`Main.kt:527`). No stock check (always increment).

> **New product tip:** Products created this way start with selling price 0 — edit them in Product module to set a sell price.

---

## 5. PRODUCT (`Main.kt:542`)

**List:** Grouped by `type` header (gold Girassol 18sp, `Main.kt:570`), each row: `Product | Qty | Cost | Sell | Rack | [Edit] [Del]` (`Main.kt:572-585`). Search by product name or type (`Main.kt:557`).

### 5.1 Add Product — `+ Add` (`Main.kt:592`)

Fields (two per row for compactness):
- `Product Name` (full width)
- `Type | Qty`
- `Supplied By` (full width)
- `Warranty | Rack (1-30)`
- `Cost Price | Sell Price`

**Save** (`Main.kt:609-637`):
- If editing: `UPDATE products SET ... WHERE id=?` (`Main.kt:612`).
- If inserting: `INSERT INTO products ...` → then **auto-creates a supplier receipt** (`Main.kt:618-632`): finds/creates `suppliers.supplier` by `suppliedBy` (fallback `Stock Correction`), inserts `supplier_receipts (paid=TRUE)` + `supplier_receipt_items` so the inbound quantity appears in supplier history as an already-paid receipt.

### 5.2 Edit / Delete

- **Edit** opens same dialog pre-filled.
- **Del** confirmation `Delete <productName>?` → `DELETE FROM products WHERE id=?` (`Main.kt:644`). Warning: `supplier_receipt_items.stock_id` may become orphan (cross-DB, not enforced).

---

## 6. INSURANCE (`Main.kt:653`)

**List:** `Customer | Vehicle | Policy | Coverage | Expires | [Edit] [Del]` (`Main.kt:676-693`), sorted by `expiry_date` ascending (`Main.kt:662`). Search by name, vehicle, or policy (`Main.kt:668`). Expiry date shown in orange `0xFFFF9966`.

### 6.1 Add / Edit Policy — `+ Add` (`Main.kt:699`)

Two-per-row form:
- `Customer Name | Vehicle No`
- `DL No | PAN`
- `RC No` (full width)
- `Insurance Co | Policy Name`
- `Coverage ₹ | Expiry (YYYY-MM-DD)`

**Save** validates `coverage` as double fallback `0.0` (`Main.kt:720-722`), date is raw string stored as `DATE` — must be `YYYY-MM-DD` or MySQL rejects with `Incorrect date value` shown in `ErrorCard:674`.

Delete requires confirmation `Delete policy for <customer>?` (`Main.kt:730`).

---

## 7. Common Patterns

| Action | Where | What happens |
|--------|-------|--------------|
| Search | all 4 lists | in-memory `contains(ignoreCase)` filter; no DB query, instant |
| Paid toggle | Bills & Receipts dialogs | `paid = NOT paid` immediate, red/green pill reflects state |
| Error display | top of each list + inside dialogs | red `0xFF5C2E2E` card with `e.message` |
| Refresh | after every write | `refresh++` triggers `remember(refresh)` re-query |
| Autocomplete | Customer/Supplier Step 2 | up to 5 `product_name.contains(input, ignoreCase)` suggestions |

---

## 8. Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Error: Communications link failure` on every screen | MySQL not running / wrong port | `net start mysql` or check `localhost:3306` |
| `Unknown database 'customers'` | `customers_db.sql` not imported | Create per `docs/DATABASE.md §3.3` |
| `Data too long for column 'phone'` | Phone >15 chars | Keep phone ≤15 chars |
| `Check constraint 'products_chk_1' violated` / `quantity >=0` | Rack outside 1-30 or negative qty | Use Rack 1-30, Qty ≥0 |
| `Insufficient stock: X` red card on Save Bill | Selling more than available | Reduce qty or receive via Supplier first |
| `Incorrect date value` on insurance save | Expiry not `YYYY-MM-DD` | Use format `2027-06-15` |
| UI freezes for seconds | DB on slow machine / `remember` query on main thread | Wait or restart MySQL |

---

## 9. Keyboard / Mouse Tips

- `Tab` cycles text fields inside dialogs; `Enter` inside a field doesn't auto-submit — click the dialog's action button.
- Clicking a suggestion text in Add Bill/Receipt dialogs auto-fills price/quantity.
- The product suggestions list updates live as you type; exact case-insensitive match turns `+ Add Item` from disabled (gray inactive not shown — actually button hidden until match, `Main.kt:463` supplier, `Main.kt:233` customer) to active.

