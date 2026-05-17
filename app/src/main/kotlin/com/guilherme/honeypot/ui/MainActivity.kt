package com.guilherme.honeypot.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guilherme.honeypot.data.AppPreferences
import com.guilherme.honeypot.service.EmergencyService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            var setupNeeded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!prefs.isSetupComplete()) {
                    setupNeeded = true
                    showSplash = false
                } else {
                    delay(1500)
                    showSplash = false
                }
            }

            if (setupNeeded) {
                LaunchedEffect(Unit) {
                    startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                    finish()
                }
            } else if (showSplash) {
                SplashScreen()
            } else {
                PinScreen(
                    onPinSubmit = { pin -> validatePin(pin) }
                )
            }
        }
    }

    private fun validatePin(pin: String) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            if (prefs.verifyPin(pin)) {
                Log.d("Honeypot", "Correct PIN - closing silently")
                finishAffinity()
            } else {
                Log.d("Honeypot", "Wrong PIN - triggering emergency")
                triggerEmergency()
            }
        }
    }

    private fun triggerEmergency() {
        val intent = Intent(this, EmergencyService::class.java)
        startForegroundService(intent)
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A237E)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "B",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Banco Digital",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun PinScreen(onPinSubmit: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "Banco Digital",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Digite sua senha de acesso",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(40.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < pin.length) Color(0xFF1A237E)
                                else Color(0xFFBDBDBD)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Numeric keypad
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        if (digit.isEmpty()) {
                            Spacer(modifier = Modifier.size(72.dp))
                        } else {
                            TextButton(
                                onClick = {
                                    if (digit == "⌫") {
                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    } else if (pin.length < 4) {
                                        pin += digit
                                        if (pin.length == 4) {
                                            scope.launch {
                                                delay(200)
                                                attempts++
                                                onPinSubmit(pin)
                                                errorMessage = "Senha incorreta. Tente novamente."
                                                pin = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(72.dp)
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 24.sp,
                                    color = Color(0xFF1A237E)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
