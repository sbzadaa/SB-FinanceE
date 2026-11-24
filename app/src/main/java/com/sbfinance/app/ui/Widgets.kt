package com.sbfinance.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.sbfinance.app.data.Entry
import com.sbfinance.app.logic.*
import java.time.LocalDate
import kotlin.math.max

@Composable
fun SectionTitle(title: String, suffix: String = "", onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        if (onClick != null) TextButton(onClick) { Text(suffix, color = Purple, fontWeight = FontWeight.SemiBold) }
        else if (suffix.isNotBlank()) Text(suffix, fontSize = 12.sp, color = Slate)
    }
}

@Composable
fun ShellCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(23.dp))
        .border(1.dp, Hairline, RoundedCornerShape(23.dp)).padding(18.dp), content = content)
}

@Composable
fun MoneyText(cents: Long, fontSize: TextUnit = 17.sp, hide: Boolean = false, color: Color = Ink, weight: FontWeight = FontWeight.Bold) =
    Text(if (hide) "R$ •••••" else money(cents), fontSize = fontSize, fontWeight = weight, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)

@Composable
fun EntryRow(entry: Entry, onClick: (() -> Unit)? = null, hide: Boolean = false) {
    val isIncome = entry.kind == "income"
    Row(Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).background(if (isIncome) Color(0xFFE0F7EB) else Color(0xFFF2EAFE), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            Icon(if (isIncome) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast, null, tint = if (isIncome) Positive else Purple, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${entry.category} • ${dateBR(entry.date)}", fontSize = 12.sp, color = Slate)
        }
        Text(if (hide) "••••" else (if (isIncome) "+" else "−") + money(entry.cents), color = if (isIncome) Positive else Ink,
            fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun PillStat(label: String, cents: Long, positive: Boolean, modifier: Modifier = Modifier, hide: Boolean = false) {
    Column(modifier.background(if (positive) Color(0xFFF0FFF8) else Color(0xFFFFF4F5), RoundedCornerShape(18.dp)).padding(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(if (positive) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast, null, tint = if (positive) Positive else Negative, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 12.sp, color = Slate)
        }
        Spacer(Modifier.height(5.dp))
        MoneyText(cents, fontSize = 17.sp, hide = hide)
    }
}

@Composable
fun CircularAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(76.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(57.dp).background(Color.White, RoundedCornerShape(20.dp)).border(1.dp, Hairline, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Purple, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 12.sp, color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun TrendChart(points: List<Float>, modifier: Modifier = Modifier, color: Color = Purple) {
    val safe = if (points.isEmpty()) listOf(0f, 0f) else if (points.size == 1) listOf(points[0], points[0]) else points
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val top = 16.dp.toPx(); val bottom = size.height - 15.dp.toPx()
        val highest = max(1f, safe.maxOrNull() ?: 1f)
        val smallest = minOf(0f, safe.minOrNull() ?: 0f)
        val span = (highest - smallest).coerceAtLeast(1f)
        val coords = safe.mapIndexed { idx, value ->
            androidx.compose.ui.geometry.Offset(idx.toFloat() / (safe.size - 1) * size.width,
                bottom - ((value - smallest) / span) * (bottom - top))
        }
        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3
            drawLine(Hairline, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val fill = Path().apply {
            moveTo(coords.first().x, bottom)
            lineTo(coords.first().x, coords.first().y)
            coords.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(coords.last().x, bottom); close()
        }
        drawPath(fill, brush = Brush.verticalGradient(listOf(color.copy(alpha = .18f), color.copy(alpha = .015f))))
        val line = Path().apply { moveTo(coords.first().x, coords.first().y); coords.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(line, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, 5.dp.toPx(), coords.last())
        drawCircle(Color.White, 2.dp.toPx(), coords.last())
    }
}

@Composable
fun EmptyNote(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(23.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = Lilac, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(9.dp))
        Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(subtitle, color = Slate, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, numeric: Boolean = false, placeholder: String = "") {
    OutlinedTextField(value, onChange, modifier = modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) },
        singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text))
}

@Composable
fun CategorySelect(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Category, null); Spacer(Modifier.width(8.dp)); Text("Categoria: $selected", modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.KeyboardArrowDown, null)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { onSelect(cat); expanded = false }) }
        }
    }
}

@Composable
fun PurpleButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(51.dp),
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
