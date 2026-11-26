package com.abihaniisa.hani

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.text.*
import com.abihaniisa.hani.data.*
import java.math.BigDecimal

class MainActivity:Activity(){
 private lateinit var db:DatabaseHelper
 private lateinit var root:LinearLayout
 private lateinit var body:LinearLayout
 private var page=0
 private var customerId=-1L
 private val bg=Color.rgb(246,247,244);private val ink=Color.rgb(27,39,34);private val muted=Color.rgb(112,124,117)
 private val green=Color.rgb(25,91,72);private val mint=Color.rgb(226,240,232);private val white=Color.WHITE
 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);db=DatabaseHelper(this);window.statusBarColor=bg;window.navigationBarColor=white;window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;render()}
 private fun shape(color:Int,r:Int=18,stroke:Int=0)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat();if(stroke!=0)setStroke(dp(1),stroke)}
 private fun col()=LinearLayout(this).apply{orientation=1}
 private fun row()=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
 private fun label(s:String,size:Float=14f,color:Int=ink,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(color);if(bold)typeface=Typeface.create("sans-serif-medium",0);setLineSpacing(dp(2),1f)}
 private fun space(p:LinearLayout,h:Int=12){p.addView(View(this),LinearLayout.LayoutParams(1,dp(h)))}
 private fun card()=col().apply{background=shape(white,20);setPadding(dp(17),dp(16),dp(17),dp(16));elevation=dp(1).toFloat()}
 private fun btn(s:String,primary:Boolean=true,action:()->Unit)=TextView(this).apply{text=s;textSize=14f;typeface=Typeface.create("sans-serif-medium",0);gravity=Gravity.CENTER;setTextColor(if(primary)white else green);background=shape(if(primary)green else mint,14);setPadding(dp(16),dp(14),dp(16),dp(14));minHeight=dp(48);setOnClickListener{action()}}
 private fun field(h:String,type:Int=1,multi:Boolean=false)=EditText(this).apply{hint=h;textSize=15f;setTextColor(ink);setHintTextColor(muted);inputType=type;background=shape(bg,13);setPadding(dp(14),dp(12),dp(14),dp(12));if(multi){minLines=3;gravity=Gravity.TOP}else setSingleLine(true)}
 private fun addField(p:LinearLayout,e:EditText){p.addView(e,LinearLayout.LayoutParams(-1,if(e.minLines>1)dp(112) else dp(52)));space(p,9)}
 private fun naira(v:Long)=ProductCalculator.naira(v)
 private fun render(){
  root=col().apply{setBackgroundColor(bg)}
  val header=col().apply{setPadding(dp(20),dp(14),dp(20),dp(13));setBackgroundColor(white)}
  val top=row()
  val logo=ImageView(this).apply{setImageResource(R.drawable.hani);scaleType=ImageView.ScaleType.CENTER_CROP;background=shape(mint,13);clipToOutline=true}
  top.addView(logo,LinearLayout.LayoutParams(dp(42),dp(42)))
  val heading=col().apply{setPadding(dp(11),0,0,0)}
  heading.addView(label("HANI  /  BUSINESS",11f,green,true))
  heading.addView(label(listOf("Your overview","Products","Customer ledger","Customer memory")[page],22f,ink,true))
  top.addView(heading,LinearLayout.LayoutParams(0,-2,1f))
  val more=label("⋯",28f,ink,true).apply{gravity=Gravity.CENTER;setOnClickListener{founder()}}
  top.addView(more,LinearLayout.LayoutParams(dp(40),dp(42)))
  header.addView(top);root.addView(header)
  body=col().apply{setPadding(dp(17),dp(18),dp(17),dp(22))}
  val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false;addView(body)}
  root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  when(page){0->home();1->products();2->customers();else->memory()}
  val nav=row().apply{setPadding(dp(8),dp(8),dp(8),dp(8));setBackgroundColor(white);elevation=dp(12).toFloat()}
  listOf("Products","Ledger","Memory").forEachIndexed{i,n->
   val active=page==i+1
   val v=col().apply{gravity=Gravity.CENTER;setPadding(dp(5),dp(8),dp(5),dp(8));if(active)background=shape(mint,14);setOnClickListener{page=i+1;render()}}
   v.addView(label(listOf("▦","◷","✳")[i],19f,if(active)green else muted,true).apply{gravity=Gravity.CENTER})
   v.addView(label(n,11f,if(active)green else muted,true).apply{gravity=Gravity.CENTER})
   nav.addView(v,LinearLayout.LayoutParams(0,-2,1f))
  }
  root.addView(nav);setContentView(root)
 }
 private fun home(){
  val cs=db.customers();val debt=cs.sumOf{it.balance}
  val hero=card().apply{background=shape(green,22)}
  hero.addView(label("A clearer view of your business",22f,white,true));space(hero,7)
  hero.addView(label("Product costs, customer accounts and the details worth remembering — together.",14f,Color.rgb(220,235,227)))
  space(hero,16);hero.addView(btn("＋  Add a product",true){page=1;render();productForm()})
  body.addView(hero);space(body,14)
  val stats=row()
  listOf(Triple("Products",db.products().size.toString(),green),Triple("Customers",cs.size.toString(),ink),Triple("Outstanding",naira(debt),green)).forEach{(t,v,c)->
   val x=card();x.addView(label(t,11f,muted,true));space(x,6);x.addView(label(v,if(t=="Outstanding")14f else 22f,c,true))
   stats.addView(x,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(6)})
  };body.addView(stats);space(body,15)
  val quick=card();quick.addView(label("Your workspace",17f,ink,true));space(quick,9)
  quick.addView(btn("Manage products",false){page=1;render()});space(quick,8)
  quick.addView(btn("Open customer ledger",false){page=2;render()});space(quick,8)
  quick.addView(btn("Search customer memory",false){page=3;render()})
  body.addView(quick);space(body,14)
  val about=card();about.addView(label("Made for the way your shop works.",16f,ink,true));space(about,5);about.addView(label("Hani keeps the essential records simple, organised and close at hand.",13f,muted));body.addView(about)
 }
 private fun products(){
  val intro=card();intro.addView(label("Know what every package costs.",18f,ink,true));space(intro,5);intro.addView(label("Enter your purchase cost and define packaging in your own units. Hani calculates unit costs and the next higher ₦50 selling-price step.",13f,muted));space(intro,12);intro.addView(btn("＋  Add product"){productForm()});body.addView(intro);space(body)
  val list=db.products();if(list.isEmpty())empty("No products yet","Add a product to see its package cost breakdown.")
  list.forEach{p->
   val c=card();c.addView(label(p.name,17f,ink,true));space(c,4);c.addView(label("Purchase ${naira(p.purchasePriceKobo)}  ·  ${p.purchaseQuantity} ${p.baseUnit}",12f,muted));space(c,10)
   db.packages(p.id).forEach{pk->val cost=ProductCalculator.costForPackage(p.purchasePriceKobo,pk.quantity,p.purchaseQuantity);c.addView(label(pk.name+"  ·  "+pk.quantity+" "+p.baseUnit,13f,ink,true));c.addView(label("Cost ${naira(cost)}     Suggested ${naira(ProductCalculator.suggestedSellingPriceKobo(cost))}",12f,green));space(c,6)}
   val actions=row();actions.addView(btn("Edit",false){productForm(p)} ,LinearLayout.LayoutParams(0,-2,1f));actions.addView(View(this),LinearLayout.LayoutParams(dp(8),1));actions.addView(btn("Delete",false){confirm("Delete ${p.name}?","This removes the product and its packaging details."){db.deleteProduct(p.id);render()}},LinearLayout.LayoutParams(0,-2,1f));c.addView(actions);body.addView(c);space(body)
  }
 }
 private fun productForm(old:ProductRecord?=null){
  val f=col().apply{setPadding(dp(5),dp(4),dp(5),0)}
  val n=field("Product name").apply{setText(old?.name?:"")}
  val price=field("Purchase price (₦)",8194).apply{if(old!=null)setText(BigDecimal.valueOf(old.purchasePriceKobo).divide(BigDecimal(100)).toPlainString())}
  val unit=field("Smallest unit (e.g. sachet)").apply{setText(old?.baseUnit?:"")}
  val qty=field("Purchased pack quantity in smallest units",8194).apply{setText(old?.purchaseQuantity?:"")}
  val packs=field("One package per line: Carton=50\nStrip=4\nSachet=1",1,true).apply{setText(db.packages(old?.id?:-1).joinToString("\n"){"${it.name}=${it.quantity}"})}
  listOf(n,price,unit,qty,packs).forEach{addField(f,it)}
  AlertDialog.Builder(this).setTitle(if(old==null)"Add product" else "Edit product").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create().also{d->d.setOnShowListener{d.getButton(-1).setOnClickListener{
   try{val pk=ProductCalculator.parseNaira(price.text.toString());val q=BigDecimal(qty.text.toString().trim());val parsed=packs.text.lines().filter{it.contains("=")}.map{val a=it.split("=",limit=2);PackageRecord(a[0].trim(),BigDecimal(a[1].trim()).stripTrailingZeros().toPlainString())};require(n.text.isNotBlank()&&unit.text.isNotBlank()&&pk>0&&q>BigDecimal.ZERO&&parsed.isNotEmpty()&&parsed.all{it.name.isNotBlank()&&BigDecimal(it.quantity)>BigDecimal.ZERO}&&parsed.any{BigDecimal(it.quantity).compareTo(q)==0});if(old==null)db.addProduct(n.text.toString().trim(),pk,unit.text.toString().trim(),q.toPlainString(),parsed)else db.updateProduct(old.id,n.text.toString().trim(),pk,unit.text.toString().trim(),q.toPlainString(),parsed);d.dismiss();render()}catch(e:Exception){Toast.makeText(this,"Check all fields and include the purchased pack in package levels.",Toast.LENGTH_LONG).show()}
  }};d.show()}
 }
 private fun customers(){
  val intro=card();intro.addView(label("Customer accounts",18f,ink,true));space(intro,5);intro.addView(label("Record goods collected, payments, cash advances and returns. Every entry contributes to the live balance.",13f,muted));space(intro,12);intro.addView(btn("＋  Add customer"){customerForm()});body.addView(intro);space(body)
  val list=db.customers();if(list.isEmpty())empty("No customers yet","Add a customer to start a ledger.")
  list.forEach{c->val x=card();x.addView(label(c.name,16f,ink,true));if(c.phone.isNotBlank())x.addView(label(c.phone,12f,muted));space(x,7);x.addView(label("Balance  "+naira(c.balance),16f,if(c.balance>0)green else muted,true));space(x,10);x.addView(btn("Open account"){customerId=c.id;detail(c.id)});space(x,7);x.addView(btn("Delete customer",false){confirm("Delete ${c.name}?","This permanently deletes this customer, all ledger transactions, account cycles and memory notes."){db.deleteCustomer(c.id);render()}});body.addView(x);space(body)}
 }
 private fun customerForm(){val f=col().apply{setPadding(dp(5),dp(4),dp(5),0)};val n=field("Customer name");val p=field("Phone number",3);addField(f,n);addField(f,p);AlertDialog.Builder(this).setTitle("New customer").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->if(n.text.isNotBlank()){db.addCustomer(n.text.toString().trim(),p.text.toString().trim());render()}else n.error="Enter a name"}.show()}
 private fun detail(id:Long){
  val c=db.customers().firstOrNull{it.id==id}?:return
  val f=col().apply{setPadding(dp(16),dp(8),dp(16),dp(8))}
  val head=card();head.addView(label(c.name,21f,ink,true));head.addView(label(c.phone,12f,muted));space(head,8);head.addView(label("CURRENT BALANCE",10f,muted,true));head.addView(label(naira(c.balance),27f,green,true));f.addView(head);space(f)
  listOf("Goods collected" to "SALE","Customer payment" to "PAYMENT","Cash given to customer" to "ADVANCE","Money returned" to "RETURN").forEach{(t,k)->f.addView(btn(t,k=="SALE"){entry(id,k)});space(f,7)}
  f.addView(btn("Add customer memory note",false){noteForm(id)});space(f,14);f.addView(label("TRANSACTION HISTORY",12f,muted,true));space(f,8)
  val entries=db.ledger(id);if(entries.isEmpty())f.addView(label("No transactions recorded yet.",13f,muted))
  entries.forEach{e->val x=card();x.addView(label(e.description,15f,ink,true));x.addView(label("${e.type}  ·  ${e.date}",11f,muted));x.addView(label((if(e.type=="SALE"||e.type=="ADVANCE")"+" else "−")+naira(e.amount),14f,green,true));if(e.note.isNotBlank())x.addView(label(e.note,12f,muted));space(x,7);x.addView(btn("Delete transaction",false){confirm("Delete this transaction?","The selected entry will be removed and the customer balance recalculated."){db.deleteEntry(e.id);(f.parent as? ViewGroup)?.let{};detail(id)}});f.addView(x);space(f,8)}
  val sc=ScrollView(this).apply{addView(f)};AlertDialog.Builder(this).setTitle("Customer ledger").setView(sc).setPositiveButton("Close",null).show()
 }
 private fun entry(id:Long,type:String){val f=col().apply{setPadding(dp(5),dp(4),dp(5),0)};val desc=field("Description / items collected");val amount=field("Amount in Naira",8194);val note=field("Optional note");listOf(desc,amount,note).forEach{addField(f,it)};AlertDialog.Builder(this).setTitle(type.replace("_"," ")).setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create().also{d->d.setOnShowListener{d.getButton(-1).setOnClickListener{try{val a=ProductCalculator.parseNaira(amount.text.toString());require(desc.text.isNotBlank()&&a>0);db.addEntry(id,type,desc.text.toString().trim(),a,note.text.toString().trim());d.dismiss();detail(id)}catch(e:Exception){Toast.makeText(this,"Enter a description and valid amount.",Toast.LENGTH_SHORT).show()}}};d.show()}}
 private fun memory(){
  val intro=card();intro.addView(label("Remember what matters.",18f,ink,true));space(intro,5);intro.addView(label("Preferences, agreements and reminders stay separate from financial records.",13f,muted));body.addView(intro);space(body)
  val search=field("Search customer, title or note");body.addView(search);space(body)
  val holder=col();body.addView(holder)
  fun refresh(q:String){holder.removeAllViews();val notes=db.searchNotes(q);if(notes.isEmpty())holder.addView(label("No matching notes. Add a note from a customer's account.",13f,muted));notes.forEach{n->val x=card();x.addView(label(n.title,16f,ink,true));x.addView(label(n.customerName+"  ·  "+n.date,11f,green,true));space(x,5);x.addView(label(n.body,14f,muted));space(x,9);x.addView(btn("Edit note",false){noteForm(n.customerId,n)});space(x,7);x.addView(btn("Delete note",false){confirm("Delete this note?","This removes the selected customer memory note."){db.deleteNote(n.id);refresh(search.text.toString())}});holder.addView(x);space(holder)}} 
  search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){refresh(s?.toString()?:"")};override fun afterTextChanged(e:Editable?){}});refresh("")
 }
 private fun noteForm(id:Long,old:NoteRecord?=null){val f=col().apply{setPadding(dp(5),dp(4),dp(5),0)};val t=field("Note title").apply{setText(old?.title?:"")};val b=field("Customer preference, agreement or reminder",1,true).apply{setText(old?.body?:"")};addField(f,t);addField(f,b);AlertDialog.Builder(this).setTitle(if(old==null)"Customer memory" else "Edit memory").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create().also{d->d.setOnShowListener{d.getButton(-1).setOnClickListener{if(t.text.isBlank()||b.text.isBlank()){Toast.makeText(this,"Enter a title and note.",Toast.LENGTH_SHORT).show()}else{if(old==null)db.addNote(id,t.text.toString().trim(),b.text.toString().trim())else db.updateNote(old.id,t.text.toString().trim(),b.text.toString().trim());d.dismiss();if(page==3)render()}}};d.show()}}
 private fun confirm(title:String,message:String,yes:()->Unit){AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->yes()}.show()}
 private fun empty(title:String,desc:String){val c=card();c.addView(label(title,16f,ink,true));space(c,5);c.addView(label(desc,13f,muted));body.addView(c);space(body)}
 private fun founder(){
  val f=col().apply{setPadding(dp(16),dp(8),dp(16),dp(8))}
  val photo=ImageView(this).apply{setImageResource(R.drawable.family);scaleType=ImageView.ScaleType.CENTER_CROP;background=shape(mint,18);clipToOutline=true}
  f.addView(photo,LinearLayout.LayoutParams(-1,dp(190)));space(f,12);f.addView(label(DeveloperProfile.name,20f,ink,true));f.addView(label("Popularly known as ${DeveloperProfile.knownAs}",13f,green,true));space(f,8);f.addView(label(DeveloperProfile.biography,13f,muted));space(f,10)
  fun link(text:String,url:String){f.addView(btn(text,false){try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}catch(_:Exception){Toast.makeText(this,"No app available to open this link.",Toast.LENGTH_SHORT).show()}});space(f,6)}
  link("Madrasatus Sahaba",DeveloperProfile.madrasatusSahaba);link("Veritas",DeveloperProfile.veritas);link("Abihani Express",DeveloperProfile.abihaniExpress);link("Email", "mailto:${DeveloperProfile.email}");link("Call", "tel:${DeveloperProfile.whatsapp}");link("WhatsApp","https://wa.me/2347067551684")
  AlertDialog.Builder(this).setTitle("About Hani").setView(f).setPositiveButton("Close",null).show()
 }
}
