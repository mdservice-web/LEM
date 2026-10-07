package com.loanemimanager

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var summary: TextView
    private val prefs by lazy { getSharedPreferences("loans", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
    }

    private fun money(n: Long): String =
        NumberFormat.getCurrencyInstance(Locale("en","IN")).format(n).replace("₹", "₹")

    private fun load(): JSONArray = JSONArray(prefs.getString("data", "[]"))

    private fun save(a: JSONArray) {
        prefs.edit().putString("data", a.toString()).apply()
    }

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 24)
        }
        summary = TextView(this).apply { textSize = 18f }
        val title = TextView(this).apply {
            text = "Loan EMI Manager"
            textSize = 28f
            setPadding(0,0,0,18)
        }
        val add = Button(this).apply {
            text = "+ Add Loan"
            setOnClickListener { showAddLoan() }
        }
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 0)
        }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(title)
        root.addView(summary)
        root.addView(add)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refresh()
    }

    private fun refresh() {
        val a = load()
        var total = 0L
        var emi = 0L
        list.removeAllViews()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            total += o.optLong("remaining")
            emi += o.optLong("emi")
            val card = TextView(this).apply {
                text = "${o.optString("name")}\nEMI: ${money(o.optLong("emi"))}   Remaining: ${money(o.optLong("remaining"))}\nDue: ${o.optString("due")}"
                textSize = 17f
                setPadding(22, 20, 22, 20)
                setOnClickListener { showLoan(i) }
            }
            list.addView(card)
            list.addView(Space(this).apply { minimumHeight = 12 })
        }
        summary.text = "Total remaining: ${money(total)}\nMonthly EMI: ${money(emi)}\nActive loans: ${a.length()}"
    }

    private fun showAddLoan() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,10,40,10) }
        val name = EditText(this).apply { hint = "Loan / EMI name" }
        val lender = EditText(this).apply { hint = "Lender name (optional)" }
        val amount = EditText(this).apply { hint = "Total loan amount"; inputType = 2 }
        val emi = EditText(this).apply { hint = "Monthly EMI"; inputType = 2 }
        val due = EditText(this).apply { hint = "Due date (e.g. 7th)" }
        listOf(name,lender,amount,emi,due).forEach { box.addView(it) }
        AlertDialogBuilder().create(box) { 
            if (name.text.isBlank() || amount.text.isBlank() || emi.text.isBlank()) return@create
            val a = load()
            a.put(JSONObject().apply {
                put("name", name.text.toString())
                put("lender", lender.text.toString())
                put("amount", amount.text.toString().toLongOrNull() ?: 0)
                put("emi", emi.text.toString().toLongOrNull() ?: 0)
                put("remaining", amount.text.toString().toLongOrNull() ?: 0)
                put("due", due.text.toString())
                put("paid", 0)
            })
            save(a); refresh()
        }
    }

    private fun showLoan(index: Int) {
        val a = load()
        val o = a.getJSONObject(index)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,10,40,10) }
        box.addView(TextView(this).apply {
            text = "${o.optString("name")}\n\nLender: ${o.optString("lender")}\nTotal: ${money(o.optLong("amount"))}\nEMI: ${money(o.optLong("emi"))}\nRemaining: ${money(o.optLong("remaining"))}\nPaid EMIs: ${o.optInt("paid")}"
            textSize = 18f
        })
        AlertDialogBuilder().create(box) {
            val e = o.optLong("emi")
            val r = o.optLong("remaining")
            if (e > 0 && r > 0) {
                o.put("remaining", maxOf(0L, r - e))
                o.put("paid", o.optInt("paid") + 1)
                save(a); refresh()
            }
        }.setTitle("Loan Details").show()
    }

    private fun AlertDialogBuilder(): BuilderWrapper = BuilderWrapper()

    private inner class BuilderWrapper {
        fun create(view: android.view.View, action: () -> Unit): android.app.AlertDialog {
            return android.app.AlertDialog.Builder(this@MainActivity)
                .setView(view)
                .setPositiveButton("Save / Mark Paid") { _, _ -> action() }
                .setNegativeButton("Cancel", null)
                .create()
        }
        fun setTitle(t: String): BuilderWrapper = this
        fun show() {}
    }
}
