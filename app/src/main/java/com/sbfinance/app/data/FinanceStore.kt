package com.sbfinance.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sbfinance.app.reminders.Reminders
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

fun uid() = UUID.randomUUID().toString()
fun today() = LocalDate.now().toString()

data class Entry(
    val id: String = uid(), val title: String, val cents: Long,
    val kind: String, val category: String, val date: String = today(),
    val billId: String = "", val billPeriod: String = ""
)

data class Bill(
    val id: String = uid(), val title: String, val cents: Long,
    val category: String, val nextDate: String, val installmentCount: Int = 0,
    val completedCount: Int = 0, val active: Boolean = true, val dueDay: Int = 0
)

data class Goal(val id: String = uid(), val title: String, val target: Long, val saved: Long = 0L)

data class FinanceData(
    val entries: List<Entry> = emptyList(), val bills: List<Bill> = emptyList(),
    val goals: List<Goal> = emptyList(), val budgets: Map<String, Long> = emptyMap(),
    val hideBalance: Boolean = false, val notifyHour: Int = 9
)

private fun JSONObject.str(name: String, fallback: String = "") = optString(name, fallback)
private fun JSONObject.num(name: String) = optLong(name, 0L)

fun toJson(state: FinanceData): String = JSONObject().apply {
    put("schema", 3)
    put("entries", JSONArray().also { array -> state.entries.forEach { e -> array.put(JSONObject().apply {
        put("id", e.id); put("title", e.title); put("cents", e.cents); put("kind", e.kind)
        put("category", e.category); put("date", e.date); put("billId", e.billId); put("billPeriod", e.billPeriod)
    }) } })
    put("bills", JSONArray().also { array -> state.bills.forEach { b -> array.put(JSONObject().apply {
        put("id", b.id); put("title", b.title); put("cents", b.cents); put("category", b.category)
        put("nextDate", b.nextDate); put("installmentCount", b.installmentCount)
        put("completedCount", b.completedCount); put("active", b.active); put("dueDay", b.dueDay)
    }) } })
    put("goals", JSONArray().also { array -> state.goals.forEach { g -> array.put(JSONObject().apply {
        put("id", g.id); put("title", g.title); put("target", g.target); put("saved", g.saved)
    }) } })
    put("budgets", JSONObject().also { obj -> state.budgets.forEach { (k, v) -> obj.put(k, v) } })
    put("hideBalance", state.hideBalance); put("notifyHour", state.notifyHour)
}.toString(2)

fun fromJson(raw: String): FinanceData {
    val obj = JSONObject(raw)
    require(obj.optInt("schema", 0) in 1..3) { "Arquivo de backup incompatível." }
    val entries = obj.optJSONArray("entries") ?: JSONArray()
    val bills = obj.optJSONArray("bills") ?: JSONArray()
    val goals = obj.optJSONArray("goals") ?: JSONArray()
    val budgetObj = obj.optJSONObject("budgets") ?: JSONObject()
    return FinanceData(
        entries = (0 until entries.length()).mapNotNull { i -> entries.optJSONObject(i)?.let { e ->
            Entry(e.str("id", uid()), e.str("title"), e.num("cents"), e.str("kind"),
                e.str("category", "Outros"), e.str("date", today()), e.str("billId"), e.str("billPeriod"))
        } },
        bills = (0 until bills.length()).mapNotNull { i -> bills.optJSONObject(i)?.let { b ->
            Bill(b.str("id", uid()), b.str("title"), b.num("cents"), b.str("category", "Outros"),
                b.str("nextDate", today()), b.optInt("installmentCount", b.optInt("total", 0)),
                b.optInt("completedCount", b.optInt("paid", 0)), b.optBoolean("active", true), b.optInt("dueDay", 0))
        } },
        goals = (0 until goals.length()).mapNotNull { i -> goals.optJSONObject(i)?.let { g ->
            Goal(g.str("id", uid()), g.str("title"), g.num("target"), g.num("saved"))
        } },
        budgets = budgetObj.keys().asSequence().associateWith { budgetObj.optLong(it) },
        hideBalance = obj.optBoolean("hideBalance", obj.optBoolean("hide", false)), notifyHour = obj.optInt("notifyHour", 9).coerceIn(0,23)
    )
}

class FinanceStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("sbfinance_v3", Context.MODE_PRIVATE)
    var state by mutableStateOf(runCatching { fromJson(preferences.getString("data", "") ?: "") }.getOrDefault(FinanceData()))
        private set

    private fun commit(next: FinanceData) {
        state = next
        preferences.edit().putString("data", toJson(next)).apply()
        Reminders.scheduleAll(context, next.bills, next.notifyHour)
    }
    fun addEntry(entry: Entry) = commit(state.copy(entries = listOf(entry) + state.entries))
    fun editEntry(entry: Entry) = commit(state.copy(entries = state.entries.map { if (it.id == entry.id) entry else it }))
    fun deleteEntry(id: String): Boolean {
        val entry = state.entries.firstOrNull { it.id == id } ?: return false
        if (entry.billId.isNotBlank()) {
            val bill = state.bills.firstOrNull { it.id == entry.billId }
            if (bill != null) {
                val last = state.entries.filter { it.billId == entry.billId }
                    .maxByOrNull { it.billPeriod }
                if (last?.id != entry.id) return false
                val updated = bill.copy(nextDate = entry.billPeriod,
                    completedCount = (bill.completedCount - 1).coerceAtLeast(0), active = true)
                commit(state.copy(entries = state.entries.filterNot { it.id == id },
                    bills = state.bills.map { if (it.id == bill.id) updated else it }))
                return true
            }
        }
        commit(state.copy(entries = state.entries.filterNot { it.id == id }))
        return true
    }
    fun addBill(bill: Bill) = commit(state.copy(bills = listOf(bill) + state.bills))
    fun editBill(bill: Bill) = commit(state.copy(bills = state.bills.map { if (it.id == bill.id) bill else it }))
    fun deleteBill(id: String) {
        Reminders.cancel(context, id)
        commit(state.copy(bills = state.bills.filterNot { it.id == id }))
    }
    fun payBill(id: String): Boolean {
        val bill = state.bills.firstOrNull { it.id == id && it.active } ?: return false
        val period = bill.nextDate
        if (state.entries.any { it.billId == id && it.billPeriod == period }) return false
        val newCount = bill.completedCount + 1
        val nextDate = nextMonth(period, bill.dueDay)
        val updated = bill.copy(completedCount = newCount, nextDate = nextDate,
            active = bill.installmentCount == 0 || newCount < bill.installmentCount)
        val entry = Entry(title = bill.title, cents = bill.cents, kind = "expense",
            category = bill.category, billId = id, billPeriod = period)
        commit(state.copy(entries = listOf(entry) + state.entries,
            bills = state.bills.map { if (it.id == id) updated else it }))
        return true
    }
    fun addGoal(g: Goal) = commit(state.copy(goals = listOf(g) + state.goals))
    fun changeGoal(g: Goal) = commit(state.copy(goals = state.goals.map { if (it.id == g.id) g else it }))
    fun removeGoal(id: String) = commit(state.copy(goals = state.goals.filterNot { it.id == id }))
    fun setBudget(category: String, cents: Long) = commit(state.copy(budgets = state.budgets + (category to cents)))
    fun setHidden(hidden: Boolean) = commit(state.copy(hideBalance = hidden))
    fun setHour(hour: Int) = commit(state.copy(notifyHour = hour))
    fun exportJson() = toJson(state)
    fun restoreJson(json: String) {
        val restored = fromJson(json)
        state.bills.forEach { Reminders.cancel(context, it.id) }
        // Impede repetição de identificadores no histórico importado.
        require(restored.entries.map { it.id }.distinct().size == restored.entries.size) { "Backup contém lançamentos duplicados." }
        commit(restored)
    }
    fun clearAll() {
        state.bills.forEach { Reminders.cancel(context, it.id) }
        commit(FinanceData())
    }
    init { Reminders.scheduleAll(context, state.bills, state.notifyHour) }
}

fun nextMonth(date: String, originalDueDay: Int = 0): String = runCatching {
    val original = LocalDate.parse(date)
    val next = java.time.YearMonth.from(original.plusMonths(1))
    next.atDay((if (originalDueDay in 1..31) originalDueDay else original.dayOfMonth).coerceAtMost(next.lengthOfMonth())).toString()
}.getOrDefault(LocalDate.now().plusMonths(1).toString())
