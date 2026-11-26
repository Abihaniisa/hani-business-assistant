package com.abihaniisa.hani

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import com.abihaniisa.hani.data.*
import java.math.BigDecimal

class MainActivity : Activity() {
    private lateinit var db: DatabaseHelper
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var tab = 0
    private var selectedCustomer = -1L
    private val green = Color.rgb(31,91,78)
    private val pale = Color.rgb(229,240,235)
    private val ink = Color.rgb(30,39,36)
    private val muted = Color.rgb(111,122,117)
    private val bg = Color.rgb(247,248,246)
    private val white = Color.WHITE
    private val tabs = listOf("Home","Products","Customers","Memory")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DatabaseHelper(this)
        window.statusBarColor = white
        window.navigationBarColor = white
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        render()
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float = 14f, color: Int = ink, bold: Boolean = false) =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    private fun rounded(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
    }
    private fun card(): LinearLayout = column().apply {
        background = rounded(white, 18); setPadding(dp(16),dp(15),dp(16),dp(15))
    }
    private fun gap(parent: LinearLayout, h: Int = 12) {
        parent.addView(View(this), LinearLayout.LayoutParams(1,dp(h)))
    }
    private fun addLabel(parent: LinearLayout, value: String, size: Float = 14f, color: Int = ink, bold: Boolean = false) {
        parent.addView(text(value,size,color,bold))
    }
    private fun button(label: String, action: () -> Unit, primary: Boolean = true): TextView =
        text(label,14f,if(primary) white else green,true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16),dp(13),dp(16),dp(13))
            background = rounded(if(primary) green else pale,13)
            setOnClickListener { action() }
        }
    private fun field(hint: String, inputType: Int = 1): EditText = EditText(this).apply {
        this.hint = hint; textSize = 15f; setTextColor(ink); setHintTextColor(muted)
        this.inputType = inputType
        setSingleLine(true)
        background = rounded(bg,12)
        setPadding(dp(13),dp(7),dp(13),dp(7))
    }
    private fun addField(parent: LinearLayout, edit: EditText) {
        parent.addView(edit, LinearLayout.LayoutParams(-1,dp(52)))
        gap(parent,9)
    }

    private fun render() {
        root = column().apply { setBackgroundColor(bg) }
        val header = column().apply {
            setPadding(dp(20),dp(18),dp(20),dp(14)); setBackgroundColor(white)
        }
        val top = row()
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.hani)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(pale,12); clipToOutline = true
        }
        top.addView(logo,LinearLayout.LayoutParams(dp(42),dp(42)))
        val heading = column().apply { setPadding(dp(11),0,0,0) }
        heading.addView(text("HANI",12f,green,true))
        heading.addView(text(
            listOf("Business overview","Product management","Customer accounts","Customer memory")[tab],
            22f,ink,true
        ))
        top.addView(heading,LinearLayout.LayoutParams(0,-2,1f))
        header.addView(top)
        root.addView(header)
        content = column().apply { setPadding(dp(18),dp(18),dp(18),dp(18)) }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(content) }
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        when(tab) {
            0 -> home()
            1 -> productScreen()
            2 -> customerScreen()
            else -> memoryScreen()
        }
        val nav = row().apply {
            setPadding(dp(5),dp(8),dp(5),dp(8)); setBackgroundColor(white)
        }
        tabs.forEachIndexed { index, name ->
            val item = text(name,12f,if(tab==index) green else muted,true).apply {
                gravity=Gravity.CENTER; setPadding(0,dp(13),0,dp(13))
                if(tab==index) background=rounded(pale,13)
                setOnClickListener { tab=index; selectedCustomer=-1; render() }
            }
            nav.addView(item,LinearLayout.LayoutParams(0,-2,1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun home() {
        val products = db.products().size
        val customers = db.customers()
        val debt = customers.sumOf { it.balance }
        val welcome = card()
        addLabel(welcome,"Your shop, better organised.",21f,ink,true)
        gap(welcome,7)
        addLabel(welcome,"Keep track of stock costs, customer balances and important customer details in one place.",14f,muted)
        content.addView(welcome)
        gap(content)
        val stats = row()
        listOf("Products" to products.toString(),"Customers" to customers.size.toString(),"Outstanding" to ProductCalculator.naira(debt)).forEach { (title,value) ->
            val c=card()
            addLabel(c,title,12f,muted,true); gap(c,5); addLabel(c,value,if(title=="Outstanding")16f else 20f,ink,true)
            stats.addView(c,LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(6) })
        }
        content.addView(stats)
        gap(content)
        val actions=card()
        addLabel(actions,"Quick actions",16f,ink,true); gap(actions)
        actions.addView(button("Add a product",{tab=1;render()}))
        gap(actions,9)
        actions.addView(button("Add a customer",{tab=2;render();addCustomerDialog()},false))
        gap(actions,9)
        actions.addView(button("Search customer memory",{tab=3;render()},false))
        content.addView(actions)
        gap(content)
        val about=card()
        addLabel(about,"About Hani",16f,ink,true); gap(about,5)
        addLabel(about,"Created by Abihani Isa",14f,muted)
        about.addView(button("Meet the founder",{founderDialog()},false))
        content.addView(about)
    }

    private fun productScreen() {
        val intro=card()
        addLabel(intro,"Product cost breakdown",18f,ink,true); gap(intro,5)
        addLabel(intro,"Save purchase costs and packaging sizes. Hani calculates the cost of each package and suggests the next higher ₦50 selling-price step.",13f,muted)
        intro.addView(button("＋  Add product",{addProductDialog()}))
        content.addView(intro)
        gap(content)
        val products=db.products()
        if(products.isEmpty()) emptyState("No products yet","Add your first product to start calculating package costs.")
        products.forEach { p ->
            val c=card()
            addLabel(c,p.name,17f,ink,true); gap(c,4)
            addLabel(c,"Purchase cost: ${ProductCalculator.naira(p.purchasePriceKobo)}",14f,muted)
            addLabel(c,"Purchase quantity: ${p.purchaseQuantity} ${p.baseUnit}",13f,muted)
            gap(c,8)
            db.packages(p.id).forEach { pack ->
                val cost=ProductCalculator.costForPackage(p.purchasePriceKobo,pack.quantity,p.purchaseQuantity)
                val suggested=ProductCalculator.suggestedSellingPriceKobo(cost)
                addLabel(c,"${pack.name} (${pack.quantity} ${p.baseUnit})",14f,ink,true)
                addLabel(c,"Cost ${ProductCalculator.naira(cost)}  ·  Suggested ${ProductCalculator.naira(suggested)}",13f,green)
                gap(c,5)
            }
            content.addView(c); gap(content)
        }
    }

    private fun addProductDialog() {
        val form=column().apply { setPadding(dp(8),dp(4),dp(8),0) }
        val name=field("Product name")
        val price=field("Purchase price in Naira",3)
        val base=field("Smallest unit (e.g. sachet, diaper)")
        val purchaseQty=field("Quantity in smallest units in the purchased pack",8194)
        val packaging=EditText(this).apply {
            hint="Package levels, one per line: Carton=50\\nStrip=4\\nSachet=1"
            textSize=14f; minLines=4; gravity=Gravity.TOP
            setTextColor(ink); setHintTextColor(muted); background=rounded(bg,12)
            setPadding(dp(12),dp(10),dp(12),dp(10))
        }
        listOf(name,price,base,purchaseQty).forEach { addField(form,it) }
        form.addView(text("Package levels (name=quantity in smallest units)",12f,muted,true))
        gap(form,5); form.addView(packaging,LinearLayout.LayoutParams(-1,dp(112)))
        AlertDialog.Builder(this).setTitle("Add product").setView(form)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Save",null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(-1).setOnClickListener {
                        try {
                            val n=name.text.toString().trim()
                            val b=base.text.toString().trim()
                            val pq=BigDecimal(purchaseQty.text.toString().trim())
                            val pk=ProductCalculator.parseNaira(price.text.toString())
                            val packs=packaging.text.toString().lines().filter { it.contains("=") }.map {
                                val parts=it.split("=",limit=2)
                                PackageRecord(parts[0].trim(),BigDecimal(parts[1].trim()).stripTrailingZeros().toPlainString())
                            }
                            require(n.isNotBlank() && b.isNotBlank() && pk>0 && pq>BigDecimal.ZERO)
                            require(packs.isNotEmpty() && packs.all { it.name.isNotBlank() && BigDecimal(it.quantity)>BigDecimal.ZERO })
                            require(packs.any { BigDecimal(it.quantity).compareTo(pq)==0 }) {
                                "Include the purchased package among the package levels."
                            }
                            db.addProduct(n,pk,b,pq.stripTrailingZeros().toPlainString(),packs)
                            dialog.dismiss(); render()
                        } catch(e:Exception) {
                            Toast.makeText(this,"Check the product details and package quantities.",Toast.LENGTH_LONG).show()
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun customerScreen() {
        val top=card()
        addLabel(top,"Customer accounts",18f,ink,true); gap(top,5)
        addLabel(top,"Record goods collected, customer payments and cash advances. Balances update automatically.",13f,muted)
        top.addView(button("＋  Add customer",{addCustomerDialog()}))
        content.addView(top); gap(content)
        val customers=db.customers()
        if(customers.isEmpty()) emptyState("No customers yet","Add a customer to start an account.")
        customers.forEach { c ->
            val item=card()
            addLabel(item,c.name,16f,ink,true)
            if(c.phone.isNotBlank()) addLabel(item,c.phone,13f,muted)
            gap(item,5)
            addLabel(item,"Balance: ${ProductCalculator.naira(c.balance)}",15f,if(c.balance>0) green else muted,true)
            item.addView(button("Open account",{selectedCustomer=c.id;customerDetail(c.id)}))
            content.addView(item); gap(content)
        }
    }

    private fun addCustomerDialog() {
        val form=column().apply { setPadding(dp(8),dp(4),dp(8),0) }
        val name=field("Customer name")
        val phone=field("Phone number",3)
        addField(form,name); addField(form,phone)
        AlertDialog.Builder(this).setTitle("New customer").setView(form)
            .setNegativeButton("Cancel",null).setPositiveButton("Save",null).create().also { d ->
                d.setOnShowListener {
                    d.getButton(-1).setOnClickListener {
                        val n=name.text.toString().trim()
                        if(n.isBlank()) { name.error="Enter a name"; return@setOnClickListener }
                        db.addCustomer(n,phone.text.toString().trim())
                        d.dismiss(); render()
                    }
                }
                d.show()
            }
    }

    private fun customerDetail(id:Long) {
        val customer=db.customers().firstOrNull { it.id==id } ?: run { render(); return }
        val balance=customer.balance
        val box=column().apply { setPadding(dp(18),dp(18),dp(18),dp(18)); setBackgroundColor(bg) }
        val heading=card()
        addLabel(heading,customer.name,21f,ink,true)
        if(customer.phone.isNotBlank()) addLabel(heading,customer.phone,13f,muted)
        gap(heading,8); addLabel(heading,"Current balance",12f,muted)
        addLabel(heading,ProductCalculator.naira(balance),25f,green,true)
        box.addView(heading)
        gap(box)
        val actions=card()
        actions.addView(button("＋  Record goods collected",{entryDialog(id,"SALE")}))
        gap(actions,8)
        actions.addView(button("Record customer payment",{entryDialog(id,"PAYMENT")},false))
        gap(actions,8)
        actions.addView(button("Record cash given to customer",{entryDialog(id,"ADVANCE")},false))
        gap(actions,8)
        actions.addView(button("Record money returned by customer",{entryDialog(id,"RETURN")},false))
        gap(actions,8)
        actions.addView(button("Add customer memory note",{noteDialog(id)},false))
        gap(actions,8)
        actions.addView(button("Settle this account",{settleDialog(id)},false))
        box.addView(actions); gap(box)
        addLabel(box,"Transaction history",17f,ink,true); gap(box,8)
        val entries=db.ledger(id)
        if(entries.isEmpty()) addLabel(box,"No transactions in this account cycle yet.",13f,muted)
        entries.forEach { e ->
            val c=card()
            val positive=e.type=="SALE" || e.type=="ADVANCE"
            addLabel(c,e.description,15f,ink,true)
            addLabel(c,"${e.type.replace("_"," ")}  ·  ${e.date}",11f,muted)
            addLabel(c,(if(positive) "+" else "−")+ProductCalculator.naira(e.amount),15f,if(positive) green else muted,true)
            if(e.note.isNotBlank()) addLabel(c,e.note,12f,muted)
            box.addView(c); gap(box,7)
        }
        val scroll=ScrollView(this).apply { addView(box) }
        AlertDialog.Builder(this).setTitle("Customer account").setView(scroll)
            .setPositiveButton("Close",null).show()
    }

    private fun entryDialog(id:Long,type:String) {
        val title=when(type) {
            "SALE"->"Goods collected"; "PAYMENT"->"Customer payment"
            "ADVANCE"->"Cash given to customer"; else->"Money returned"
        }
        val form=column().apply { setPadding(dp(8),dp(4),dp(8),0) }
        val description=field(if(type=="SALE") "Items collected (e.g. Hypo, macaroni)" else "Description")
        val amount=field("Amount in Naira",3)
        val note=field("Additional note (optional)")
        addField(form,description); addField(form,amount); addField(form,note)
        AlertDialog.Builder(this).setTitle(title).setView(form)
            .setNegativeButton("Cancel",null).setPositiveButton("Save",null).create().also { d ->
                d.setOnShowListener {
                    d.getButton(-1).setOnClickListener {
                        try {
                            val desc=description.text.toString().trim()
                            val money=ProductCalculator.parseNaira(amount.text.toString())
                            require(desc.isNotBlank() && money>0)
                            db.addEntry(id,type,desc,money,note.text.toString().trim())
                            d.dismiss(); customerDetail(id); render()
                        } catch(e:Exception) {
                            Toast.makeText(this,"Enter a description and a valid amount.",Toast.LENGTH_LONG).show()
                        }
                    }
                }
                d.show()
            }
    }

    private fun settleDialog(id:Long) {
        AlertDialog.Builder(this).setTitle("Settle account")
            .setMessage("An account can only be settled when its balance is exactly zero. Its transaction history will be preserved.")
            .setNegativeButton("Cancel",null).setPositiveButton("Settle",null).create().also { d ->
                d.setOnShowListener {
                    d.getButton(-1).setOnClickListener {
                        if(db.settle(id)) {
                            d.dismiss(); render()
                            Toast.makeText(this,"Account cycle settled. History preserved.",Toast.LENGTH_LONG).show()
                        } else Toast.makeText(this,"This account balance must be zero before settlement.",Toast.LENGTH_LONG).show()
                    }
                }
                d.show()
            }
    }

    private fun memoryScreen() {
        val search=field("Search customer, title or note")
        val holder=column()
        val panel=card()
        addLabel(panel,"Customer memory",18f,ink,true); gap(panel,5)
        addLabel(panel,"Keep useful information about customer preferences, agreements and reminders separately from financial transactions.",13f,muted)
        content.addView(panel); gap(content)
        content.addView(search); gap(content)
        content.addView(holder)
        fun refresh(q:String) {
            holder.removeAllViews()
            val notes=db.searchNotes(q.trim())
            if(notes.isEmpty()) {
                holder.addView(text("No matching notes. Add a note from a customer's account.",13f,muted))
            }
            notes.forEach { n ->
                val c=card()
                addLabel(c,n.title,16f,ink,true)
                addLabel(c,n.customerName,12f,green,true)
                gap(c,5); addLabel(c,n.body,14f,muted)
                holder.addView(c); gap(holder)
            }
        }
        search.addTextChangedListener(object:android.text.TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { refresh(s?.toString() ?: "") }
            override fun afterTextChanged(s:android.text.Editable?) {}
        })
        refresh("")
    }

    private fun noteDialog(id:Long) {
        val form=column().apply { setPadding(dp(8),dp(4),dp(8),0) }
        val title=field("Note title")
        val body=EditText(this).apply {
            hint="Write the customer's preference, agreement or reminder"
            minLines=4; gravity=Gravity.TOP; textSize=14f
            setTextColor(ink); background=rounded(bg,12)
            setPadding(dp(12),dp(10),dp(12),dp(10))
        }
        addField(form,title); form.addView(body,LinearLayout.LayoutParams(-1,dp(110)))
        AlertDialog.Builder(this).setTitle("Customer memory").setView(form)
            .setNegativeButton("Cancel",null).setPositiveButton("Save note",null).create().also { d ->
                d.setOnShowListener {
                    d.getButton(-1).setOnClickListener {
                        val t=title.text.toString().trim()
                        val b=body.text.toString().trim()
                        if(t.isBlank() || b.isBlank()) {
                            Toast.makeText(this,"Enter a title and note.",Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        db.addNote(id,t,b); d.dismiss(); customerDetail(id)
                    }
                }
                d.show()
            }
    }

    private fun founderDialog() {
        val layout=column().apply { setPadding(dp(18),dp(12),dp(18),dp(8)) }
        val photo=ImageView(this).apply {
            setImageResource(R.drawable.family)
            scaleType=ImageView.ScaleType.CENTER_CROP
            background=rounded(pale,16); clipToOutline=true
        }
        layout.addView(photo,LinearLayout.LayoutParams(-1,dp(210)))
        gap(layout,12)
        addLabel(layout,"Isa Yahya Bayero",20f,ink,true)
        addLabel(layout,"Popularly known as Abihani Isa",13f,green,true)
        gap(layout,8)
        addLabel(layout,"Author of Symphony of Two Hearts: Abihani & Ummihani and My Gift to Dear Students. Creator of Madrasatus Sahaba, Veritas and Abihani Express.",14f,muted)
        gap(layout,8)
        addLabel(layout,"abihaniisa@gmail.com",13f,ink)
        addLabel(layout,"WhatsApp: +234 706 755 1684",13f,ink)
        AlertDialog.Builder(this).setTitle("About the founder").setView(layout)
            .setPositiveButton("Close",null).show()
    }

    private fun emptyState(title:String,description:String) {
        val c=card()
        addLabel(c,title,16f,ink,true); gap(c,5); addLabel(c,description,13f,muted)
        content.addView(c); gap(content)
    }
}
