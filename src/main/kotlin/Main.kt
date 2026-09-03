import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import java.sql.DriverManager
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight

val girassol = FontFamily(Font("fonts/girassol.ttf"))
private const val DB_USER = "root"
private const val DB_PASS = "root"

private object AppColors {
    val Bg = Color(0xFF2E2E2E)
    val Card = Color(0xFF3A3A3A)
    val Gold = Color(0xFFD5C875)
    val Dark = Color(0xFF353535)
    val Green = Color(0xFF1C4A1E)
    val Red = Color(0xFF760E03)
    val Navy = Color(0xFF23204B)
}

private inline fun <T> withDb(db: String, block: (java.sql.Connection) -> T): T =
    DriverManager.getConnection("jdbc:mysql://localhost:3306/$db", DB_USER, DB_PASS).use(block)

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(text, Modifier.weight(weight), AppColors.Gold, fontWeight = FontWeight.Bold)
}

private fun togglePaid(db: String, sql: String, id: Int, onSuccess: () -> Unit) {
    try { withDb(db) { c -> c.prepareStatement(sql).apply { setInt(1, id) }.executeUpdate() }; onSuccess() } catch (_: Exception) { }
}

data class CustomerRow(val customerId: Int, val name: String, val phone: String, val billId: Int, val billDate: String, val paid: Boolean, val itemName: String, val quantity: Int, val price: Double)
data class SupplierRow(val supplierId: Int, val name: String, val phone: String, val receiptId: Int, val receiptDate: String, val paid: Boolean, val productName: String, val quantity: Int, val costPrice: Double)
data class ProductRow(val id: Int, val productName: String, val type: String, val quantity: Int, val suppliedBy: String, val warranty: String, val rack: Int, val costPrice: Double, val sellingPrice: Double)
data class InsuranceRow(val id: Int, val customerName: String, val vehicleNumber: String, val drivingLicense: String, val panNumber: String, val rcNumber: String, val insuranceCompany: String, val policyName: String, val coverage: Double, val expiryDate: String)
data class LineItem(val stockId: Int, val productName: String, val qty: Int, val price: Double)

fun main() = application {
    Window(onCloseRequest = ::exitApplication) { App() }
}

@Composable
fun App() {
    var screen by remember { mutableStateOf("home") }
    when (screen) {
        "home"      -> HomeScreen { screen = it }
        "supplier"  -> SupplierScreen { screen = "home" }
        "customer"  -> CustomerScreen { screen = "home" }
        "product"   -> productScreen { screen = "home" }
        "insurance" -> insuranceScreen { screen = "home" }
    }
}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.Bg).padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("WELCOME", fontFamily = girassol, color = Color(0x99FFFFFFA), fontSize = 50.sp)

        Spacer(Modifier.height(30.dp))

        Row { HomeBtn("SUPPLIER") { onNavigate("supplier") }; Spacer(Modifier.width(20.dp)); HomeBtn("CUSTOMER") { onNavigate("customer") } }
        Spacer(Modifier.height(20.dp))
        Row { HomeBtn("PRODUCT") { onNavigate("product") }; Spacer(Modifier.width(20.dp)); HomeBtn("INSURANCE") { onNavigate("insurance") } }
    }
}
@Composable private fun HomeBtn(t: String, c: () -> Unit) = Button(onClick = c, modifier = Modifier.size(250.dp, 132.dp).border(1.dp, AppColors.Gold), colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text(t, fontFamily = girassol, fontSize = 24.sp, color = Color.White) }

