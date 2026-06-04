package com.guilherme.honeypot.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guilherme.honeypot.data.AppPreferences
import com.guilherme.honeypot.helper.TelegramNotifier
import com.guilherme.honeypot.ui.theme.HoneypotTheme
import com.guilherme.honeypot.ui.theme.InterFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        setContent {
            HoneypotTheme {
                var unlocked by remember { mutableStateOf(false) }

                if (!unlocked) {
                    AdminGateScreen(
                        verify = { pin -> prefs.verifyAdminPin(pin) },
                        onUnlock = { unlocked = true },
                        onCancel = { finish() }
                    )
                } else {
                    SettingsScreen(
                        prefs = prefs,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminGateScreen(
    verify: suspend (String) -> Boolean,
    onUnlock: () -> Unit,
    onCancel: () -> Unit
) {
    val nuPurple = Color(0xFF820AD1)
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Acesso restrito",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = InterFamily
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Digite o PIN admin para abrir as configurações",
                fontSize = 14.sp,
                color = Color.Gray,
                fontFamily = InterFamily
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 12 && it.all { c -> c.isDigit() }) {
                        pin = it
                        error = ""
                    }
                },
                label = { Text("PIN admin") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = error.isNotEmpty()
            )

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (verify(pin)) {
                            onUnlock()
                        } else {
                            error = "PIN incorreto"
                            pin = ""
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = pin.length >= 6,
                colors = ButtonDefaults.buttonColors(containerColor = nuPurple),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Entrar", color = Color.White, fontFamily = InterFamily)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Cancelar", fontFamily = InterFamily)
            }
        }
    }
}

@Composable
private fun SettingsScreen(prefs: AppPreferences, onClose: () -> Unit) {
    val nuPurple = Color(0xFF820AD1)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newPin by remember { mutableStateOf("") }
    var newAdminPin by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    // Load current values
    LaunchedEffect(Unit) {
        token = prefs.getTelegramToken()
        chatId = prefs.getTelegramChatId()
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configurações",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = InterFamily,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onClose) {
                    Text("Fechar", fontFamily = InterFamily)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PINs
            SectionTitle("Senhas")
            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                label = { Text("Novo PIN normal (4 dígitos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Deixe em branco para manter o PIN atual",
                fontSize = 12.sp,
                color = Color.Gray,
                fontFamily = InterFamily
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newAdminPin,
                onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) newAdminPin = it },
                label = { Text("Novo PIN admin (6+ dígitos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Deixe em branco para manter o PIN admin atual",
                fontSize = 12.sp,
                color = Color.Gray,
                fontFamily = InterFamily
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Telegram
            SectionTitle("Telegram")
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Bot Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = chatId,
                onValueChange = { chatId = it },
                label = { Text("Chat ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            TelegramNotifier(token, chatId)
                                .sendAlert(null, null, System.currentTimeMillis(), null)
                        }
                        Toast.makeText(
                            context,
                            if (ok) "Mensagem de teste enviada!" else "Falha ao enviar — verifique token/chatId",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = token.isNotEmpty() && chatId.isNotEmpty()
            ) {
                Text("Enviar mensagem de teste", fontFamily = InterFamily)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save
            Button(
                onClick = {
                    scope.launch {
                        var changed = false

                        if (newPin.isNotEmpty()) {
                            if (newPin.length != 4) {
                                Toast.makeText(context, "PIN normal deve ter 4 dígitos", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            prefs.savePin(newPin)
                            changed = true
                        }

                        if (newAdminPin.isNotEmpty()) {
                            if (newAdminPin.length < 6) {
                                Toast.makeText(context, "PIN admin deve ter 6+ dígitos", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            prefs.saveAdminPin(newAdminPin)
                            changed = true
                        }

                        if (token.isEmpty() || chatId.isEmpty()) {
                            Toast.makeText(context, "Token e Chat ID não podem ficar vazios", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        prefs.saveTelegramConfig(token, chatId)
                        changed = true

                        Toast.makeText(
                            context,
                            if (changed) "Configurações salvas" else "Nada para salvar",
                            Toast.LENGTH_SHORT
                        ).show()
                        onClose()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = nuPurple),
                shape = RoundedCornerShape(28.dp),
                enabled = !loading
            ) {
                Text("Salvar", color = Color.White, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1A1A1A),
        fontFamily = InterFamily,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
