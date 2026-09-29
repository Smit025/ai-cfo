package com.aicfo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.domain.AiCfoController

@Composable
fun LockScreen(controller: AiCfoController, tick: Int, onBiometric: () -> Unit) {
    val model = remember(tick) { controller.lock() }
    var pinMode by remember(model.preferPin, model.mustCreatePin) {
        mutableStateOf(model.preferPin || model.mustCreatePin)
    }
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 440.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (pinMode) {
                PinUnlock(controller, tick, creating = !model.pinSet, onBiometric = onBiometric, showBio = model.hardwareAvailable)
            } else {
                Spacer(Modifier.height(36.dp))
                MarkBlob()
                Spacer(Modifier.height(22.dp))
                Text(
                    model.kicker,
                    color = AiColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.1.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(model.title, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "You're still signed in. Biometrics unlock this device — not a new login.",
                    color = AiColors.Muted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Column(
                    Modifier.fillMaxWidth().softCard(radius = 22.dp).padding(vertical = 22.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AiColors.AccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = AiColors.Accent)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Biometrics", color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        model.methodDetail,
                        color = AiColors.Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(22.dp))
                PrimaryButton(if (model.hardwareAvailable) "Unlock with biometrics" else model.primaryCta) {
                    if (model.hardwareAvailable) onBiometric() else controller.unlockWithoutHardware()
                }
                if (model.hardwareAvailable) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Use PIN",
                        color = AiColors.Muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { pinMode = true }.padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PinUnlock(
    controller: AiCfoController,
    tick: Int,
    creating: Boolean,
    onBiometric: () -> Unit,
    showBio: Boolean,
) {
    val error = remember(tick) { controller.pinError() }
    var first by remember { mutableStateOf<String?>(null) }
    var entry by remember { mutableStateOf("") }
    Spacer(Modifier.height(12.dp))
    Text(
        if (creating) "Create a 6-digit PIN" else "Enter your PIN",
        color = AiColors.Text,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Device unlock only. Your account stays signed in with phone + OTP.",
        color = AiColors.Muted,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(18.dp))
    PinDots(entry.length)
    Spacer(Modifier.height(8.dp))
    Text(
        if (creating && first == null) "Enter PIN · confirm next" else if (creating) "Confirm PIN" else "6 digits",
        color = AiColors.Muted,
        fontSize = 13.sp,
    )
    if (error.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(error, color = AiColors.Danger, fontSize = 13.sp)
    }
    Spacer(Modifier.height(16.dp))
    PinKeypad(
        onDigit = { digit ->
            if (entry.length >= 6) return@PinKeypad
            val next = entry + digit
            entry = next
            if (next.length < 6) return@PinKeypad
            if (creating) {
                val pending = first
                if (pending == null) {
                    first = next
                    entry = ""
                } else if (!controller.saveDevicePin(pending, next)) {
                    entry = ""
                }
            } else if (!controller.unlockWithPin(next)) {
                entry = ""
            }
        },
        onDelete = { if (entry.isNotEmpty()) entry = entry.dropLast(1) },
    )
    if (showBio) {
        Spacer(Modifier.height(14.dp))
        Text(
            "Use biometrics",
            color = AiColors.Accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.clickable { onBiometric() }.padding(8.dp),
        )
    }
}

@Composable
private fun MarkBlob() {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(132.dp)
                .offset(x = 16.dp, y = -10.dp)
                .clip(CircleShape)
                .background(AiColors.Accent.copy(alpha = 0.18f)),
        )
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(AiColors.Accent.copy(alpha = 0.28f)),
        )
        Box(
            Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(AiColors.White),
            contentAlignment = Alignment.Center,
        ) {
            Text("F", color = AiColors.Accent, fontWeight = FontWeight.Bold, fontSize = 34.sp)
        }
    }
}
