package com.guilherme.honeypot.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.guilherme.honeypot.BuildConfig
import com.guilherme.honeypot.R
import com.guilherme.honeypot.data.AppPreferences
import com.guilherme.honeypot.data.PendingAlert
import com.guilherme.honeypot.helper.CameraHelper
import com.guilherme.honeypot.helper.LocationHelper
import com.guilherme.honeypot.receiver.MyDeviceAdminReceiver
import com.guilherme.honeypot.work.AlertScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EmergencyService : Service(), LifecycleOwner {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private var mediaPlayer: MediaPlayer? = null
    private var originalAlarmVolume: Int? = null
    private var originalMusicVolume: Int? = null

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

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            getForegroundServiceTypes()
        )

        scope.launch {
            executeEmergencyProtocol()
        }

        return START_NOT_STICKY
    }

    private suspend fun executeEmergencyProtocol() {
        val prefs = AppPreferences(this)
        val dryRun = prefs.isDryRun()
        val hasLocationPermission = hasLocationPermission()
        val hasCameraPermission = hasCameraPermission()

        Log.d("Honeypot", "Emergency protocol started (dryRun=$dryRun)")
        playAlarm()

        // Parallel: get location + take photo
        val locationDeferred = scope.async(Dispatchers.IO) {
            if (hasLocationPermission) {
                LocationHelper(this@EmergencyService).getCurrentLocation()
            } else {
                Log.d("Honeypot", "Location permission not granted, skipping location")
                null
            }
        }
        val photoDeferred = scope.async(Dispatchers.Main) {
            if (hasCameraPermission) {
                CameraHelper(this@EmergencyService).captureSilent(this@EmergencyService)
            } else {
                Log.d("Honeypot", "Camera permission not granted, skipping camera")
                null
            }
        }

        val location = locationDeferred.await()
        val photoFile = photoDeferred.await()

        Log.d("Honeypot", "Location: $location, Photo: ${photoFile?.absolutePath ?: "none"}")

        // Persist alert + enqueue resilient dispatch (survives lockNow and process death)
        PendingAlert.persist(
            context = this@EmergencyService,
            timestamp = System.currentTimeMillis(),
            latitude = location?.latitude,
            longitude = location?.longitude,
            photoFile = photoFile
        )
        AlertScheduler.enqueueImmediate(this@EmergencyService)

        delay(5000)

        // Lock device after the alarm window
        if (!dryRun) {
            lockDevice()
        } else {
            Log.d("Honeypot", "Dry-run mode: skipping device lock")
            Toast.makeText(this, "PIN incorreto - alarme de teste disparado", Toast.LENGTH_SHORT).show()
        }

        stopSelf()
    }

    private fun getForegroundServiceTypes(): Int {
        var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK

        if (hasCameraPermission()) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        }
        if (hasLocationPermission()) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }

        return types
    }

    private fun hasCameraPermission(): Boolean {
        return hasPermission(Manifest.permission.CAMERA)
    }

    private fun hasLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun playAlarm() {
        try {
            maximizeAlarmVolume()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.ai_meu_messi)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                start()
            }

            if (mediaPlayer == null) {
                Log.w("Honeypot", "Custom alarm resource could not be loaded")
            } else {
                Log.d("Honeypot", "Alarm started")
            }
        } catch (e: Exception) {
            Log.e("Honeypot", "Alarm failed: ${e.message}")
        }
    }

    private fun maximizeAlarmVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        if (originalAlarmVolume == null) {
            originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        if (originalMusicVolume == null) {
            originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }

        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
    }

    private fun restoreAudioVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        originalAlarmVolume?.let {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0)
        }
        originalMusicVolume?.let {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0)
        }
        originalAlarmVolume = null
        originalMusicVolume = null
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
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        restoreAudioVolume()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        scope.cancel()
        super.onDestroy()
    }
}
