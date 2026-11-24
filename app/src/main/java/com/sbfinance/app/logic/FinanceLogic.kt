package com.sbfinance.app.logic

import com.sbfinance.app.data.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val ptBR = Locale.forLanguageTag("pt-BR")
val categories = listOf("Alimentação", "Transporte", "Moradia", "Saúde", "Compras", "Lazer", "Educação", "Assinaturas", "Contas", "Outros")
fun money(cents: Long) = NumberFormat.getCurrencyInstance(ptBR).format(cents / 100.0)
fun dateBR(date: String): String = runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(date)
fun amountToCents(raw: String): Long? {
    val text = raw.trim().replace("R$", "").replace(" ", "")
    if (text.isBlank()) return null
    val number = when {
        text.contains(',') -> text.replace(".", "").replace(",", ".")
        Regex("\\d{1,3}(?:\\.\\d{3})+").matches(text) -> text.replace(".", "")
        else -> text
    }
    return number.toBigDecimalOrNull()?.multiply(java.math.BigDecimal(100))?.let {
        runCatching { it.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() }.getOrNull()
    }?.takeIf { it > 0 }
}
fun detectCategory(title: String): String {
    val s = title.lowercase(ptBR)
    val rules = linkedMapOf(
        "Alimentação" to listOf("mercado", "supermerc", "comida", "pastel", "lanche", "restaurante", "pizza", "ifood", "café", "almoço", "jantar", "padaria", "açaí", "coxinha"),
        "Transporte" to listOf("uber", "99", "combust", "gasolina", "ônibus", "estacion", "passagem", "moto", "posto"),
        "Moradia" to listOf("aluguel", "condomínio", "reforma", "imóvel"),
        "Saúde" to listOf("farmácia", "remédio", "médico", "consulta", "dentista", "hospital"),
        "Compras" to listOf("roupa", "loja", "sapato", "shopee", "amazon", "produto"),
        "Lazer" to listOf("cinema", "jogo", "steam", "viagem", "parque", "show"),
        "Educação" to listOf("curso", "escola", "faculdade", "livro", "aula"),
        "Assinaturas" to listOf("netflix", "spotify", "disney", "prime", "assinatura", "streaming"),
        "Contas" to listOf("energia", "luz", "água", "internet", "telefone", "fatura", "vivo")
    )
    return rules.entries.firstOrNull { (_, words) -> words.any { s.contains(it) } }?.key ?: "Outros"
}

data class ParsedEntry(val title: String, val cents: Long, val kind: String, val category: String)
fun parseNatural(raw: String): ParsedEntry? {
    val s = raw.trim()
    // Somente padrões monetários explícitos. Se ambíguo, não lança automaticamente.
    val regex = Regex("(?i)(?:r\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)(?![\\d.,])\\s*(?:reais|real|rs)?")
    val match = regex.find(s) ?: return null
    val cents = amountToCents(match.groupValues[1]) ?: return null
    val lower = s.lowercase(ptBR)
    val income = listOf("recebi", "ganhei", "salário", "salario", "renda", "entrou", "vendi", "pix recebido").any { lower.contains(it) }
    var title = s.replaceRange(match.range, "").replace(Regex("(?i)\\b(gastei|paguei|comprei|recebi|ganhei|de|com|em|por|reais|real|renda|um|uma)\\b"), " ")
        .replace(Regex("\\s+"), " ").trim(' ', '-', ',', '.')
    if (title.isBlank()) title = if (income) "Receita" else "Despesa"
    title = title.replaceFirstChar { it.titlecase(ptBR) }
    return ParsedEntry(title, cents, if (income) "income" else "expense", if (income) "Outros" else detectCategory(title))
}

fun sumIncome(entries: List<Entry>) = entries.filter { it.kind == "income" }.sumOf { it.cents }
fun sumExpense(entries: List<Entry>) = entries.filter { it.kind == "expense" }.sumOf { it.cents }
fun balance(entries: List<Entry>): Long {
    val posted = entries.filter { runCatching { !LocalDate.parse(it.date).isAfter(LocalDate.now()) }.getOrDefault(true) }
    return sumIncome(posted) - sumExpense(posted)
}
fun entriesOfMonth(entries: List<Entry>, month: YearMonth): List<Entry> = entries.filter {
    runCatching { YearMonth.from(LocalDate.parse(it.date)) == month }.getOrDefault(false)
}
fun predictedBalance(data: FinanceData): Long = balance(data.entries) - data.bills.filter { it.active }.sumOf { it.cents }
fun dueStatus(date: String): String = runCatching {
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date))
    when {
        days < 0 -> "Atrasada ${-days} dia(s)"
        days == 0L -> "Vence hoje"
        days == 1L -> "Vence amanhã"
        else -> "Vence em $days dias"
    }
}.getOrDefault("Data inválida")
fun summaryInsights(data: FinanceData): List<String> {
    val month = entriesOfMonth(data.entries, YearMonth.now())
    val income = sumIncome(month); val spent = sumExpense(month)
    val active = data.bills.filter { it.active }
    val result = mutableListOf<String>()
    if (month.isEmpty()) result += "Registre suas primeiras receitas e despesas para receber análises personalizadas."
    else {
        val pct = if (income > 0) spent * 100 / income else 0
        result += if (income > 0) "Você gastou $pct% das receitas registradas neste mês." else "Você registrou ${money(spent)} em despesas neste mês, mas ainda não informou receitas."
        val category = month.filter { it.kind == "expense" }.groupBy { it.category }.mapValues { (_, values) -> values.sumOf { it.cents } }.maxByOrNull { it.value }
        if (category != null) result += "Maior categoria de gastos: ${category.key} (${money(category.value)})."
        if (income > 0 && spent > income * 0.8) result += "Atenção: seus gastos já superam 80% da renda registrada no mês."
        else if (income > spent && income > 0) result += "Sua diferença positiva no mês é ${money(income - spent)}. Considere reservá-la para uma meta."
    }
    val soon = active.count {
        runCatching { LocalDate.parse(it.nextDate) <= LocalDate.now().plusDays(7) }.getOrDefault(false)
    }
    if (soon > 0) result += "Você tem $soon conta(s) com vencimento nos próximos 7 dias ou em atraso."
    data.budgets.forEach { (category, limit) ->
        val spentByCat = month.filter { it.kind == "expense" && it.category == category }.sumOf { it.cents }
        if (limit > 0 && spentByCat > limit) result += "O orçamento de $category foi excedido em ${money(spentByCat - limit)}."
    }
    result += "Saldo projetado após as próximas contas cadastradas: ${money(predictedBalance(data))}. Não inclui despesas futuras ainda não registradas."
    return result
}
