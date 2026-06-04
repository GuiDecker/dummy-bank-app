package com.guilherme.honeypot.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guilherme.honeypot.R
import com.guilherme.honeypot.data.AppPreferences
import com.guilherme.honeypot.service.EmergencyService
import com.guilherme.honeypot.ui.theme.HoneypotTheme
import com.guilherme.honeypot.ui.theme.InterFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        setContent {
            HoneypotTheme {
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
                color = Color.White,
                fontFamily = InterFamily
            )
        }
    }
}

@Composable
fun PinScreen(onPinSubmit: (String) -> Unit) {
    val nuPurple = Color(0xFF820AD1)
    val cpfMasked = "553.***.***-82"
    var senha by remember { mutableStateOf("") }
    var senhaVisible by remember { mutableStateOf(false) }

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
                .padding(horizontal = 28.dp)
                .padding(top = 32.dp, bottom = 24.dp)
        ) {
            // Logo nu (purple) - small, top left
            Image(
                painter = painterResource(id = R.drawable.nubank),
                contentDescription = "Nubank",
                modifier = Modifier
                    .size(40.dp),
                colorFilter = ColorFilter.tint(nuPurple)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Title
            Text(
                text = "Acesse sua conta",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = InterFamily
            )

            Spacer(modifier = Modifier.height(40.dp))

            // CPF (read-only, masked) — looks like a recognized account
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CPF",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontFamily = InterFamily
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cpfMasked,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A1A),
                    fontFamily = InterFamily
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Senha field (with toggle visibility)
            UnderlineField(
                label = "Senha",
                value = senha,
                onValueChange = { senha = it },
                keyboardType = KeyboardType.Password,
                isPassword = !senhaVisible,
                trailingIcon = {
                    IconButton(onClick = { senhaVisible = !senhaVisible }) {
                        Icon(
                            imageVector = if (senhaVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (senhaVisible) "Ocultar senha" else "Mostrar senha",
                            tint = Color.Gray
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Possui 8 caracteres ou mais",
                fontSize = 13.sp,
                color = Color.Gray,
                fontFamily = InterFamily
            )

            Spacer(modifier = Modifier.weight(1f))

            // Continuar button
            Button(
                onClick = { onPinSubmit(senha) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = nuPurple),
                shape = RoundedCornerShape(28.dp),
                enabled = senha.isNotEmpty()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Continuar",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFamily
                    )
                    Text(
                        text = "→",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun UnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray,
            fontFamily = InterFamily
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A1A),
                    fontFamily = InterFamily
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f)
            )
            if (trailingIcon != null) trailingIcon()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE0E0E0))
        )
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
            val context = LocalContext.current
            var tapCount by remember { mutableIntStateOf(0) }
            var firstTapAt by remember { mutableStateOf(0L) }

            Image(
                painter = painterResource(id = R.drawable.nubank),
                contentDescription = "Nubank",
                modifier = Modifier
                    .size(64.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - firstTapAt > 3000) {
                            firstTapAt = now
                            tapCount = 1
                        } else {
                            tapCount++
                        }
                        if (tapCount >= 5) {
                            tapCount = 0
                            context.startActivity(
                                Intent(context, SettingsActivity::class.java)
                            )
                        }
                    }
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
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFamily
                )
                Text(
                    text = "⌄",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFamily,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }
        }

        // Hero image — slightly tilted, anchored top-right, doesn't push text
        Image(
            painter = painterResource(id = R.drawable.nu_cards_hero),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(1.25f)
                .fillMaxHeight(0.78f)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-20).dp)
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
                color = Color.White,
                fontFamily = InterFamily
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
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily
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
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily
                )
            }
        }
    }
}
