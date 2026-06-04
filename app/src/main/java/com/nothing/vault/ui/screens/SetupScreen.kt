package com.nothing.vault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.vault.data.model.VaultFolder
import com.nothing.vault.data.repository.VaultRepository
import com.nothing.vault.ui.components.LiquidGlassBackground
import com.nothing.vault.ui.theme.Accent
import com.nothing.vault.ui.theme.AccentDark
import com.nothing.vault.ui.theme.GlassBorder
import com.nothing.vault.ui.theme.GlassWhite
import com.nothing.vault.ui.theme.GlassWhiteMedium
import com.nothing.vault.ui.theme.TextMuted
import com.nothing.vault.ui.theme.TextPrimary
import com.nothing.vault.ui.theme.TextSecondary
import com.nothing.vault.ui.theme.FolderColors

@Composable
fun SetupScreen(
    repository: VaultRepository,
    onComplete: () -> Unit
) {
    var folders by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var currentName by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var currentPinConfirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Nothing",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Light,
                fontSize = 42.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your vault folders",
                color = TextSecondary,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (folders.isEmpty()) {
                Text(
                    text = "Add at least one folder to get started.\nEach folder has its own PIN.\nWrong PIN opens the decoy screen.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Existing folders
            folders.forEachIndexed { index, (name, _) ->
                val color = FolderColors[index % FolderColors.size]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    ) {}
                    Text(
                        text = "$name •••••",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add folder form
            OutlinedTextField(
                value = currentName,
                onValueChange = { currentName = it },
                label = { Text("Folder name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = Accent,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = currentPin,
                onValueChange = { if (it.length <= 6) currentPin = it },
                label = { Text("PIN (4-6 digits)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = Accent,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = currentPinConfirm,
                onValueChange = { if (it.length <= 6) currentPinConfirm = it },
                label = { Text("Confirm PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = Accent,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        currentName.isBlank() -> error = "Enter a folder name"
                        currentPin.length < 4 -> error = "PIN must be 4-6 digits"
                        currentPin != currentPinConfirm -> error = "PINs don't match"
                        folders.any { it.first == currentName } -> error = "Folder name already exists"
                        else -> {
                            folders = folders + Pair(currentName, currentPin)
                            currentName = ""
                            currentPin = ""
                            currentPinConfirm = ""
                            error = null
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    disabledContainerColor = AccentDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add folder", color = TextPrimary)
            }

            if (folders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        folders.forEach { (name, pin) ->
                            repository.createFolder(name, pin)
                        }
                        onComplete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        disabledContainerColor = AccentDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done — ${folders.size} folders", color = TextPrimary)
                }
            }
        }
    }
}