@Composable
fun CustomerScreen(onBack: () -> Unit) {
    var refresh by remember { mutableStateOf(0) }; var search by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<String?>(null) }
    var editingCustomer by remember { mutableStateOf<CustomerRow?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deletingCustomer by remember { mutableStateOf<CustomerRow?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    val allRows = remember(refresh) {
        val list = mutableListOf<CustomerRow>()
        try {
            withDb("customers") { c ->
                val r = c.createStatement().executeQuery("""
                    SELECT c.id, c.name, c.phone, b.bill_id, b.bill_date, b.paid, bi.item_name, bi.quantity, bi.price
                    FROM customer c JOIN bills b ON c.id = b.customer_id JOIN bill_items bi ON b.bill_id = bi.bill_id
                    ORDER BY b.bill_date DESC
                """.trimIndent())
                while (r.next()) list.add(CustomerRow(r.getInt("id"), r.getString("name"), r.getString("phone"), r.getInt("bill_id"), r.getString("bill_date"), r.getBoolean("paid"), r.getString("item_name"), r.getInt("quantity"), r.getDouble("price")))
            }
            errorMsg = ""
        } catch (e: Exception) { errorMsg = e.message ?: "Error" }
        list
    }
    val filtered = if (search.isBlank()) allRows else allRows.filter { it.name.contains(search, ignoreCase = true) || it.phone.contains(search) }
    val grouped = filtered.groupBy { it.name }

    Box(Modifier.fillMaxSize().background(AppColors.Bg)) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader("CUSTOMERS", onBack) { showAdd = true }
            Spacer(Modifier.height(8.dp)); SearchBar(search, { search = it }, "Search customers...")
            if (errorMsg.isNotBlank()) ErrorCard(errorMsg)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) { HeaderCell("Name", 2f); HeaderCell("Phone", 2f); HeaderCell("Actions", 2.5f) }
            Divider(color = Color.Gray, thickness = 1.dp)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 6.dp)) {
                grouped.forEach { (name, rows) ->
                    val phone = rows.first().phone
                    DataCard {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, Modifier.weight(2f), color = Color.White, fontSize = 15.sp)
                            Text(phone, Modifier.weight(2f), color = Color.LightGray, fontSize = 14.sp)
                            Row(Modifier.weight(2.5f), horizontalArrangement = Arrangement.End) {
                                Btn("Bills", AppColors.Gold) { selectedCustomer = name }
                                Spacer(Modifier.width(4.dp))
                                Btn("Edit", AppColors.Navy) { editingCustomer = rows.first() }
                                Spacer(Modifier.width(4.dp))
                                Btn("Del", AppColors.Red) { deletingCustomer = rows.first() }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCustomer?.let { nm ->
        val items = allRows.filter { it.name == nm }
        AlertDialog(onDismissRequest = { selectedCustomer = null }, title = { Text("Bills for $nm", fontFamily = girassol, fontSize = 20.sp) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                items.groupBy { it.billId }.forEach { (bid, bs) ->
                    val tot = bs.sumOf { it.quantity * it.price }; val pd = bs.first().paid
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), backgroundColor = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Bill #$bid — ${bs.first().billDate.take(10)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Button(onClick = {
                                    try { withDb("customers") { c -> c.prepareStatement("UPDATE bills SET paid = NOT paid WHERE bill_id = ?").apply { setInt(1, bid) }.executeUpdate() }; refresh++ } catch (_: Exception) { selectedCustomer = null }
                                }, colors = ButtonDefaults.buttonColors(backgroundColor = if (pd) AppColors.Green else Color(0xFF9E9E9E)), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(26.dp)) { Text(if (pd) "Paid" else "Unpaid", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                            bs.forEach { itm -> Row(Modifier.fillMaxWidth()) { Text(itm.itemName, Modifier.weight(2f), fontSize = 13.sp); Text("x${itm.quantity}", Modifier.weight(1f), fontSize = 13.sp); Text("₹${itm.price}", Modifier.weight(1f), fontSize = 13.sp); Text("₹${itm.quantity * itm.price}", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Total: ₹$tot", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }, confirmButton = { Button(onClick = { selectedCustomer = null }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text("Close", color = Color.White) } }, shape = RoundedCornerShape(12.dp))
    }

    if (showAdd) AddCustomerBillDialog(refresh = { refresh++ }) { showAdd = false }

    editingCustomer?.let { cust ->
        var nm by remember { mutableStateOf(cust.name) }; var ph by remember { mutableStateOf(cust.phone) }
        AlertDialog(onDismissRequest = { editingCustomer = null }, title = { Text("Edit Customer", fontFamily = girassol) }, text = {
            Column {
                OutlinedTextField(value = nm, onValueChange = { nm = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp)); OutlinedTextField(value = ph, onValueChange = { ph = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }, confirmButton = {
            Button(onClick = {
                try { withDb("customers") { c -> c.prepareStatement("UPDATE customer SET name=?, phone=? WHERE id=?").apply { setString(1, nm); setString(2, ph); setInt(3, cust.customerId) }.executeUpdate() }; refresh++; editingCustomer = null } catch (e: Exception) { errorMsg = e.message ?: "Update failed" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text("Update", color = Color.White) }
        }, dismissButton = { TextButton(onClick = { editingCustomer = null }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
    }

    deletingCustomer?.let { cust ->
        AlertDialog(onDismissRequest = { deletingCustomer = null }, title = { Text("Delete Customer", fontFamily = girassol) }, text = { Text("Delete ${cust.name} and all their bills?") },
            confirmButton = {
                Button(onClick = {
                    try { withDb("customers") { c -> c.prepareStatement("DELETE FROM customer WHERE id=?").apply { setInt(1, cust.customerId) }.executeUpdate() }; refresh++; deletingCustomer = null } catch (e: Exception) { errorMsg = e.message ?: "Delete failed" }
                }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Red)) { Text("Delete", color = Color.White) }
            }, dismissButton = { TextButton(onClick = { deletingCustomer = null }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
    }
}

@Composable
private fun AddCustomerBillDialog(refresh: () -> Unit, onClose: () -> Unit) {
    var step by remember { mutableStateOf(0) }; var nm by remember { mutableStateOf("") }; var ph by remember { mutableStateOf("") }; var foundId by remember { mutableStateOf<Int?>(null) }; var msg by remember { mutableStateOf("") }
    var prodName by remember { mutableStateOf("") }; var itemQty by remember { mutableStateOf("1") }; var itemPrice by remember { mutableStateOf("0") }
    val lineItems = remember { mutableStateListOf<LineItem>() }
    var prodList by remember { mutableStateOf(listOf<ProductRow>()) }

    LaunchedEffect(Unit) {
        try {
            withDb("stock") { c ->
                val r = c.createStatement().executeQuery("SELECT id, product_name, selling_price FROM products ORDER BY product_name")
                val l = mutableListOf<ProductRow>()
                while (r.next()) l.add(ProductRow(r.getInt("id"), r.getString("product_name"), "", 0, "", "", 0, 0.0, r.getDouble("selling_price")))
                prodList = l
            }
        } catch (e: Exception) { msg = "Failed to load products: ${e.message}" }
    }

    AlertDialog(onDismissRequest = onClose, title = { Text(if (step < 2) "New Bill" else "Add Items to Bill", fontFamily = girassol) }, text = {
        Column(Modifier.width(400.dp)) {
            when (step) {
                0 -> {
                    OutlinedTextField(value = nm, onValueChange = { nm = it }, label = { Text("Customer Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (msg.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(msg, color = if (msg.startsWith("Found")) Color(
                        0xFF084908
                    ) else Color(0xFFFF6B6B), fontSize = 13.sp) }
                }
                1 -> {
                    Text("Customer '$nm' not found. Enter phone to create:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp)); OutlinedTextField(value = ph, onValueChange = { ph = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                2 -> {
                    if (msg.isNotBlank()) {
                        Card(Modifier.fillMaxWidth().padding(bottom = 6.dp), backgroundColor = Color(0xFF5C2E2E), shape = RoundedCornerShape(6.dp)) {
                            Text(msg, color = Color(0xFFFF6B6B), modifier = Modifier.padding(10.dp), fontSize = 13.sp)
                        }
                    }
                    Text("Adding items for: $nm", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = prodName, onValueChange = { prodName = it }, label = { Text("Product") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Qty") }, singleLine = true, modifier = Modifier.width(60.dp))
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(value = itemPrice, onValueChange = { itemPrice = it }, label = { Text("₹") }, singleLine = true, modifier = Modifier.width(70.dp))
                    }
                    if (prodName.isNotBlank()) {
                        val matches = prodList.filter { it.productName.contains(prodName, ignoreCase = true) }
                        if (matches.isNotEmpty()) {
                            Text("Suggestions:", fontSize = 11.sp, color = Color.Gray)
                            matches.take(5).forEach { m ->
                                Text("${m.productName} (₹${m.sellingPrice})", fontSize = 12.sp, color = AppColors.Navy, modifier = Modifier.clickable { prodName = m.productName; itemPrice = m.sellingPrice.toString(); itemQty = "1" })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val match = prodList.find { it.productName.equals(prodName, ignoreCase = true) }
                        if (match != null && itemQty.toIntOrNull() != null && itemQty.toInt() > 0 && itemPrice.toDoubleOrNull() != null) {
                            lineItems.add(LineItem(match.id, match.productName, itemQty.toInt(), itemPrice.toDouble()))
                            prodName = ""; itemQty = "1"; itemPrice = match.sellingPrice.toString()
                        }
                    }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Navy)) { Text("+ Add Item", color = Color.White, fontSize = 13.sp) }
                    if (lineItems.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp)); Divider(color = Color.Gray, thickness = 1.dp); Spacer(Modifier.height(4.dp))
                        Text("Items (${lineItems.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        lineItems.forEach { li -> Text("${li.productName} x${li.qty} @ ₹${li.price} = ₹${li.qty * li.price}", fontSize = 12.sp) }
                        Text("Bill Total: ₹${lineItems.sumOf { it.qty * it.price }}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }, confirmButton = {
        when (step) {
            0 -> Button(onClick = {
                if (nm.isBlank()) return@Button
                try {
                    val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/customers", DB_USER, DB_PASS)
                    val r = c.prepareStatement("SELECT id, phone FROM customer WHERE name = ?").apply { setString(1, nm) }.executeQuery()
                    if (r.next()) { foundId = r.getInt("id"); ph = r.getString("phone"); msg = "Found: $nm ✓"; step = 2 }
                    else { msg = "Not found"; step = 1 }
                    c.close()
                } catch (e: Exception) { msg = e.message ?: "Error" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text("Check", color = Color.White) }
            1 -> Button(onClick = {
                if (ph.isBlank()) return@Button
                try {
                    val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/customers", DB_USER, DB_PASS)
                    val p = c.prepareStatement("INSERT INTO customer (name, phone) VALUES (?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS)
                    p.setString(1, nm); p.setString(2, ph); p.executeUpdate()
                    val r = p.generatedKeys; r.next(); foundId = r.getInt(1); c.close(); step = 2
                } catch (e: Exception) { msg = e.message ?: "Error" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Green)) { Text("Create & Continue", color = Color.White) }
            2 -> Button(enabled = lineItems.isNotEmpty(), onClick = {
                try {
                    val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/customers", DB_USER, DB_PASS)
                    c.autoCommit = false
                    try {
                        val pb = c.prepareStatement("INSERT INTO bills (customer_id, bill_date, paid) VALUES (?, NOW(), FALSE)", java.sql.Statement.RETURN_GENERATED_KEYS)
                        pb.setInt(1, foundId!!); pb.executeUpdate()
                        val rb = pb.generatedKeys; rb.next(); val billId = rb.getInt(1)
                        val pi = c.prepareStatement("INSERT INTO bill_items (bill_id, item_name, quantity, price) VALUES (?, ?, ?, ?)")
                        val ps = c.prepareStatement("UPDATE stock.products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?")
                        lineItems.forEach { li ->
                            pi.setInt(1, billId); pi.setString(2, li.productName); pi.setInt(3, li.qty); pi.setDouble(4, li.price); pi.executeUpdate()
                            ps.setInt(1, li.qty); ps.setInt(2, li.stockId); ps.setInt(3, li.qty)
                            if (ps.executeUpdate() == 0) throw Exception("Insufficient stock: ${li.productName}")
                        }
                        c.commit(); refresh()
                    } catch (ex: Exception) { c.rollback(); msg = ex.message ?: "Transaction failed"; return@Button
                    } finally { c.autoCommit = true; c.close() }
                    onClose()
                } catch (e: Exception) { msg = e.message ?: "Error" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Green)) { Text("Save Bill (₹${lineItems.sumOf { it.qty * it.price }})", color = Color.White) }
        }
    }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
}

@Composable
fun SupplierScreen(onBack: () -> Unit) {
    var refresh by remember { mutableStateOf(0) }; var search by remember { mutableStateOf("") }
    var selectedSupplier by remember { mutableStateOf<String?>(null) }
    var editingSupplier by remember { mutableStateOf<SupplierRow?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deletingSupplier by remember { mutableStateOf<SupplierRow?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    val allRows = remember(refresh) {
        val list = mutableListOf<SupplierRow>()
        try {
            withDb("suppliers") { c ->
                val r = c.createStatement().executeQuery("""
                    SELECT s.id, s.name, s.phone, r.id AS receipt_id, r.receipt_date, r.paid, p.product_name, ri.quantity, ri.cost_price
                    FROM supplier s JOIN supplier_receipts r ON s.id = r.supplier_id JOIN supplier_receipt_items ri ON r.id = ri.receipt_id
                    JOIN stock.products p ON ri.stock_id = p.id ORDER BY r.receipt_date DESC
                """.trimIndent())
                while (r.next()) list.add(SupplierRow(r.getInt("id"), r.getString("name"), r.getString("phone"), r.getInt("receipt_id"), r.getString("receipt_date"), r.getBoolean("paid"), r.getString("product_name"), r.getInt("quantity"), r.getDouble("cost_price")))
            }
            errorMsg = ""
        } catch (e: Exception) { errorMsg = e.message ?: "Error" }
        list
    }
    val filtered = if (search.isBlank()) allRows else allRows.filter { it.name.contains(search, ignoreCase = true) || it.phone.contains(search) }
    val grouped = filtered.groupBy { it.name }

    Box(Modifier.fillMaxSize().background(AppColors.Bg)) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader("SUPPLIERS", onBack) { showAdd = true }
            Spacer(Modifier.height(8.dp)); SearchBar(search, { search = it }, "Search suppliers...")
            if (errorMsg.isNotBlank()) ErrorCard(errorMsg)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) { HeaderCell("Name", 2f); HeaderCell("Phone", 2f); HeaderCell("Actions", 2.5f) }
            Divider(color = Color.Gray, thickness = 1.dp)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 6.dp)) {
                grouped.forEach { (name, rows) ->
                    val phone = rows.first().phone
                    DataCard {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, Modifier.weight(2f), color = Color.White, fontSize = 15.sp)
                            Text(phone, Modifier.weight(2f), color = Color.LightGray, fontSize = 14.sp)
                            Row(Modifier.weight(2.5f), horizontalArrangement = Arrangement.End) {
                                Btn("View", AppColors.Green) { selectedSupplier = name }
                                Spacer(Modifier.width(4.dp))
                                Btn("Edit", AppColors.Navy) { editingSupplier = rows.first() }
                                Spacer(Modifier.width(4.dp))
                                Btn("Del", AppColors.Red) { deletingSupplier = rows.first() }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSupplier?.let { nm ->
        val items = allRows.filter { it.name == nm }
        AlertDialog(onDismissRequest = { selectedSupplier = null }, title = { Text("Receipts from $nm", fontFamily = girassol, fontSize = 20.sp) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                items.groupBy { it.receiptId }.forEach { (rid, rs) ->
                    val tot = rs.sumOf { it.quantity * it.costPrice }; val pd = rs.first().paid
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), backgroundColor = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Receipt #$rid — ${rs.first().receiptDate.take(10)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Button(onClick = {
                                    try { withDb("suppliers") { c -> c.prepareStatement("UPDATE supplier_receipts SET paid = NOT paid WHERE id = ?").apply { setInt(1, rid) }.executeUpdate() }; refresh++ } catch (_: Exception) { selectedSupplier = null }
                                }, colors = ButtonDefaults.buttonColors(backgroundColor = if (pd) AppColors.Green else Color(0xFF9E9E9E)), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(26.dp)) { Text(if (pd) "Paid" else "Unpaid", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                            rs.forEach { itm -> Row(Modifier.fillMaxWidth()) { Text(itm.productName, Modifier.weight(2f), fontSize = 13.sp); Text("x${itm.quantity}", Modifier.weight(1f), fontSize = 13.sp); Text("₹${itm.costPrice}", Modifier.weight(1f), fontSize = 13.sp); Text("₹${itm.quantity * itm.costPrice}", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Total: ₹$tot", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }, confirmButton = { Button(onClick = { selectedSupplier = null }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text("Close", color = Color.White) } }, shape = RoundedCornerShape(12.dp))
    }

    if (showAdd) AddSupplierReceiptDialog(refresh = { refresh++ }) { showAdd = false }

    editingSupplier?.let { sup ->
        var nm by remember { mutableStateOf(sup.name) }; var ph by remember { mutableStateOf(sup.phone) }
        AlertDialog(onDismissRequest = { editingSupplier = null }, title = { Text("Edit Supplier", fontFamily = girassol) }, text = {
            Column {
                OutlinedTextField(value = nm, onValueChange = { nm = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp)); OutlinedTextField(value = ph, onValueChange = { ph = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }, confirmButton = {
            Button(onClick = {
                try { withDb("suppliers") { c -> c.prepareStatement("UPDATE supplier SET name=?, phone=? WHERE id=?").apply { setString(1, nm); setString(2, ph); setInt(3, sup.supplierId) }.executeUpdate() }; refresh++; editingSupplier = null } catch (e: Exception) { errorMsg = e.message ?: "Update failed" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text("Update", color = Color.White) }
        }, dismissButton = { TextButton(onClick = { editingSupplier = null }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
    }

    deletingSupplier?.let { sup ->
        AlertDialog(onDismissRequest = { deletingSupplier = null }, title = { Text("Delete Supplier", fontFamily = girassol) }, text = { Text("Delete ${sup.name} and all their receipts?") },
            confirmButton = {
                Button(onClick = {
                    try { withDb("suppliers") { c -> c.prepareStatement("DELETE FROM supplier WHERE id=?").apply { setInt(1, sup.supplierId) }.executeUpdate() }; refresh++; deletingSupplier = null } catch (e: Exception) { errorMsg = e.message ?: "Delete failed" }
                }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Red)) { Text("Delete", color = Color.White) }
            }, dismissButton = { TextButton(onClick = { deletingSupplier = null }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
    }
}

@Composable
private fun AddSupplierReceiptDialog(refresh: () -> Unit, onClose: () -> Unit) {
    var step by remember { mutableStateOf(0) }; var nm by remember { mutableStateOf("") }; var ph by remember { mutableStateOf("") }; var foundId by remember { mutableStateOf<Int?>(null) }; var msg by remember { mutableStateOf("") }
    var prodName by remember { mutableStateOf("") }; var itemQty by remember { mutableStateOf("1") }; var itemCost by remember { mutableStateOf("0") }
    val lineItems = remember { mutableStateListOf<LineItem>() }
    var prodList by remember { mutableStateOf(listOf<ProductRow>()) }
    var newType by remember { mutableStateOf("") }; var newRack by remember { mutableStateOf("1") }

    LaunchedEffect(Unit) {
        try {
            withDb("stock") { c ->
                val r = c.createStatement().executeQuery("SELECT id, product_name, cost_price FROM products ORDER BY product_name")
                val l = mutableListOf<ProductRow>()
                while (r.next()) l.add(ProductRow(r.getInt("id"), r.getString("product_name"), "", 0, "", "", 0, r.getDouble("cost_price"), 0.0))
                prodList = l
            }
        } catch (e: Exception) { msg = "Failed to load products: ${e.message}" }
    }

    fun resetNewForm() { prodName = ""; itemQty = "1"; itemCost = "0"; newType = ""; newRack = "1" }

    AlertDialog(onDismissRequest = onClose, title = { Text(if (step < 2) "New Receipt" else "Add Items to Receipt", fontFamily = girassol) }, text = {
        Column(Modifier.width(420.dp).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> {
                    OutlinedTextField(value = nm, onValueChange = { nm = it }, label = { Text("Supplier Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (msg.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(msg, color = if (msg.startsWith("Found")) AppColors.Green else Color(0xFFFF6B6B), fontSize = 13.sp) }
                }
                1 -> {
                    Text("Supplier '$nm' not found. Enter phone to create:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp)); OutlinedTextField(value = ph, onValueChange = { ph = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                2 -> {
                    if (msg.isNotBlank()) {
                        Card(Modifier.fillMaxWidth().padding(bottom = 6.dp), backgroundColor = Color(0xFF5C2E2E), shape = RoundedCornerShape(6.dp)) {
                            Text(msg, color = Color(0xFFFF6B6B), modifier = Modifier.padding(10.dp), fontSize = 13.sp)
                        }
                    }
                    Text("Adding items for: $nm", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = prodName, onValueChange = { prodName = it }, label = { Text("Product") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Qty") }, singleLine = true, modifier = Modifier.width(60.dp))
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(value = itemCost, onValueChange = { itemCost = it }, label = { Text("₹") }, singleLine = true, modifier = Modifier.width(70.dp))
                    }
                    val match = prodList.find { it.productName.equals(prodName, ignoreCase = true) }
                    if (prodName.isNotBlank()) {
                        val matches = prodList.filter { it.productName.contains(prodName, ignoreCase = true) }
                        if (matches.isNotEmpty()) {
                            Text("Suggestions:", fontSize = 11.sp, color = Color.Gray)
                            matches.take(5).forEach { m ->
                                Text("${m.productName} (₹${m.costPrice})", fontSize = 12.sp, color = AppColors.Navy, modifier = Modifier.clickable { prodName = m.productName; itemCost = m.costPrice.toString(); itemQty = "1" })
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (match != null) {
                        Button(enabled = itemQty.toIntOrNull() != null && itemQty.toInt() > 0 && itemCost.toDoubleOrNull() != null, onClick = {
                            lineItems.add(LineItem(match.id, match.productName, itemQty.toInt(), itemCost.toDouble()))
                            prodName = ""; itemQty = "1"; itemCost = match.costPrice.toString()
                        }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Navy)) { Text("+ Add Item", color = Color.White, fontSize = 13.sp) }
                    } else if (prodName.isNotBlank()) {
                        Text("⚠ Product not in stock", fontSize = 12.sp, color = Color(0xFFFF9966))
                        Spacer(Modifier.height(4.dp))
                        Text("New product details:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Row { OutlinedTextField(value = newType, onValueChange = { newType = it }, label = { Text("Type") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = newRack, onValueChange = { newRack = it }, label = { Text("Rack (1-30)") }, singleLine = true, modifier = Modifier.weight(1f)) }
                        Spacer(Modifier.height(4.dp))
                        Button(enabled = itemQty.toIntOrNull() != null && itemQty.toInt() > 0 && itemCost.toDoubleOrNull() != null && newType.isNotBlank(), onClick = {
                            try {
                                val sc = DriverManager.getConnection("jdbc:mysql://localhost:3306/stock", DB_USER, DB_PASS)
                                val ip = sc.prepareStatement("INSERT INTO products (product_name, type, quantity, supplied_by, warranty, rack, cost_price, selling_price) VALUES (?, ?, 0, ?, '', ?, ?, 0)", java.sql.Statement.RETURN_GENERATED_KEYS)
                                ip.setString(1, prodName); ip.setString(2, newType); ip.setString(3, nm); ip.setInt(4, newRack.toIntOrNull() ?: 1); ip.setDouble(5, itemCost.toDoubleOrNull() ?: 0.0); ip.executeUpdate()
                                val gk = ip.generatedKeys; gk.next(); val newId = gk.getInt(1)
                                sc.close()
                                lineItems.add(LineItem(newId, prodName, itemQty.toInt(), itemCost.toDouble()))
                                prodList = prodList + ProductRow(newId, prodName, newType, 0, nm, "", newRack.toIntOrNull() ?: 1, itemCost.toDoubleOrNull() ?: 0.0, 0.0)
                                resetNewForm()
                            } catch (e: Exception) { msg = e.message ?: "Create failed" }
                        }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Green)) { Text("Create Product & Add Item", color = Color.White, fontSize = 13.sp) }
                    }
                    if (lineItems.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp)); Divider(color = Color.Gray, thickness = 1.dp); Spacer(Modifier.height(4.dp))
                        Text("Items (${lineItems.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        lineItems.forEach { li -> Text("${li.productName} x${li.qty} @ ₹${li.price} = ₹${li.qty * li.price}", fontSize = 12.sp) }
                        Text("Receipt Total: ₹${lineItems.sumOf { it.qty * it.price }}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }, confirmButton = {
        when (step) {
            0 -> Button(onClick = {
                if (nm.isBlank()) return@Button
                try {
                    val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/suppliers", DB_USER, DB_PASS)
                    val r = c.prepareStatement("SELECT id, phone FROM supplier WHERE name = ?").apply { setString(1, nm) }.executeQuery()
                    if (r.next()) { foundId = r.getInt("id"); ph = r.getString("phone"); msg = "Found: $nm ✓"; step = 2 }
                    else { msg = "Not found"; step = 1 }
                    c.close()
                } catch (e: Exception) { msg = e.message ?: "Error" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text("Check", color = Color.White) }
            1 -> Button(onClick = {
                if (ph.isBlank()) return@Button
                try {
                    val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/suppliers", DB_USER, DB_PASS)
                    val p = c.prepareStatement("INSERT INTO supplier (name, phone) VALUES (?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS)
                    p.setString(1, nm); p.setString(2, ph); p.executeUpdate()
                    val r = p.generatedKeys; r.next(); foundId = r.getInt(1); c.close(); step = 2
                } catch (e: Exception) { msg = e.message ?: "Error" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Green)) { Text("Create & Continue", color = Color.White) }
            2 -> Button(enabled = lineItems.isNotEmpty(), onClick = {
                try {
                    val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/suppliers", DB_USER, DB_PASS)
                    c.autoCommit = false
                    try {
                        val pr = c.prepareStatement("INSERT INTO supplier_receipts (supplier_id, receipt_date, paid) VALUES (?, NOW(), FALSE)", java.sql.Statement.RETURN_GENERATED_KEYS)
                        pr.setInt(1, foundId!!); pr.executeUpdate()
                        val rr = pr.generatedKeys; rr.next(); val receiptId = rr.getInt(1)
                        val pi = c.prepareStatement("INSERT INTO supplier_receipt_items (receipt_id, stock_id, quantity, cost_price) VALUES (?, ?, ?, ?)")
                        val ps = c.prepareStatement("UPDATE stock.products SET quantity = quantity + ? WHERE id = ?")
                        lineItems.forEach { li ->
                            pi.setInt(1, receiptId); pi.setInt(2, li.stockId); pi.setInt(3, li.qty); pi.setDouble(4, li.price); pi.executeUpdate()
                            ps.setInt(1, li.qty); ps.setInt(2, li.stockId); ps.executeUpdate()
                        }
                        c.commit(); refresh()
                    } catch (ex: Exception) { c.rollback(); msg = ex.message ?: "Transaction failed"; return@Button
                    } finally { c.autoCommit = true; c.close() }
                    onClose()
                } catch (e: Exception) { msg = e.message ?: "Error" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Green)) { Text("Save Receipt (₹${lineItems.sumOf { it.qty * it.price }})", color = Color.White) }
        }
    }, dismissButton = { TextButton(onClick = { onClose(); resetNewForm() }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
}

@Composable
fun productScreen(onBack: () -> Unit) {
    var refresh by remember { mutableStateOf(0) }; var search by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<ProductRow?>(null) }; var showAdd by remember { mutableStateOf(false) }
    var deletingProduct by remember { mutableStateOf<ProductRow?>(null) }; var errorMsg by remember { mutableStateOf("") }

    val products = remember(refresh) {
        val list = mutableListOf<ProductRow>()
        try {
            withDb("stock") { c ->
                val r = c.createStatement().executeQuery("SELECT * FROM products ORDER BY product_name")
                while (r.next()) list.add(ProductRow(r.getInt("id"), r.getString("product_name"), r.getString("type") ?: "", r.getInt("quantity"), r.getString("supplied_by") ?: "", r.getString("warranty") ?: "", r.getInt("rack"), r.getDouble("cost_price"), r.getDouble("selling_price")))
            }
            errorMsg = ""
        } catch (e: Exception) { errorMsg = e.message ?: "Error" }
        list
    }
    val filtered = if (search.isBlank()) products else products.filter { it.productName.contains(search, ignoreCase = true) || it.type.contains(search, ignoreCase = true) }
    val grouped = filtered.groupBy { it.type }

    Box(Modifier.fillMaxSize().background(AppColors.Bg)) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader("PRODUCTS", onBack) { showAdd = true }
            Spacer(Modifier.height(8.dp)); SearchBar(search, { search = it }, "Search products...")
            if (errorMsg.isNotBlank()) ErrorCard(errorMsg)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) { HeaderCell("Product", 2f); HeaderCell("Qty", 1f); HeaderCell("Cost", 1f); HeaderCell("Sell", 1f); HeaderCell("Rack", 1f); HeaderCell("Actions", 1.5f) }
            Divider(color = Color.Gray, thickness = 1.dp)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 6.dp)) {
                grouped.forEach { (type, items) ->
                    Text(type, color = AppColors.Gold, fontSize = 18.sp, fontFamily = girassol, modifier = Modifier.padding(vertical = 6.dp))
                    items.forEach { p ->
                        DataCard {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(p.productName, Modifier.weight(2f), color = Color.White, fontSize = 14.sp)
                                Text("${p.quantity}", Modifier.weight(1f), color = Color.White, fontSize = 14.sp)
                                Text("₹${p.costPrice}", Modifier.weight(1f), color = Color.LightGray, fontSize = 13.sp)
                                Text("₹${p.sellingPrice}", Modifier.weight(1f), color = Color(0xFF7EC87E), fontSize = 13.sp)
                                Text("R${p.rack}", Modifier.weight(1f), color = Color.LightGray, fontSize = 13.sp)
                                Row(Modifier.weight(1.5f), horizontalArrangement = Arrangement.End) {
                                    Btn("Edit", AppColors.Navy) { editingProduct = p }
                                    Spacer(Modifier.width(4.dp))
                                    Btn("Del", AppColors.Red) { deletingProduct = p }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editingProduct != null) {
        val isEdit = editingProduct != null; val p = editingProduct
        var productName by remember { mutableStateOf(p?.productName ?: "") }; var type by remember { mutableStateOf(p?.type ?: "") }; var qty by remember { mutableStateOf(p?.quantity?.toString() ?: "0") }
        var suppliedBy by remember { mutableStateOf(p?.suppliedBy ?: "") }; var warranty by remember { mutableStateOf(p?.warranty ?: "") }; var rack by remember { mutableStateOf(p?.rack?.toString() ?: "1") }
        var costPrice by remember { mutableStateOf(p?.costPrice?.toString() ?: "0") }; var sellingPrice by remember { mutableStateOf(p?.sellingPrice?.toString() ?: "0") }
        AlertDialog(onDismissRequest = { showAdd = false; editingProduct = null }, title = { Text(if (isEdit) "Edit Product" else "Add Product", fontFamily = girassol) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()).width(350.dp)) {
                OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row { OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Qty") }, singleLine = true, modifier = Modifier.weight(1f)) }
                Spacer(Modifier.height(6.dp)); OutlinedTextField(value = suppliedBy, onValueChange = { suppliedBy = it }, label = { Text("Supplied By") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row { OutlinedTextField(value = warranty, onValueChange = { warranty = it }, label = { Text("Warranty") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = rack, onValueChange = { rack = it }, label = { Text("Rack (1-30)") }, singleLine = true, modifier = Modifier.weight(1f)) }
                Spacer(Modifier.height(6.dp))
                Row { OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Cost Price") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = sellingPrice, onValueChange = { sellingPrice = it }, label = { Text("Sell Price") }, singleLine = true, modifier = Modifier.weight(1f)) }
            }
        }, confirmButton = {
            Button(onClick = {
                try {
                    val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/stock", DB_USER, DB_PASS)
                    if (isEdit) { val p2 = c.prepareStatement("UPDATE products SET product_name=?, type=?, quantity=?, supplied_by=?, warranty=?, rack=?, cost_price=?, selling_price=? WHERE id=?")
                        p2.setString(1, productName); p2.setString(2, type); p2.setInt(3, qty.toIntOrNull() ?: 0); p2.setString(4, suppliedBy); p2.setString(5, warranty); p2.setInt(6, rack.toIntOrNull() ?: 1); p2.setDouble(7, costPrice.toDoubleOrNull() ?: 0.0); p2.setDouble(8, sellingPrice.toDoubleOrNull() ?: 0.0); p2.setInt(9, p!!.id); p2.executeUpdate()
                    } else {
                        val p2 = c.prepareStatement("INSERT INTO products (product_name,type,quantity,supplied_by,warranty,rack,cost_price,selling_price) VALUES (?,?,?,?,?,?,?,?)", java.sql.Statement.RETURN_GENERATED_KEYS)
                        p2.setString(1, productName); p2.setString(2, type); p2.setInt(3, qty.toIntOrNull() ?: 0); p2.setString(4, suppliedBy); p2.setString(5, warranty); p2.setInt(6, rack.toIntOrNull() ?: 1); p2.setDouble(7, costPrice.toDoubleOrNull() ?: 0.0); p2.setDouble(8, sellingPrice.toDoubleOrNull() ?: 0.0); p2.executeUpdate()
                        val gk = p2.generatedKeys; gk.next(); val newId = gk.getInt(1)
                        try {
                            val sc = DriverManager.getConnection("jdbc:mysql://localhost:3306/suppliers", DB_USER, DB_PASS)
                            val sr = sc.prepareStatement("SELECT id FROM supplier WHERE name = ?").apply { setString(1, suppliedBy.ifBlank { "Stock Correction" }) }.executeQuery()
                            val sid: Int
                            if (sr.next()) { sid = sr.getInt("id") } else {
                                val sp = sc.prepareStatement("INSERT INTO supplier (name, phone) VALUES (?, '0000000000')", java.sql.Statement.RETURN_GENERATED_KEYS)
                                sp.setString(1, suppliedBy.ifBlank { "Stock Correction" }); sp.executeUpdate()
                                val sk = sp.generatedKeys; sk.next(); sid = sk.getInt(1)
                            }
                            val rp = sc.prepareStatement("INSERT INTO supplier_receipts (supplier_id, receipt_date, paid) VALUES (?, NOW(), TRUE)", java.sql.Statement.RETURN_GENERATED_KEYS)
                            rp.setInt(1, sid); rp.executeUpdate()
                            val rk = rp.generatedKeys; rk.next(); val rid = rk.getInt(1)
                            val rip = sc.prepareStatement("INSERT INTO supplier_receipt_items (receipt_id, stock_id, quantity, cost_price) VALUES (?, ?, ?, ?)")
                            rip.setInt(1, rid); rip.setInt(2, newId); rip.setInt(3, qty.toIntOrNull() ?: 0); rip.setDouble(4, costPrice.toDoubleOrNull() ?: 0.0); rip.executeUpdate()
                            sc.close()
                        } catch (e: Exception) { errorMsg = "Receipt creation failed: ${e.message}" }
                    }
                    c.close(); refresh++; showAdd = false; editingProduct = null
                } catch (e: Exception) { errorMsg = e.message ?: "Save failed" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text(if (isEdit) "Update" else "Save", color = Color.White) }
        }, dismissButton = { TextButton(onClick = { showAdd = false; editingProduct = null }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
    }

    deletingProduct?.let { prod ->
        AlertDialog(onDismissRequest = { deletingProduct = null }, title = { Text("Delete Product", fontFamily = girassol) }, text = { Text("Delete ${prod.productName}?") },
            confirmButton = { Button(onClick = {
                try { withDb("stock") { c -> c.prepareStatement("DELETE FROM products WHERE id=?").apply { setInt(1, prod.id) }.executeUpdate() }; refresh++; deletingProduct = null } catch (e: Exception) { errorMsg = e.message ?: "Delete failed" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Red)) { Text("Delete", color = Color.White) } },
            dismissButton = { TextButton(onClick = { deletingProduct = null }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
    }
}

@Composable
fun insuranceScreen(onBack: () -> Unit) {
    var refresh by remember { mutableStateOf(0) }; var search by remember { mutableStateOf("") }
    var editingPolicy by remember { mutableStateOf<InsuranceRow?>(null) }; var showAdd by remember { mutableStateOf(false) }
    var deletingPolicy by remember { mutableStateOf<InsuranceRow?>(null) }; var errorMsg by remember { mutableStateOf("") }

    val policies = remember(refresh) {
        val list = mutableListOf<InsuranceRow>()
        try {
            withDb("insurance") { c ->
                val r = c.createStatement().executeQuery("SELECT * FROM insurance ORDER BY expiry_date")
                while (r.next()) list.add(InsuranceRow(r.getInt("id"), r.getString("customer_name"), r.getString("vehicle_number"), r.getString("driving_license") ?: "", r.getString("pan_number") ?: "", r.getString("rc_number") ?: "", r.getString("insurance_company"), r.getString("policy_name"), r.getDouble("coverage"), r.getString("expiry_date")))
            }
            errorMsg = ""
        } catch (e: Exception) { errorMsg = e.message ?: "Error" }
        list
    }
    val filtered = if (search.isBlank()) policies else policies.filter { it.customerName.contains(search, ignoreCase = true) || it.vehicleNumber.contains(search) || it.policyName.contains(search, ignoreCase = true) }

    Box(Modifier.fillMaxSize().background(AppColors.Bg)) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader("INSURANCE", onBack) { showAdd = true }
            Spacer(Modifier.height(8.dp)); SearchBar(search, { search = it }, "Search by name, vehicle, or policy...")
            if (errorMsg.isNotBlank()) ErrorCard(errorMsg)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) { HeaderCell("Customer", 2f); HeaderCell("Vehicle", 1.5f); HeaderCell("Policy", 1.5f); HeaderCell("Coverage", 1f); HeaderCell("Expires", 1f); HeaderCell("Actions", 1.5f) }
            Divider(color = Color.Gray, thickness = 1.dp)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 6.dp)) {
                filtered.forEach { pol ->
                    DataCard {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(pol.customerName, Modifier.weight(2f), color = Color.White, fontSize = 14.sp)
                            Text(pol.vehicleNumber, Modifier.weight(1.5f), color = Color.LightGray, fontSize = 13.sp)
                            Text(pol.policyName, Modifier.weight(1.5f), color = Color.White, fontSize = 13.sp)
                            Text("₹${pol.coverage}", Modifier.weight(1f), color = Color(0xFF7EC87E), fontSize = 13.sp)
                            Text(pol.expiryDate.take(10), Modifier.weight(1f), color = Color(0xFFFF9966), fontSize = 13.sp)
                            Row(Modifier.weight(1.5f), horizontalArrangement = Arrangement.End) {
                                Btn("Edit", AppColors.Navy) { editingPolicy = pol }
                                Spacer(Modifier.width(4.dp))
                                Btn("Del", AppColors.Red) { deletingPolicy = pol }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editingPolicy != null) {
        val isEdit = editingPolicy != null; val p = editingPolicy
        var customerName by remember { mutableStateOf(p?.customerName ?: "") }; var vehicleNumber by remember { mutableStateOf(p?.vehicleNumber ?: "") }; var drivingLicense by remember { mutableStateOf(p?.drivingLicense ?: "") }
        var panNumber by remember { mutableStateOf(p?.panNumber ?: "") }; var rcNumber by remember { mutableStateOf(p?.rcNumber ?: "") }; var insuranceCompany by remember { mutableStateOf(p?.insuranceCompany ?: "") }
        var policyName by remember { mutableStateOf(p?.policyName ?: "") }; var coverage by remember { mutableStateOf(p?.coverage?.toString() ?: "0") }; var expiryDate by remember { mutableStateOf(p?.expiryDate?.take(10) ?: "") }
        AlertDialog(onDismissRequest = { showAdd = false; editingPolicy = null }, title = { Text(if (isEdit) "Edit Policy" else "Add Policy", fontFamily = girassol) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()).width(400.dp)) {
                Row { OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = vehicleNumber, onValueChange = { vehicleNumber = it }, label = { Text("Vehicle No") }, singleLine = true, modifier = Modifier.weight(1f)) }
                Spacer(Modifier.height(6.dp))
                Row { OutlinedTextField(value = drivingLicense, onValueChange = { drivingLicense = it }, label = { Text("DL No") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = panNumber, onValueChange = { panNumber = it }, label = { Text("PAN") }, singleLine = true, modifier = Modifier.weight(1f)) }
                Spacer(Modifier.height(6.dp)); OutlinedTextField(value = rcNumber, onValueChange = { rcNumber = it }, label = { Text("RC No") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row { OutlinedTextField(value = insuranceCompany, onValueChange = { insuranceCompany = it }, label = { Text("Insurance Co") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = policyName, onValueChange = { policyName = it }, label = { Text("Policy Name") }, singleLine = true, modifier = Modifier.weight(1f)) }
                Spacer(Modifier.height(6.dp))
                Row { OutlinedTextField(value = coverage, onValueChange = { coverage = it }, label = { Text("Coverage ₹") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(6.dp)); OutlinedTextField(value = expiryDate, onValueChange = { expiryDate = it }, label = { Text("Expiry (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f)) }
            }
        }, confirmButton = {
            Button(onClick = {
                try {
                    val c = DriverManager.getConnection("jdbc:mysql://localhost:3306/insurance", DB_USER, DB_PASS)
                    if (isEdit) { val p2 = c.prepareStatement("UPDATE insurance SET customer_name=?, vehicle_number=?, driving_license=?, pan_number=?, rc_number=?, insurance_company=?, policy_name=?, coverage=?, expiry_date=? WHERE id=?")
                        p2.setString(1, customerName); p2.setString(2, vehicleNumber); p2.setString(3, drivingLicense); p2.setString(4, panNumber); p2.setString(5, rcNumber); p2.setString(6, insuranceCompany); p2.setString(7, policyName); p2.setDouble(8, coverage.toDoubleOrNull() ?: 0.0); p2.setString(9, expiryDate); p2.setInt(10, p!!.id); p2.executeUpdate()
                    } else { val p2 = c.prepareStatement("INSERT INTO insurance (customer_name,vehicle_number,driving_license,pan_number,rc_number,insurance_company,policy_name,coverage,expiry_date) VALUES (?,?,?,?,?,?,?,?,?)")
                        p2.setString(1, customerName); p2.setString(2, vehicleNumber); p2.setString(3, drivingLicense); p2.setString(4, panNumber); p2.setString(5, rcNumber); p2.setString(6, insuranceCompany); p2.setString(7, policyName); p2.setDouble(8, coverage.toDoubleOrNull() ?: 0.0); p2.setString(9, expiryDate); p2.executeUpdate() }
                    c.close(); refresh++; showAdd = false; editingPolicy = null
                } catch (e: Exception) { errorMsg = e.message ?: "Save failed" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark)) { Text(if (isEdit) "Update" else "Save", color = Color.White) }
        }, dismissButton = { TextButton(onClick = { showAdd = false; editingPolicy = null }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
    }

    deletingPolicy?.let { pol ->
        AlertDialog(onDismissRequest = { deletingPolicy = null }, title = { Text("Delete Policy", fontFamily = girassol) }, text = { Text("Delete policy for ${pol.customerName}?") },
            confirmButton = { Button(onClick = {
                try { withDb("insurance") { c -> c.prepareStatement("DELETE FROM insurance WHERE id=?").apply { setInt(1, pol.id) }.executeUpdate() }; refresh++; deletingPolicy = null } catch (e: Exception) { errorMsg = e.message ?: "Delete failed" }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Red)) { Text("Delete", color = Color.White) } },
            dismissButton = { TextButton(onClick = { deletingPolicy = null }) { Text("Cancel") } }, shape = RoundedCornerShape(12.dp))
    }
}

@Composable private fun ScreenHeader(title: String, onBack: () -> Unit, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Dark), modifier = Modifier.size(44.dp)) { Text("←", color = Color.White, fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp)); Text(title, fontFamily = girassol, color = AppColors.Gold, fontSize = 28.sp, modifier = Modifier.weight(1f))
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Green), shape = RoundedCornerShape(8.dp)) { Text("+ Add", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun SearchBar(q: String, o: (String) -> Unit, h: String) {
    OutlinedTextField(value = q, onValueChange = o, placeholder = { Text(h, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = AppColors.Gold, unfocusedBorderColor = Color.Gray))
}
@Composable private fun ErrorCard(m: String) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), backgroundColor = Color(0xFF5C2E2E), shape = RoundedCornerShape(8.dp)) { Text(m, color = Color(0xFFFF6B6B), modifier = Modifier.padding(12.dp)) }
}
@Composable private fun DataCard(c: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), backgroundColor = AppColors.Card, shape = RoundedCornerShape(8.dp), elevation = 3.dp, content = c)
}
@Composable private fun Btn(t: String, c: Color, a: () -> Unit) {
    Button(onClick = a, colors = ButtonDefaults.buttonColors(backgroundColor = c), shape = RoundedCornerShape(6.dp), modifier = Modifier.height(30.dp)) { Text(t, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}
