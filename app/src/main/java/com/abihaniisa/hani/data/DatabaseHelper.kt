package com.abihaniisa.hani.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ProductRecord(val id:Long,val name:String,val purchasePriceKobo:Long,val purchaseQuantity:String,val baseUnit:String)
data class PackageRecord(val name:String,val quantity:String)
data class CustomerRecord(val id:Long,val name:String,val phone:String,val balance:Long)
data class LedgerRecord(val id:Long,val type:String,val description:String,val amount:Long,val note:String,val date:String)
data class NoteRecord(val id:Long,val customerId:Long,val customerName:String,val title:String,val body:String,val date:String)

class DatabaseHelper(context:Context):SQLiteOpenHelper(context,"hani_business.db",null,1) {
    override fun onCreate(db:SQLiteDatabase) {
        db.execSQL("""CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,purchase_price_kobo INTEGER NOT NULL,base_unit TEXT NOT NULL,purchase_quantity TEXT NOT NULL,created_at TEXT DEFAULT CURRENT_TIMESTAMP)""")
        db.execSQL("""CREATE TABLE product_packaging(id INTEGER PRIMARY KEY AUTOINCREMENT,product_id INTEGER NOT NULL,package_name TEXT NOT NULL,base_unit_quantity TEXT NOT NULL,FOREIGN KEY(product_id) REFERENCES products(id))""")
        db.execSQL("""CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT DEFAULT '',created_at TEXT DEFAULT CURRENT_TIMESTAMP)""")
        db.execSQL("""CREATE TABLE account_cycles(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'OPEN',opened_at TEXT DEFAULT CURRENT_TIMESTAMP,settled_at TEXT,FOREIGN KEY(customer_id) REFERENCES customers(id))""")
        db.execSQL("""CREATE TABLE ledger_entries(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,cycle_id INTEGER NOT NULL,entry_type TEXT NOT NULL,description TEXT NOT NULL,amount_kobo INTEGER NOT NULL,note TEXT DEFAULT '',created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(customer_id) REFERENCES customers(id),FOREIGN KEY(cycle_id) REFERENCES account_cycles(id))""")
        db.execSQL("""CREATE TABLE customer_notes(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,title TEXT NOT NULL,body TEXT NOT NULL,created_at TEXT DEFAULT CURRENT_TIMESTAMP,updated_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(customer_id) REFERENCES customers(id))""")
        db.execSQL("CREATE INDEX idx_ledger_customer ON ledger_entries(customer_id,created_at)")
        db.execSQL("CREATE INDEX idx_notes_customer ON customer_notes(customer_id)")
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) {}
    private fun cv(vararg p:Pair<String,Any?>)=ContentValues().apply{p.forEach{(k,v)->when(v){null->putNull(k);is String->put(k,v);is Long->put(k,v);is Int->put(k,v);else->put(k,v.toString())}}}
    fun addProduct(name:String,price:Long,unit:String,qty:String,packs:List<PackageRecord>):Long {
        val d=writableDatabase;d.beginTransaction()
        try {val id=d.insertOrThrow("products",null,cv("name" to name,"purchase_price_kobo" to price,"base_unit" to unit,"purchase_quantity" to qty))
            packs.forEach{d.insertOrThrow("product_packaging",null,cv("product_id" to id,"package_name" to it.name,"base_unit_quantity" to it.quantity))}
            d.setTransactionSuccessful();return id
        } finally {d.endTransaction()}
    }
    fun updateProduct(id:Long,name:String,price:Long,unit:String,qty:String,packs:List<PackageRecord>) {
        val d=writableDatabase;d.beginTransaction()
        try {d.update("products",cv("name" to name,"purchase_price_kobo" to price,"base_unit" to unit,"purchase_quantity" to qty),"id=?",arrayOf(id.toString()))
            d.delete("product_packaging","product_id=?",arrayOf(id.toString()))
            packs.forEach{d.insertOrThrow("product_packaging",null,cv("product_id" to id,"package_name" to it.name,"base_unit_quantity" to it.quantity))}
            d.setTransactionSuccessful()
        } finally {d.endTransaction()}
    }
    fun deleteProduct(id:Long){val d=writableDatabase;d.beginTransaction();try{d.delete("product_packaging","product_id=?",arrayOf("$id"));d.delete("products","id=?",arrayOf("$id"));d.setTransactionSuccessful()}finally{d.endTransaction()}}
    fun products():List<ProductRecord>{val a=mutableListOf<ProductRecord>();readableDatabase.rawQuery("SELECT * FROM products ORDER BY name COLLATE NOCASE",null).use{c->while(c.moveToNext())a.add(ProductRecord(c.getLong(0),c.getString(1),c.getLong(2),c.getString(4),c.getString(3)))};return a}
    fun packages(id:Long):List<PackageRecord>{val a=mutableListOf<PackageRecord>();readableDatabase.rawQuery("SELECT package_name,base_unit_quantity FROM product_packaging WHERE product_id=? ORDER BY id",arrayOf("$id")).use{c->while(c.moveToNext())a.add(PackageRecord(c.getString(0),c.getString(1)))};return a}
    fun addCustomer(name:String,phone:String):Long{val d=writableDatabase;d.beginTransaction();try{val id=d.insertOrThrow("customers",null,cv("name" to name,"phone" to phone));d.insertOrThrow("account_cycles",null,cv("customer_id" to id,"status" to "OPEN"));d.setTransactionSuccessful();return id}finally{d.endTransaction()}}
    fun deleteCustomer(id:Long){val d=writableDatabase;d.beginTransaction();try{d.delete("ledger_entries","customer_id=?",arrayOf("$id"));d.delete("account_cycles","customer_id=?",arrayOf("$id"));d.delete("customer_notes","customer_id=?",arrayOf("$id"));d.delete("customers","id=?",arrayOf("$id"));d.setTransactionSuccessful()}finally{d.endTransaction()}}
    fun customers():List<CustomerRecord>{val a=mutableListOf<CustomerRecord>();readableDatabase.rawQuery("""SELECT c.id,c.name,c.phone,COALESCE((SELECT SUM(CASE WHEN e.entry_type IN('SALE','ADVANCE') THEN e.amount_kobo WHEN e.entry_type IN('PAYMENT','RETURN') THEN -e.amount_kobo ELSE 0 END) FROM ledger_entries e JOIN account_cycles x ON x.id=e.cycle_id WHERE e.customer_id=c.id AND x.status='OPEN'),0) FROM customers c ORDER BY c.name COLLATE NOCASE""",null).use{c->while(c.moveToNext())a.add(CustomerRecord(c.getLong(0),c.getString(1),c.getString(2),c.getLong(3)))};return a}
    fun addEntry(customerId:Long,type:String,description:String,amount:Long,note:String){require(type in listOf("SALE","PAYMENT","ADVANCE","RETURN")&&amount>0);val d=writableDatabase;d.beginTransaction();try{var cycle=-1L;d.rawQuery("SELECT id FROM account_cycles WHERE customer_id=? AND status='OPEN' ORDER BY id DESC LIMIT 1",arrayOf("$customerId")).use{if(it.moveToFirst())cycle=it.getLong(0)};if(cycle<0)cycle=d.insertOrThrow("account_cycles",null,cv("customer_id" to customerId,"status" to "OPEN"));d.insertOrThrow("ledger_entries",null,cv("customer_id" to customerId,"cycle_id" to cycle,"entry_type" to type,"description" to description,"amount_kobo" to amount,"note" to note));d.setTransactionSuccessful()}finally{d.endTransaction()}}
    fun deleteEntry(id:Long){writableDatabase.delete("ledger_entries","id=?",arrayOf("$id"))}
    fun ledger(customerId:Long):List<LedgerRecord>{val a=mutableListOf<LedgerRecord>();readableDatabase.rawQuery("SELECT id,entry_type,description,amount_kobo,note,created_at FROM ledger_entries WHERE customer_id=? ORDER BY created_at DESC,id DESC",arrayOf("$customerId")).use{c->while(c.moveToNext())a.add(LedgerRecord(c.getLong(0),c.getString(1),c.getString(2),c.getLong(3),c.getString(4),c.getString(5)))};return a}
    fun addNote(id:Long,title:String,body:String){writableDatabase.insertOrThrow("customer_notes",null,cv("customer_id" to id,"title" to title,"body" to body))}
    fun updateNote(id:Long,title:String,body:String){writableDatabase.update("customer_notes",cv("title" to title,"body" to body,"updated_at" to "datetime('now')"),"id=?",arrayOf("$id"))}
    fun deleteNote(id:Long){writableDatabase.delete("customer_notes","id=?",arrayOf("$id"))}
    fun searchNotes(q:String=""):List<NoteRecord>{val a=mutableListOf<NoteRecord>();readableDatabase.rawQuery("""SELECT n.id,n.customer_id,c.name,n.title,n.body,n.created_at FROM customer_notes n JOIN customers c ON c.id=n.customer_id WHERE (?='' OR n.title LIKE ? OR n.body LIKE ? OR c.name LIKE ?) ORDER BY n.updated_at DESC,n.id DESC""",arrayOf(q,"%$q%","%$q%","%$q%")).use{c->while(c.moveToNext())a.add(NoteRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5)))};return a}
}
