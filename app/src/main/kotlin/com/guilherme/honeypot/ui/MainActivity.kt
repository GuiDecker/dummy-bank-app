package com.guilherme.honeypot.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guilherme.honeypot.R
import com.guilherme.honeypot.data.AppPreferences
import com.guilherme.honeypot.service.EmergencyService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        setContent {
            var screen by remember { mutableStateOf("splash") }
            var setupNeeded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!prefs.isSetupComplete()) {
                    setupNeeded = true
                } else {
                    delay(1500)
                    screen = "welcome"
                }
            }

            if (setupNeeded) {
                LaunchedEffect(Unit) {
                    startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                    finish()
                }
            } else when (screen) {
                "splash" -> SplashScreen()
                "welcome" -> WelcomeScreen(onStart = { screen = "pin" })
                "pin" -> PinScreen(onPinSubmit = { pin -> validatePin(pin) })
            }
        }
    }

    private fun validatePin(pin: String) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            if (prefs.verifyPin(pin)) {
                Log.d("Honeypot", "Correct PIN - triggering emergency")
                triggerEmergency()
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
            .background(Color(0xFF820AD1)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.nubank),
                contentDescription = "Nubank",
                modifier = Modifier.size(132.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nubank",
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
                text = "Nubank",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF820AD1)
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
                                if (index < pin.length) Color(0xFF820AD1)
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
                                    color = Color(0xFF820AD1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val nuPurpleLight = Color(0xFFA259E6)
    val nuPurple = Color(0xFF820AD1)
    val nuPurpleDeep = Color(0xFF6B0BB8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(nuPurpleLight, nuPurple, nuPurpleDeep)
                )
            )
    ) {
        // Top bar: nu logo (left) + "Brasil v" pill (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.nubank),
                contentDescription = "Nubank",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .background(Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Brasil",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "⌄",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }
        }

        // Hero image — slightly tilted, anchored top-right, doesn't push text
        Image(
            painter = painterResource(id = R.drawable.nu_cards_hero),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(1.1f)
                .fillMaxHeight(0.7f)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 60.dp)
                .rotate(-30f),
                // .rotate(-15f),
            contentScale = ContentScale.Fit
        )

        // Bottom content: headline + CTAs (pushed up from the bottom edge)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Um mundo\nfinanceiro sem\ncomplexidades",
                fontSize = 42.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Primary CTA: Começar (darker purple for contrast against background)
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A0764)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Começar",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary CTA: Já sou cliente
            TextButton(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Já sou cliente",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
