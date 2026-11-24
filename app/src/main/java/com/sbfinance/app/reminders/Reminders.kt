package com.sbfinance.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sbfinance.app.MainActivity
import com.sbfinance.app.data.Bill
import com.sbfinance.app.data.FinanceStore
import java.time.LocalDate
import java.time.ZoneId

object Reminders {
    const val CHANNEL = "sbfinance_contas"
    private val offsets = listOf(-3, -1, 0, 1)

    private fun intent(context: Context, id: String, offset: Int): PendingIntent {
        val action = "com.sbfinance.app.BILL.$id.$offset"
        val i = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action; putExtra("bill_id", id); putExtra("offset", offset)
        }
        return PendingIntent.getBroadcast(context, action.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun cancel(context: Context, id: String) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        offsets.forEach { offset -> alarms.cancel(intent(context, id, offset)) }
    }
    fun scheduleAll(context: Context, bills: List<Bill>, hour: Int) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (bill in bills) {
            cancel(context, bill.id)
            if (!bill.active) continue
            val date = runCatching { LocalDate.parse(bill.nextDate) }.getOrNull() ?: continue
            for (offset in offsets) {
                val instant = date.plusDays(offset.toLong()).atTime(hour.coerceIn(0,23), 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (instant <= System.currentTimeMillis()) continue
                // Não exige permissão especial de alarme exato; o Android pode adiar.
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instant, intent(context, bill.id, offset))
            }
        }
    }
    fun channel(context: Context) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(NotificationChannel(CHANNEL, "Lembretes de contas", NotificationManager.IMPORTANCE_DEFAULT))
    }
}

class ReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("bill_id") ?: return
        val offset = intent.getIntExtra("offset", 0)
        val store = FinanceStore(context)
        val bill = store.state.bills.firstOrNull { it.id == id && it.active } ?: return
        // Alarme atrasado de um período já pago nunca deve gerar alerta.
        val date = runCatching { LocalDate.parse(bill.nextDate) }.getOrNull() ?: return
        if (date.plusDays(offset.toLong()).isAfter(LocalDate.now())) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        Reminders.channel(context)
        val title = when(offset) {
            -3 -> "Vence em 3 dias"
            -1 -> "Vence amanhã"
            0 -> "Conta vence hoje"
            else -> "Conta em atraso"
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, Reminders.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title)
            .setContentText(bill.title + " • Abra o SB Finance para conferir e marcar como paga.")
            .setAutoCancel(true).setContentIntent(open).build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((id + offset).hashCode(), notification)
    }
}

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            FinanceStore(context) // A inicialização restabelece alarmes pendentes.
        }
    }
}
