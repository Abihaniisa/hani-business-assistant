package com.abihaniisa.hani.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ProductRecord(
    val id: Long, val name: String, val purchasePriceKobo: Long,
    val purchaseQuantity: String, val baseUnit: String
)
data class PackageRecord(val name: String, val quantity: String)
data class CustomerRecord(val id: Long, val name: String, val phone: String, val balance: Long)
data class LedgerRecord(
    val id: Long, val type: String, val description: String,
    val amount: Long, val note: String, val date: String
)
data class NoteRecord(
    val id: Long, val customerId: Long, val customerName: String,
    val title: String, val body: String, val date: String
)

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "hani_business.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE products(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            purchase_price_kobo INTEGER NOT NULL,
            base_unit TEXT NOT NULL,
            purchase_quantity TEXT NOT NULL,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )""")
        db.execSQL("""CREATE TABLE product_packaging(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER NOT NULL,
            package_name TEXT NOT NULL,
            base_unit_quantity TEXT NOT NULL,
            FOREIGN KEY(product_id) REFERENCES products(id)
        )""")
        db.execSQL("""CREATE TABLE customers(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            phone TEXT DEFAULT '',
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )""")
        db.execSQL("""CREATE TABLE account_cycles(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id INTEGER NOT NULL,
            status TEXT NOT NULL DEFAULT 'OPEN',
            opened_at TEXT DEFAULT CURRENT_TIMESTAMP,
            settled_at TEXT,
            FOREIGN KEY(customer_id) REFERENCES customers(id)
        )""")
        db.execSQL("""CREATE TABLE ledger_entries(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id INTEGER NOT NULL,
            cycle_id INTEGER NOT NULL,
            entry_type TEXT NOT NULL,
            description TEXT NOT NULL,
            amount_kobo INTEGER NOT NULL,
            note TEXT DEFAULT '',
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY(customer_id) REFERENCES customers(id),
            FOREIGN KEY(cycle_id) REFERENCES account_cycles(id)
        )""")
        db.execSQL("""CREATE TABLE customer_notes(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id INTEGER NOT NULL,
            title TEXT NOT NULL,
            body TEXT NOT NULL,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY(customer_id) REFERENCES customers(id)
        )""")
        db.execSQL("CREATE INDEX idx_ledger_customer ON ledger_entries(customer_id, created_at)")
        db.execSQL("CREATE INDEX idx_notes_customer ON customer_notes(customer_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema changes must use additive migrations. Never erase customer records.
    }

    private fun values(vararg pairs: Pair<String, Any?>) = ContentValues().apply {
        pairs.forEach { (key, value) ->
            when (value) {
                null -> putNull(key)
                is String -> put(key, value)
                is Long -> put(key, value)
                is Int -> put(key, value)
                else -> put(key, value.toString())
            }
        }
    }

    fun addProduct(
        name: String, priceKobo: Long, baseUnit: String,
        purchaseQuantity: String, packages: List<PackageRecord>
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val id = db.insertOrThrow("products", null, values(
                "name" to name, "purchase_price_kobo" to priceKobo,
                "base_unit" to baseUnit, "purchase_quantity" to purchaseQuantity
            ))
            packages.forEach {
                db.insertOrThrow("product_packaging", null, values(
                    "product_id" to id, "package_name" to it.name,
                    "base_unit_quantity" to it.quantity
                ))
            }
            db.setTransactionSuccessful()
            return id
        } finally { db.endTransaction() }
    }

    fun products(): List<ProductRecord> {
        val result = mutableListOf<ProductRecord>()
        readableDatabase.rawQuery("SELECT * FROM products ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) result.add(ProductRecord(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getLong(c.getColumnIndexOrThrow("purchase_price_kobo")),
                c.getString(c.getColumnIndexOrThrow("purchase_quantity")),
                c.getString(c.getColumnIndexOrThrow("base_unit"))
            ))
        }
        return result
    }

    fun packages(productId: Long): List<PackageRecord> {
        val result = mutableListOf<PackageRecord>()
        readableDatabase.rawQuery(
            "SELECT package_name,base_unit_quantity FROM product_packaging WHERE product_id=? ORDER BY id",
            arrayOf(productId.toString())
        ).use { c ->
            while (c.moveToNext()) result.add(PackageRecord(c.getString(0), c.getString(1)))
        }
        return result
    }

    fun addCustomer(name: String, phone: String): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val id = db.insertOrThrow("customers", null, values("name" to name, "phone" to phone))
            db.insertOrThrow("account_cycles", null, values("customer_id" to id, "status" to "OPEN"))
            db.setTransactionSuccessful()
            return id
        } finally { db.endTransaction() }
    }

    private fun balance(customerId: Long, cycleId: Long): Long {
        readableDatabase.rawQuery(
            """SELECT COALESCE(SUM(CASE
                WHEN entry_type IN ('SALE','ADVANCE') THEN amount_kobo
                WHEN entry_type IN ('PAYMENT','RETURN') THEN -amount_kobo
                ELSE 0 END),0)
                FROM ledger_entries WHERE customer_id=? AND cycle_id=?""",
            arrayOf(customerId.toString(), cycleId.toString())
        ).use { c -> if (c.moveToFirst()) return c.getLong(0) }
        return 0
    }

    fun customers(): List<CustomerRecord> {
        val result = mutableListOf<CustomerRecord>()
        readableDatabase.rawQuery("""
            SELECT c.id,c.name,c.phone,
            COALESCE((SELECT SUM(CASE
              WHEN e.entry_type IN ('SALE','ADVANCE') THEN e.amount_kobo
              WHEN e.entry_type IN ('PAYMENT','RETURN') THEN -e.amount_kobo
              ELSE 0 END)
              FROM ledger_entries e JOIN account_cycles a ON a.id=e.cycle_id
              WHERE e.customer_id=c.id AND a.status='OPEN'),0) AS balance
            FROM customers c ORDER BY c.name COLLATE NOCASE
        """, null).use { c ->
            while (c.moveToNext()) result.add(CustomerRecord(
                c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)
            ))
        }
        return result
    }

    fun addEntry(customerId: Long, type: String, description: String, amountKobo: Long, note: String) {
        require(type in listOf("SALE", "PAYMENT", "ADVANCE", "RETURN"))
        require(amountKobo > 0)
        val db = writableDatabase
        db.beginTransaction()
        try {
            var cycleId = -1L
            db.rawQuery(
                "SELECT id FROM account_cycles WHERE customer_id=? AND status='OPEN' ORDER BY id DESC LIMIT 1",
                arrayOf(customerId.toString())
            ).use { if (it.moveToFirst()) cycleId = it.getLong(0) }
            if (cycleId == -1L) {
                cycleId = db.insertOrThrow("account_cycles", null, values(
                    "customer_id" to customerId, "status" to "OPEN"
                ))
            }
            db.insertOrThrow("ledger_entries", null, values(
                "customer_id" to customerId, "cycle_id" to cycleId,
                "entry_type" to type, "description" to description,
                "amount_kobo" to amountKobo, "note" to note
            ))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun ledger(customerId: Long): List<LedgerRecord> {
        val result = mutableListOf<LedgerRecord>()
        readableDatabase.rawQuery("""
            SELECT e.id,e.entry_type,e.description,e.amount_kobo,e.note,e.created_at
            FROM ledger_entries e JOIN account_cycles a ON a.id=e.cycle_id
            WHERE e.customer_id=? AND a.status='OPEN'
            ORDER BY e.created_at DESC,e.id DESC
        """, arrayOf(customerId.toString())).use { c ->
            while (c.moveToNext()) result.add(LedgerRecord(
                c.getLong(0), c.getString(1), c.getString(2),
                c.getLong(3), c.getString(4), c.getString(5)
            ))
        }
        return result
    }

    fun settle(customerId: Long): Boolean {
        val db = writableDatabase
        var cycleId = -1L
        db.rawQuery(
            "SELECT id FROM account_cycles WHERE customer_id=? AND status='OPEN' ORDER BY id DESC LIMIT 1",
            arrayOf(customerId.toString())
        ).use { if (it.moveToFirst()) cycleId = it.getLong(0) }
        if (cycleId == -1L || balance(customerId, cycleId) != 0L) return false
        db.update("account_cycles", values("status" to "SETTLED", "settled_at" to "CURRENT_TIMESTAMP"),
            "id=?", arrayOf(cycleId.toString()))
        return true
    }

    fun addNote(customerId: Long, title: String, body: String) {
        writableDatabase.insertOrThrow("customer_notes", null, values(
            "customer_id" to customerId, "title" to title, "body" to body
        ))
    }

    fun searchNotes(query: String = ""): List<NoteRecord> {
        val result = mutableListOf<NoteRecord>()
        readableDatabase.rawQuery("""
            SELECT n.id,n.customer_id,c.name,n.title,n.body,n.created_at
            FROM customer_notes n JOIN customers c ON c.id=n.customer_id
            WHERE (?='' OR n.title LIKE ? OR n.body LIKE ? OR c.name LIKE ?)
            ORDER BY n.updated_at DESC,n.id DESC
        """, arrayOf(query, "%$query%", "%$query%", "%$query%")).use { c ->
            while (c.moveToNext()) result.add(NoteRecord(
                c.getLong(0), c.getLong(1), c.getString(2),
                c.getString(3), c.getString(4), c.getString(5)
            ))
        }
        return result
    }
}
