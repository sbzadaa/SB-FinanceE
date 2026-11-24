package com.sbfinance.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.sbfinance.app.data.FinanceStore
import com.sbfinance.app.reminders.Reminders
import com.sbfinance.app.ui.SBFinanceApp

class MainActivity : ComponentActivity() {
    private lateinit var store: FinanceStore
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val backup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { contentResolver.openOutputStream(it)?.use { out -> out.write(store.exportJson().toByteArray(Charsets.UTF_8)) } }
    }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val result = runCatching { contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                store.restoreJson(reader.readText())
            } }
            restoreMessage = if (result.isSuccess) "Backup restaurado." else "Não foi possível importar: ${result.exceptionOrNull()?.message}"
        }
    }
    private var restoreMessage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = FinanceStore(this)
        Reminders.channel(this)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            SBFinanceApp(store = store, onBackup = { backup.launch("SBFinance-backup.json") },
                onRestore = { restore.launch(arrayOf("application/json", "text/plain")) },
                restoreMessage = restoreMessage, clearMessage = { restoreMessage = "" })
        }
    }
}
