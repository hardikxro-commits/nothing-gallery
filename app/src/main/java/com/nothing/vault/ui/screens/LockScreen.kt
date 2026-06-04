package com.nothing.vault.ui.screens

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nothing.vault.data.repository.VaultRepository
import com.nothing.vault.ui.components.GlassmorphicCard
import com.nothing.vault.ui.components.LiquidGlassBackground
import com.nothing.vault.ui.components.PinPad
import com.nothing.vault.ui.theme.Accent
import com.nothing.vault.ui.theme.GlassWhite
import com.nothing.vault.ui.theme.TextMuted
import com.nothing.vault.ui.theme.TextPrimary
import com.nothing.vault.ui.theme.TextSecondary
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LockScreen(
    repository: VaultRepository,
    onFolderUnlocked: (String) -> Unit,
    onDecoy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var checkJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(showError) {
        if (showError) {
            kotlinx.coroutines.delay(1500)
            showError = false
        }
    }

    val hasBiometric = remember {
        val bioManager = BiometricManager.from(context)
        bioManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }
    val biometricFolder = remember { repository.getBiometricFolder() }

    fun checkPin(input: String) {
        checkJob?.cancel()
        checkJob = scope.launch {
            val folder = withContext(Dispatchers.IO) {
                repository.findFolderByPin(input)
            }
            if (folder != null) {
                pin = ""
                onFolderUnlocked(folder.id)
            } else if (input.length >= 6) {
                showError = true
                pin = ""
                withContext(Dispatchers.IO) { repository.setInDecoy(true) }
                onDecoy()
            } else {
                showError = true
            }
        }
    }

    fun launchBiometric() {
        if (!hasBiometric || biometricFolder == null) return

        val executor = Executors.newSingleThreadExecutor()
        val biometricPrompt = BiometricPrompt(
            context as androidx.fragment.app.FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    pin = ""
                    onFolderUnlocked(biometricFolder.id)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Nothing")
            .setSubtitle("Scan fingerprint to open vault")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Clock display (Liquid Glass style)
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nothing",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Light,
                        fontSize = 36.sp
                    )
                    Text(
                        text = "secured",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val isFilled = index < pin.length
                    val dotColor by animateColorAsState(
                        targetValue = if (isFilled) Accent else GlassWhite,
                        animationSpec = spring(dampingRatio = 0.6f),
                        label = "dotColor"
                    )
                    val dotScale by animateFloatAsState(
                        targetValue = if (isFilled) 1.3f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f),
                        label = "dotScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Invalid PIN",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Pad
            PinPad(
                onDigit = { digit ->
                    if (pin.length < 6) {
                        pin += digit
                        if (pin.length >= 4) {
                            checkPin(pin)
                        }
                    }
                },
                onDelete = {
                    if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                    }
                },
                onBiometric = { launchBiometric() },
                showBiometric = hasBiometric && biometricFolder != null
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Enter PIN to continue",
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private const val TAG = "LockScreen"
