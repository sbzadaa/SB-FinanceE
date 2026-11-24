package com.sbfinance.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.sbfinance.app.data.*
import com.sbfinance.app.logic.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val paddingH = 20.dp

@Composable
fun SBFinanceApp(store: FinanceStore, onBackup: () -> Unit, onRestore: () -> Unit,
                 restoreMessage: String, clearMessage: () -> Unit) {
    SBTheme {
        val data = store.state
        var screen by rememberSaveable { mutableIntStateOf(0) }
        var entryForm by remember { mutableStateOf<Entry?>(null) }
        var entryMode by remember { mutableStateOf<String?>(null) }
        var billForm by remember { mutableStateOf<Bill?>(null) }
        var addBill by remember { mutableStateOf(false) }
        var addGoal by remember { mutableStateOf(false) }
        var editingGoal by remember { mutableStateOf<Goal?>(null) }
        var budgetCat by remember { mutableStateOf<String?>(null) }
        var eraseConfirm by remember { mutableStateOf(false) }
        var deleteEntry by remember { mutableStateOf<Entry?>(null) }
        var deleteBill by remember { mutableStateOf<Bill?>(null) }
        var confirmPay by remember { mutableStateOf<Bill?>(null) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        LaunchedEffect(restoreMessage) {
            if (restoreMessage.isNotBlank()) { snackbar.showSnackbar(restoreMessage); clearMessage() }
        }
        Scaffold(containerColor = CanvasBg, snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (screen in 0..4) NavigationDock(current = screen, onSelect = { screen = it })
            }
        ) { window ->
            Box(Modifier.fillMaxSize().padding(window)) {
                when(screen) {
                    0 -> HomeScreen(data,
                        onAdd = { entryMode = "expense" }, onIncome = { entryMode = "income" },
                        onBills = { screen = 3 }, onGoals = { screen = 7 },
                        onHistory = { screen = 6 }, onSettings = { screen = 5 },
                        onToggle = { store.setHidden(!data.hideBalance) }, onEdit = { entryForm = it })
                    1 -> ChartsScreen(data, onSettings = { screen = 5 })
                    2 -> QuickAddScreen(onSave = { store.addEntry(it); screen = 0 },
                        onManual = { entryMode = it })
                    3 -> BillsScreen(data, onCreate = { addBill = true }, onEdit = { billForm = it },
                        onPay = { confirmPay = it }, onDelete = { deleteBill = it })
                    4 -> AssistantScreen(data, onAdd = { store.addEntry(it) })
                    5 -> SettingsScreen(data, onBack = { screen = 0 }, onBackup = onBackup, onRestore = onRestore,
                        onBudget = { budgetCat = it }, onHour = store::setHour,
                        onErase = { eraseConfirm = true })
                    6 -> HistoryScreen(data, onBack = { screen = 0 }, onEdit = { entryForm = it },
                        onDelete = { deleteEntry = it })
                    7 -> GoalsScreen(data, onBack = { screen = 0 }, onAdd = { addGoal = true },
                        onEdit = { editingGoal = it }, onDelete = store::removeGoal)
                }
            }
        }
        if (entryMode != null || entryForm != null) {
            EntryFormDialog(existing = entryForm, startingKind = entryMode ?: "expense", onDismiss = {
                entryMode = null; entryForm = null
            }, onSave = { entry ->
                if (entryForm != null) store.editEntry(entry) else store.addEntry(entry)
                entryMode = null; entryForm = null
            })
        }
        if (addBill || billForm != null) BillFormDialog(existing = billForm, onDismiss = { addBill = false; billForm = null },
            onSave = { bill ->
                if (billForm != null) store.editBill(bill) else store.addBill(bill)
                addBill = false; billForm = null
            })
        if (addGoal || editingGoal != null) GoalFormDialog(existing = editingGoal, onDismiss = { addGoal = false; editingGoal = null },
            onSave = { goal -> if (editingGoal == null) store.addGoal(goal) else store.changeGoal(goal)
                addGoal = false; editingGoal = null })
        budgetCat?.let { cat -> BudgetDialog(category = cat, value = data.budgets[cat] ?: 0L,
            onDismiss = { budgetCat = null }, onSave = { store.setBudget(cat, it); budgetCat = null }) }
        confirmPay?.let { bill ->
            ConfirmAction("Confirmar pagamento?", "${bill.title} • ${money(bill.cents)}. A cobrança de ${dateBR(bill.nextDate)} será marcada como paga e registrada uma vez nas despesas.",
                "Confirmar", onCancel = { confirmPay = null }, onConfirm = {
                    if (!store.payBill(bill.id)) scope.launch { snackbar.showSnackbar("Essa cobrança já foi registrada.") }
                    confirmPay = null
                }, danger = false)
        }
        if (eraseConfirm) ConfirmAction("Apagar todos os dados?", "Essa ação não pode ser desfeita. Faça um backup antes de continuar.", "Apagar", onCancel = { eraseConfirm = false },
            onConfirm = { store.clearAll(); eraseConfirm = false; screen = 0 })
        deleteEntry?.let { e -> ConfirmAction("Excluir lançamento?", "${e.title} • ${money(e.cents)}. Se for o último pagamento de uma conta, a parcela será reaberta. Pagamentos antigos devem ser desfeitos em ordem inversa.", "Excluir", onCancel = { deleteEntry = null },
            onConfirm = {
                if (!store.deleteEntry(e.id)) scope.launch { snackbar.showSnackbar("Exclua os pagamentos da conta do mais recente para o mais antigo.") }
                deleteEntry = null
            }) }
        deleteBill?.let { b -> ConfirmAction("Excluir conta?", "A conta será removida das pendências, mas pagamentos já registrados continuarão no histórico.", "Excluir", onCancel = { deleteBill = null },
            onConfirm = { store.deleteBill(b.id); deleteBill = null }) }
    }
}

