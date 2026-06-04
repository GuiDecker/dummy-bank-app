package com.guilherme.honeypot.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.pm.PackageManager
import android.os.Build
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.guilherme.honeypot.BuildConfig
import com.guilherme.honeypot.data.AppPreferences
import com.guilherme.honeypot.receiver.MyDeviceAdminReceiver
import kotlinx.coroutines.launch

class SetupActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val hasForegroundLocation =
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                    hasAnyLocationPermission()

            if (hasForegroundLocation) {
                requestBackgroundLocationPermission()
            }
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(
                    this,
                    "Permita localizacao em segundo plano para o protocolo completo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    companion object {
        private const val REQUEST_DEVICE_ADMIN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)
        requestRequiredPermissions()

        setContent {
            SetupScreen(
                onActivateAdmin = { requestDeviceAdmin() },
                onSave = { adminPin, token, chatId -> saveSetup(adminPin, token, chatId) }
            )
        }
    }

    private fun requestDeviceAdmin() {
        val componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Necessário para travar o dispositivo em caso de emergência"
            )
        }
        startActivityForResult(intent, REQUEST_DEVICE_ADMIN)
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filterNot { hasPermission(it) }

        if (permissions.isNotEmpty()) {
            permissionsLauncher.launch(permissions.toTypedArray())
        } else {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (!hasAnyLocationPermission()) return
        if (hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) return

        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun hasAnyLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun saveSetup(adminPin: String, token: String, chatId: String) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            if (adminPin.length < 6) {
                Toast.makeText(this@SetupActivity, "PIN admin deve ter ao menos 6 dígitos", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Use BuildConfig values as fallback
            val finalToken = token.ifEmpty { BuildConfig.TELEGRAM_TOKEN }
            val finalChatId = chatId.ifEmpty { BuildConfig.TELEGRAM_CHAT_ID }

            if (finalToken.isEmpty() || finalChatId.isEmpty()) {
                Toast.makeText(
                    this@SetupActivity,
                    "Configure o token e o chat ID do Telegram",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            prefs.saveAdminPin(adminPin)
            prefs.saveTelegramConfig(finalToken, finalChatId)
            prefs.setSetupComplete()

            Toast.makeText(this@SetupActivity, "Configuração salva!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@SetupActivity, MainActivity::class.java))
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEVICE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Device Admin ativado!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Device Admin necessário para funcionar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun SetupScreen(
    onActivateAdmin: () -> Unit,
    onSave: (adminPin: String, token: String, chatId: String) -> Unit
) {
    var adminPin by remember { mutableStateOf("") }
    var telegramToken by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Configuração Inicial",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Admin PIN (master) — abre as Configurações
        OutlinedTextField(
            value = adminPin,
            onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) adminPin = it },
            label = { Text("PIN admin (6+ dígitos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Telegram config
        OutlinedTextField(
            value = telegramToken,
            onValueChange = { telegramToken = it },
            label = { Text("Telegram Bot Token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = chatId,
            onValueChange = { chatId = it },
            label = { Text("Telegram Chat ID") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Activate Device Admin button
        Button(
            onClick = onActivateAdmin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Ativar Device Admin", color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save button
        Button(
            onClick = {
                if (adminPin.length >= 6) {
                    onSave(adminPin, telegramToken, chatId)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = adminPin.length >= 6,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Salvar e Iniciar", color = Color.White)
        }
    }
}
