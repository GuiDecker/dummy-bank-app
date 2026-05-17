package com.guilherme.honeypot.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.guilherme.honeypot.data.AppPreferences
import com.guilherme.honeypot.helper.CameraHelper
import com.guilherme.honeypot.helper.LocationHelper
import com.guilherme.honeypot.helper.TelegramNotifier
import com.guilherme.honeypot.receiver.MyDeviceAdminReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EmergencyService : Service(), LifecycleOwner {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    companion object {
        private const val CHANNEL_ID = "emergency_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Serviço ativo")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        scope.launch {
            executeEmergencyProtocol()
        }

        return START_NOT_STICKY
    }

    private suspend fun executeEmergencyProtocol() {
        val prefs = AppPreferences(this)
        val dryRun = prefs.isDryRun()

        Log.d("Honeypot", "Emergency protocol started (dryRun=$dryRun)")

        // Parallel: get location + take photo
        val locationDeferred = scope.async(Dispatchers.IO) {
            LocationHelper(this@EmergencyService).getCurrentLocation()
        }
        val photoDeferred = scope.async(Dispatchers.Main) {
            CameraHelper(this@EmergencyService).captureSilent(this@EmergencyService)
        }

        val location = locationDeferred.await()
        val photo = photoDeferred.await()

        Log.d("Honeypot", "Location: $location, Photo size: ${photo?.size ?: 0}")

        // Send to Telegram
        val token = prefs.getTelegramToken()
        val chatId = prefs.getTelegramChatId()
        if (token.isNotEmpty() && chatId.isNotEmpty()) {
            val notifier = TelegramNotifier(token, chatId)
            notifier.sendAlert(location, photo)
        } else {
            Log.w("Honeypot", "Telegram not configured, skipping notification")
        }

        // Lock device
        if (!dryRun) {
            lockDevice()
        } else {
            Log.d("Honeypot", "DRY RUN: would lock device now")
        }

        stopSelf()
    }

    private fun lockDevice() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                Log.d("Honeypot", "Device locked")
            } else {
                Log.e("Honeypot", "Device Admin not active, cannot lock")
            }
        } catch (e: Exception) {
            Log.e("Honeypot", "Lock failed: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Serviço de Emergência",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            setSound(null, null)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        scope.cancel()
        super.onDestroy()
    }
}