@Composable
private fun NavigationDock(current: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        val buttons = listOf(Triple("Início", Icons.Rounded.Home, 0), Triple("Gráficos", Icons.Rounded.ShowChart, 1),
            Triple("Adicionar", Icons.Rounded.Add, 2), Triple("Fixas", Icons.Rounded.ReceiptLong, 3),
            Triple("Assistente", Icons.Rounded.AutoAwesome, 4))
        buttons.forEach { (label, icon, tab) ->
            Column(Modifier.weight(1f).clickable { onSelect(tab) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(if (tab == 2) 45.dp else 32.dp)
                    .background(if (tab == 2) Purple else Color.Transparent, RoundedCornerShape(if (tab == 2) 16.dp else 9.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = when {
                        tab == 2 -> Color.White
                        current == tab -> Purple
                        else -> Slate
                    }, modifier = Modifier.size(if (tab == 2) 25.dp else 22.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(label, fontSize = 10.sp, color = if (current == tab) Purple else Slate, fontWeight = if (current == tab) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1)
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String = "", onSettings: (() -> Unit)? = null,
                       onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = Ink) }
        else Box(Modifier.size(42.dp).background(Purple, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Text("$" + "B", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            if (subtitle.isNotBlank()) Text(subtitle, color = Slate, fontSize = 12.sp)
        }
        if (onSettings != null) IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Configurações", tint = Ink) }
    }
}

@Composable
private fun HomeScreen(data: FinanceData, onAdd: () -> Unit, onIncome: () -> Unit, onBills: () -> Unit,
                       onGoals: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit,
                       onToggle: () -> Unit, onEdit: (Entry) -> Unit) {
    val month = YearMonth.now()
    val monthly = entriesOfMonth(data.entries, month)
    val active = data.bills.filter { it.active }.sortedBy { it.nextDate }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = paddingH, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeader("SB Finance", "Seu dinheiro sob controle", onSettings) }
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Indigo, Purple, Violet)), RoundedCornerShape(28.dp))
                .padding(23.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Saldo disponível", color = Color.White.copy(alpha = .80f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onToggle, modifier = Modifier.size(29.dp)) { Icon(if (data.hideBalance) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        "Mostrar ou ocultar valores", tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(5.dp))
                MoneyText(balance(data.entries), 33.sp, data.hideBalance, Color.White, FontWeight.ExtraBold)
                Spacer(Modifier.height(17.dp))
                HorizontalDivider(color = Color.White.copy(alpha = .19f))
                Spacer(Modifier.height(15.dp))
                Text("Após próximas contas cadastradas", fontSize = 12.sp, color = Color.White.copy(alpha = .76f))
                Spacer(Modifier.height(4.dp))
                MoneyText(predictedBalance(data), 19.sp, data.hideBalance, Color.White)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PillStat("Entradas do mês", sumIncome(monthly), true, Modifier.weight(1f), data.hideBalance)
                PillStat("Saídas do mês", sumExpense(monthly), false, Modifier.weight(1f), data.hideBalance)
            }
        }
        item {
            Column {
                SectionTitle("Acesso rápido")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CircularAction("Despesa", Icons.Rounded.NorthEast, onAdd)
                    CircularAction("Receita", Icons.Rounded.SouthWest, onIncome)
                    CircularAction("Contas", Icons.Rounded.CalendarMonth, onBills)
                    CircularAction("Metas", Icons.Rounded.Flag, onGoals)
                }
            }
        }
        item {
            ShellCard {
                SectionTitle("Resumo do mês", "${month.monthValue.toString().padStart(2, '0')}/${month.year}")
                Spacer(Modifier.height(8.dp))
                val lastDays = (6 downTo 0).map { days ->
                    val date = LocalDate.now().minusDays(days.toLong()).toString()
                    monthly.filter { it.kind == "expense" && it.date == date }.sumOf { it.cents }.toFloat()
                }
                TrendChart(lastDays)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("7 dias atrás", fontSize = 11.sp, color = Slate)
                    Text("Hoje", fontSize = 11.sp, color = Slate)
                }
                Spacer(Modifier.height(9.dp))
                Text("Gastos registrados por dia · últimos 7 dias", fontSize = 11.sp, color = Slate)
            }
        }
        if (active.isNotEmpty()) item {
            ShellCard {
                SectionTitle("Próximas contas", "Ver todas", onBills)
                active.take(2).forEach { bill ->
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Event, null, tint = Purple, modifier = Modifier.size(23.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(bill.title, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("${dateBR(bill.nextDate)} · ${dueStatus(bill.nextDate)}", color = Slate, fontSize = 11.sp)
                        }
                        MoneyText(bill.cents, 14.sp, data.hideBalance)
                    }
                }
            }
        }
        item {
            Column {
                SectionTitle("Últimas movimentações", "Ver todas", onHistory)
                Spacer(Modifier.height(8.dp))
                ShellCard {
                    if (data.entries.isEmpty()) EmptyNote("Nenhum lançamento ainda", "Adicione sua primeira receita ou despesa para começar.")
                    else data.entries.sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.id })
                        .take(4).forEachIndexed { index, entry ->
                            if (index > 0) HorizontalDivider(color = Hairline)
                            EntryRow(entry, onClick = { onEdit(entry) }, hide = data.hideBalance)
                        }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ChartsScreen(data: FinanceData, onSettings: () -> Unit) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val list = entriesOfMonth(data.entries, selectedMonth)
    val spent = sumExpense(list)
    val byCategory = list.filter { it.kind == "expense" }.groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.cents } }.toList().sortedByDescending { it.second }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader("Gráficos", "Entenda seus hábitos", onSettings) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) { Icon(Icons.Rounded.ChevronLeft, "Mês anterior") }
                Text(selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", ptBR)).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) { Icon(Icons.Rounded.ChevronRight, "Próximo mês") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillStat("Receitas", sumIncome(list), true, Modifier.weight(1f), data.hideBalance)
                PillStat("Despesas", spent, false, Modifier.weight(1f), data.hideBalance)
            }
        }
        item {
            ShellCard {
                SectionTitle("Evolução de despesas", "Diariamente")
                Spacer(Modifier.height(9.dp))
                val days = selectedMonth.lengthOfMonth()
                val daily = (1..days).map { day ->
                    list.filter { it.kind == "expense" && runCatching { LocalDate.parse(it.date).dayOfMonth == day }.getOrDefault(false) }
                        .sumOf { it.cents }.toFloat()
                }
                TrendChart(daily)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dia 1", color = Slate, fontSize = 11.sp)
                    Text("Dia $days", color = Slate, fontSize = 11.sp)
                }
            }
        }
        item {
            ShellCard {
                SectionTitle("Gastos por categoria")
                if (byCategory.isEmpty()) EmptyNote("Sem despesas nesse período", "Os gráficos aparecem conforme seus registros.")
                byCategory.forEach { (cat, value) ->
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(cat, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        MoneyText(value, 13.sp, data.hideBalance)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { if (spent > 0) value.toFloat() / spent else 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                        color = Purple, trackColor = Color(0xFFF1E9FB))
                }
            }
        }
        item {
            ShellCard {
                SectionTitle("Resultado do mês")
                Spacer(Modifier.height(10.dp))
                MoneyText(sumIncome(list) - spent, 26.sp, data.hideBalance,
                    if (sumIncome(list) >= spent) Positive else Negative)
                Text("Receitas menos despesas efetivamente registradas.", color = Slate, fontSize = 12.sp)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuickAddScreen(onSave: (Entry) -> Unit, onManual: (String) -> Unit) {
    var phrase by rememberSaveable { mutableStateOf("") }
    val parsed = remember(phrase) { parseNatural(phrase) }
    var confirmation by remember { mutableStateOf<ParsedEntry?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeader("Adicionar", "Anote em segundos") }
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Indigo, Purple)), RoundedCornerShape(26.dp)).padding(23.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFFE2C8FF))
                Spacer(Modifier.height(12.dp))
                Text("Escreva como você fala", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                Spacer(Modifier.height(9.dp))
                Text("Exemplo: Gastei 5 reais com pastel", fontSize = 14.sp, color = Color.White.copy(alpha = .85f))
                Spacer(Modifier.height(5.dp))
                Text("Também entende: Recebi 2500 de salário", fontSize = 13.sp, color = Color.White.copy(alpha = .75f))
            }
        }
        item {
            ShellCard {
                OutlinedTextField(value = phrase, onValueChange = { phrase = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("O que aconteceu?") }, minLines = 3, maxLines = 5,
                    placeholder = { Text("Gastei 18,90 em um lanche...") }, shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(14.dp))
                if (parsed != null) {
                    Text("Prévia do lançamento", fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(7.dp))
                    Text("${parsed.title} • ${parsed.category}", color = Slate, fontSize = 13.sp)
                    MoneyText(parsed.cents, 23.sp)
                    Spacer(Modifier.height(12.dp))
                } else if (phrase.isNotBlank()) {
                    Text("Informe um valor explícito, como '25,50 reais'.", fontSize = 12.sp, color = Negative)
                    Spacer(Modifier.height(9.dp))
                }
                PurpleButton("Revisar lançamento", enabled = parsed != null, onClick = { confirmation = parsed })
                Spacer(Modifier.height(8.dp))
                Text("A interpretação é local. Você confirma antes de salvar.", color = Slate, fontSize = 11.sp)
            }
        }
        item {
            SectionTitle("Ou escolha como lançar")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onManual("expense") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.NorthEast, null); Spacer(Modifier.width(5.dp)); Text("Despesa")
                }
                OutlinedButton(onClick = { onManual("income") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.SouthWest, null); Spacer(Modifier.width(5.dp)); Text("Receita")
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    confirmation?.let { item -> AlertDialog(onDismissRequest = { confirmation = null },
        icon = { Icon(Icons.Rounded.CheckCircle, null, tint = Purple) },
        title = { Text("Confirmar lançamento") },
        text = { Column {
            Text(item.title, fontWeight = FontWeight.Bold)
            Text("${item.category} · ${if (item.kind == "income") "Receita" else "Despesa"}", color = Slate)
            Spacer(Modifier.height(8.dp)); MoneyText(item.cents, 24.sp)
        } },
        confirmButton = { TextButton(onClick = {
            onSave(Entry(title = item.title, cents = item.cents, kind = item.kind, category = item.category))
            phrase = ""; confirmation = null
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Corrigir") } }) }
}

@Composable
private fun BillsScreen(data: FinanceData, onCreate: () -> Unit, onEdit: (Bill) -> Unit,
                        onPay: (Bill) -> Unit, onDelete: (Bill) -> Unit) {
    var viewCompleted by remember { mutableStateOf(false) }
    val bills = data.bills.filter { it.active != viewCompleted }.sortedBy { it.nextDate }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("Contas e parcelas", "Organize seus vencimentos") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillStat("Pendentes", data.bills.filter { it.active }.sumOf { it.cents }, false, Modifier.weight(1f), data.hideBalance)
                PillStat("Já pagas", data.entries.filter { it.billId.isNotBlank() }.sumOf { it.cents }, true, Modifier.weight(1f), data.hideBalance)
            }
        }
        item { PurpleButton("+ Nova conta ou parcela", onCreate) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !viewCompleted, onClick = { viewCompleted = false }, label = { Text("Pendentes") })
                FilterChip(selected = viewCompleted, onClick = { viewCompleted = true }, label = { Text("Concluídas") })
            }
        }
        if (bills.isEmpty()) item {
            ShellCard { EmptyNote(if (viewCompleted) "Nenhuma conta concluída" else "Contas em dia por aqui", "Cadastre despesas fixas, assinaturas ou compras parceladas.") }
        }
        items(bills, key = { it.id }) { bill ->
            ShellCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(47.dp).background(Color(0xFFF1E7FF), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                        Icon(if (bill.installmentCount > 0) Icons.Rounded.CreditCard else Icons.Rounded.EventRepeat, null, tint = Purple)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(bill.title, color = Ink, fontWeight = FontWeight.Bold)
                        Text(if (bill.installmentCount == 0) "Mensal recorrente" else "${bill.completedCount}/${bill.installmentCount} parcelas pagas", color = Slate, fontSize = 12.sp)
                    }
                    MoneyText(bill.cents, 16.sp, data.hideBalance)
                }
                Spacer(Modifier.height(12.dp))
                Text(if (bill.active) "${dueStatus(bill.nextDate)} · ${dateBR(bill.nextDate)}" else "Todas as parcelas concluídas",
                    color = if (bill.active && runCatching { LocalDate.parse(bill.nextDate).isBefore(LocalDate.now()) }.getOrDefault(false)) Negative else Slate,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (bill.installmentCount > 0) {
                    Spacer(Modifier.height(9.dp))
                    LinearProgressIndicator(progress = { bill.completedCount.toFloat() / bill.installmentCount.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                        color = Positive, trackColor = Hairline)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (bill.active) Button(onClick = { onPay(bill) }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Marcar paga")
                    }
                    OutlinedIconButton(onClick = { onEdit(bill) }) { Icon(Icons.Rounded.Edit, "Editar conta", tint = Purple) }
                    OutlinedIconButton(onClick = { onDelete(bill) }) { Icon(Icons.Rounded.DeleteOutline, "Excluir conta", tint = Negative) }
                }
            }
        }
        item {
            ShellCard {
                Text("Como funciona?", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text("A conta entra nas despesas somente quando você marcar o pagamento. O próximo vencimento é atualizado automaticamente; contas vencidas continuam pendentes.",
                    fontSize = 12.sp, color = Slate)
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun AssistantScreen(data: FinanceData, onAdd: (Entry) -> Unit) {
    var phrase by rememberSaveable { mutableStateOf("") }
    var confirming by remember { mutableStateOf<ParsedEntry?>(null) }
    val insights = remember(data) { summaryInsights(data) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader("Assistente", "Análise financeira inteligente") }
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Indigo, Purple)), RoundedCornerShape(27.dp)).padding(23.dp)) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(11.dp))
                Text("Seu dinheiro, mais claro.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 23.sp)
                Spacer(Modifier.height(5.dp))
                Text("Sugestões automáticas com base nos seus lançamentos, sem enviar informações para a internet.",
                    color = Color.White.copy(alpha = .82f), fontSize = 13.sp)
            }
        }
        item { SectionTitle("Seu diagnóstico") }
        items(insights) { advice ->
            ShellCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = Purple, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(11.dp))
                    Text(advice, fontSize = 14.sp, color = Ink)
                }
            }
        }
        item {
            ShellCard {
                Text("Registre por mensagem", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phrase, onValueChange = { phrase = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Gastei 35 com gasolina") }, maxLines = 3, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(10.dp))
                PurpleButton("Analisar texto", onClick = { confirming = parseNatural(phrase) }, enabled = parseNatural(phrase) != null)
                Spacer(Modifier.height(7.dp))
                Text("Este assistente usa regras locais. Não é uma IA generativa nem substitui aconselhamento financeiro.",
                    fontSize = 11.sp, color = Slate)
            }
        }
        item { Spacer(Modifier.height(26.dp)) }
    }
    confirming?.let { parsed -> AlertDialog(onDismissRequest = { confirming = null },
        title = { Text("Registrar movimentação?") },
        text = { Column { Text(parsed.title); Text(parsed.category, color = Slate); MoneyText(parsed.cents, 22.sp) } },
        confirmButton = { TextButton(onClick = {
            onAdd(Entry(title = parsed.title, cents = parsed.cents, kind = parsed.kind, category = parsed.category))
            phrase = ""; confirming = null
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { confirming = null }) { Text("Cancelar") } }) }
}

@Composable
private fun HistoryScreen(data: FinanceData, onBack: () -> Unit, onEdit: (Entry) -> Unit, onDelete: (Entry) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("all") }
    val filtered = data.entries.filter { e ->
        (kind == "all" || e.kind == kind) && (search.isBlank() ||
            e.title.contains(search, ignoreCase = true) || e.category.contains(search, ignoreCase = true))
    }.sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.id })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Histórico", "Todas as movimentações", onBack = onBack) }
        item { Field("Buscar título ou categoria", search, { search = it }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(Triple("Todos", "all", 0), Triple("Receitas", "income", 1), Triple("Despesas", "expense", 2)).forEach { (label, value, _) ->
                    FilterChip(selected = kind == value, onClick = { kind = value }, label = { Text(label) })
                    Spacer(Modifier.width(2.dp))
                }
            }
        }
        if (filtered.isEmpty()) item { ShellCard { EmptyNote("Sem resultados", "Tente ajustar a pesquisa ou registre um novo lançamento.") } }
        items(filtered, key = { it.id }) { entry ->
            ShellCard {
                EntryRow(entry, onClick = { onEdit(entry) }, hide = data.hideBalance)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEdit(entry) }) { Text("Editar") }
                    TextButton(onClick = { onDelete(entry) }) { Text("Excluir", color = Negative) }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun GoalsScreen(data: FinanceData, onBack: () -> Unit, onAdd: () -> Unit,
                        onEdit: (Goal) -> Unit, onDelete: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { PageHeader("Minhas metas", "Planeje seus objetivos", onBack = onBack) }
        item { PurpleButton("+ Nova meta", onAdd) }
        if (data.goals.isEmpty()) item { ShellCard { EmptyNote("Nenhuma meta cadastrada", "Crie uma meta para acompanhar suas reservas.") } }
        items(data.goals, key = { it.id }) { goal ->
            ShellCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${((goal.saved.toDouble() / goal.target.coerceAtLeast(1)) * 100).toInt().coerceAtMost(100)}%", fontWeight = FontWeight.Bold, color = Purple)
                }
                Spacer(Modifier.height(10.dp))
                MoneyText(goal.saved, 21.sp, data.hideBalance)
                Text("de ${money(goal.target)}", color = Slate, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { (goal.saved.toFloat() / goal.target.coerceAtLeast(1)).coerceIn(0f,1f) },
                    modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(10.dp)), color = Purple)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEdit(goal) }) { Text("Atualizar") }
                    TextButton(onClick = { onDelete(goal.id) }) { Text("Excluir", color = Negative) }
                }
            }
        }
        item {
            Text("O valor reservado é apenas um acompanhamento de meta; não é descontado automaticamente do saldo. Registre a transferência real como movimentação quando necessário.",
                color = Slate, fontSize = 12.sp)
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingsScreen(data: FinanceData, onBack: () -> Unit, onBackup: () -> Unit,
                           onRestore: () -> Unit, onBudget: (String) -> Unit, onHour: (Int) -> Unit,
                           onErase: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = paddingH), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("Configurações", "Personalize seu controle", onBack = onBack) }
        item {
            ShellCard {
                SectionTitle("Orçamentos mensais")
                Text("Defina um teto para cada categoria de despesas.", fontSize = 12.sp, color = Slate)
                categories.forEach { cat ->
                    Row(Modifier.fillMaxWidth().clickable { onBudget(cat) }.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(cat, fontSize = 14.sp)
                        Text(data.budgets[cat]?.let { money(it) } ?: "Definir", color = Purple, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            ShellCard {
                SectionTitle("Lembretes")
                Text("Hora para avisos de 3 dias, 1 dia, vencimento e atraso.", color = Slate, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onHour((data.notifyHour + 23) % 24) }) { Icon(Icons.Rounded.Remove, "Diminuir hora") }
                    Text("${data.notifyHour.toString().padStart(2, '0')}:00", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onHour((data.notifyHour + 1) % 24) }) { Icon(Icons.Rounded.Add, "Aumentar hora") }
                }
                Text("É necessário permitir notificações. O Android pode atrasar lembretes para economizar bateria.", color = Slate, fontSize = 11.sp)
            }
        }
        item {
            ShellCard {
                SectionTitle("Seus dados")
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FileDownload, null); Spacer(Modifier.width(7.dp)); Text("Exportar backup JSON")
                }
                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FileUpload, null); Spacer(Modifier.width(7.dp)); Text("Restaurar backup JSON")
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onErase) { Text("Apagar meus dados", color = Negative) }
                Text("Dados armazenados somente neste aparelho, sem sincronização bancária ou nuvem.", color = Slate, fontSize = 11.sp)
            }
        }
        item { Text("SB Finance 3.0 • Kotlin + Jetpack Compose", modifier = Modifier.fillMaxWidth().padding(20.dp),
            textAlign = TextAlign.Center, color = Slate, fontSize = 11.sp) }
        item { Spacer(Modifier.height(22.dp)) }
    }
}

