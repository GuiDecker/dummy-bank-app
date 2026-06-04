package com.guilherme.honeypot.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guilherme.honeypot.data.AppPreferences
import com.guilherme.honeypot.data.PendingAlert
import com.guilherme.honeypot.helper.TelegramNotifier
import java.io.File

class TelegramDispatchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        val token = prefs.getTelegramToken()
        val chatId = prefs.getTelegramChatId()

        if (token.isEmpty() || chatId.isEmpty()) {
            Log.e("Honeypot", "Telegram credentials missing — cannot dispatch alerts")
            return Result.failure()
        }

        val notifier = TelegramNotifier(token, chatId)
        val pending = PendingAlert.listAll(applicationContext)

        if (pending.isEmpty()) {
            Log.d("Honeypot", "No pending alerts to dispatch")
            return Result.success()
        }

        Log.d("Honeypot", "Dispatching ${pending.size} pending alert(s)")
        var anyFailed = false

        for (alert in pending) {
            val photoBytes = alert.photoPath
                ?.let { path -> runCatching { File(path).readBytes() }.getOrNull() }

            val ok = notifier.sendAlert(
                latitude = alert.latitude,
                longitude = alert.longitude,
                timestampMillis = alert.timestamp,
                photo = photoBytes
            )

            if (ok) {
                PendingAlert.delete(applicationContext, alert)
            } else {
                Log.w("Honeypot", "Alert ${alert.id} failed to send, will retry")
                anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}