@Composable
private fun EntryFormDialog(existing: Entry?, startingKind: String, onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var amount by remember(existing?.id) { mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.cents / 100.0).replace('.', ',') } ?: "") }
    var kind by remember(existing?.id, startingKind) { mutableStateOf(existing?.kind ?: startingKind) }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: "Outros") }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: today()) }
    val valid = title.isNotBlank() && amountToCents(amount) != null && runCatching { LocalDate.parse(date) }.isSuccess
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Novo lançamento" else "Editar lançamento") },
        text = {
            Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                if (existing?.billId?.isNotBlank() == true) {
                    Text("Pagamento de conta vinculada • mantenha como despesa", color = Slate, fontSize = 12.sp)
                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "expense", onClick = { kind = "expense" }, label = { Text("Despesa") })
                    FilterChip(selected = kind == "income", onClick = { kind = "income" }, label = { Text("Receita") })
                }
                Field("Descrição", title, { title = it }, placeholder = "Ex.: Mercado")
                Field("Valor em R$", amount, { amount = it }, numeric = true, placeholder = "25,90")
                if (kind == "expense") CategorySelect(category) { category = it }
                Field("Data (AAAA-MM-DD)", date, { date = it }, placeholder = today())
                if (!valid) Text("Informe descrição, valor positivo e uma data válida.", color = Negative, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = {
            onSave(Entry(id = existing?.id ?: uid(), title = title.trim(), cents = amountToCents(amount)!!,
                kind = kind, category = if (kind == "income") "Outros" else category, date = date,
                billId = existing?.billId ?: "", billPeriod = existing?.billPeriod ?: ""))
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun BillFormDialog(existing: Bill?, onDismiss: () -> Unit, onSave: (Bill) -> Unit) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var amount by remember(existing?.id) { mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.cents / 100.0).replace('.', ',') } ?: "") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: "Contas") }
    var nextDate by remember(existing?.id) { mutableStateOf(existing?.nextDate ?: LocalDate.now().plusDays(7).toString()) }
    var installments by remember(existing?.id) { mutableStateOf((existing?.installmentCount ?: 0).toString()) }
    var recurring by remember(existing?.id) { mutableStateOf((existing?.installmentCount ?: 0) == 0) }
    val total = installments.toIntOrNull()
    val valid = title.isNotBlank() && amountToCents(amount) != null && runCatching { LocalDate.parse(nextDate) }.isSuccess &&
        (recurring || (total != null && total in 1..360 && total >= (existing?.completedCount ?: 0)))
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Nova conta" else "Editar conta") },
        text = {
            Column(Modifier.heightIn(max = 490.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Field("Nome", title, { title = it }, placeholder = "Ex.: Internet")
                Field("Valor por pagamento (R$)", amount, { amount = it }, numeric = true, placeholder = "99,90")
                CategorySelect(category) { category = it }
                Field("Próximo vencimento (AAAA-MM-DD)", nextDate, { nextDate = it })
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    FilterChip(selected = recurring, onClick = { recurring = true }, label = { Text("Mensal") })
                    FilterChip(selected = !recurring, onClick = { recurring = false }, label = { Text("Parcelada") })
                }
                if (!recurring) Field("Número total de parcelas", installments, { installments = it }, numeric = true, placeholder = "10")
                if (existing != null) Text("Parcelas pagas: ${existing.completedCount}. Editar não apaga o histórico.", fontSize = 12.sp, color = Slate)
                Text("O valor só entra nas despesas ao marcar o pagamento.", fontSize = 12.sp, color = Slate)
                if (!valid) Text("Verifique o título, valor, data e número de parcelas.", color = Negative, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = {
            onSave(Bill(id = existing?.id ?: uid(), title = title.trim(), cents = amountToCents(amount)!!,
                category = category, nextDate = nextDate, installmentCount = if (recurring) 0 else (total ?: 0),
                completedCount = existing?.completedCount ?: 0,
                active = (recurring || (total ?: 0) > (existing?.completedCount ?: 0)),
                dueDay = LocalDate.parse(nextDate).dayOfMonth))
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun GoalFormDialog(existing: Goal?, onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var target by remember(existing?.id) { mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.target / 100.0).replace('.', ',') } ?: "") }
    var saved by remember(existing?.id) { mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.saved / 100.0).replace('.', ',') } ?: "0,00") }
    val targetCents = amountToCents(target)
    val savedCents = if (saved.replace("0", "").replace(",", "").replace(".", "").isBlank()) 0L else amountToCents(saved)
    val valid = title.isNotBlank() && targetCents != null && savedCents != null
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Nova meta" else "Atualizar meta") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Nome da meta", title, { title = it }, placeholder = "Ex.: Reserva de emergência")
            Field("Objetivo em R$", target, { target = it }, numeric = true)
            Field("Já reservado em R$", saved, { saved = it }, numeric = true)
            Text("A reserva é um acompanhamento e não altera automaticamente o saldo.", color = Slate, fontSize = 12.sp)
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(Goal(existing?.id ?: uid(), title.trim(), targetCents!!, savedCents!!)) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun BudgetDialog(category: String, value: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var amount by remember(category) { mutableStateOf(if (value > 0) String.format(java.util.Locale.US, "%.2f", value / 100.0).replace('.', ',') else "") }
    val cents = if (amount.isBlank() || amount == "0" || amount == "0,00") 0L else amountToCents(amount)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Orçamento: $category") },
        text = { Column { Field("Limite mensal em R$", amount, { amount = it }, numeric = true, placeholder = "500,00")
            Text("Deixe vazio para remover o limite.", fontSize = 12.sp, color = Slate) } },
        confirmButton = { TextButton(enabled = cents != null, onClick = { onSave(cents!!) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun ConfirmAction(title: String, message: String, action: String, onCancel: () -> Unit, onConfirm: () -> Unit, danger: Boolean = true) {
    AlertDialog(onDismissRequest = onCancel, title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(action, color = if (danger) Negative else Purple) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } })
}
