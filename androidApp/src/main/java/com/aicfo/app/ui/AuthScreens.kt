package com.aicfo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.auth.PhoneNumbers
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.presentation.AuthStep
import kotlinx.coroutines.delay

private val FieldStroke = Color(0xFFE6E8EE)
private val DotIdle = Color(0xFFD5D8E0)
private val Lavender = Color(0xFFE7E4FF)

@Composable
fun AuthFlowScreen(controller: AiCfoController, tick: Int) {
    val step = remember(tick) { controller.authStep() }
    Box(Modifier.fillMaxSize().statusBarsPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 440.dp)
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            BrandHeader()
            Spacer(Modifier.height(28.dp))
            when (step) {
                AuthStep.OTP -> OtpStep(controller, tick)
                AuthStep.EMAIL -> EmailStep(controller, tick)
                AuthStep.UNLOCK -> PinSetupStep(controller, tick)
                else -> PhoneStep(controller, tick)
            }
        }
    }
}

@Composable
private fun ColumnScope.PhoneStep(controller: AiCfoController, tick: Int) {
    var digits by remember { mutableStateOf("") }
    val error = remember(tick) { controller.phoneError() }
    val debug = remember(tick) { controller.debugAuthTools() }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Text("What's your number?", color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "We'll text a one-time code. No password to remember.",
            color = AiColors.Muted,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
        Spacer(Modifier.height(22.dp))
        Text("Mobile number", color = AiColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🇺🇸", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("+1", color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.width(4.dp))
                Text("▾", color = AiColors.Muted, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (digits.isEmpty()) {
                    Text("(555) 000-0000", color = Color(0xFFC5CAD6), fontSize = 16.sp)
                }
                BasicTextField(
                    value = PhoneNumbers.formatNational(digits),
                    onValueChange = { digits = PhoneNumbers.usDigits(it) },
                    textStyle = TextStyle(color = AiColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    singleLine = true,
                    cursorBrush = SolidColor(AiColors.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = AiColors.Danger, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Lavender)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = AiColors.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "We'll text a code — no password. US numbers first.",
                color = AiColors.Accent,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Continue", enabled = digits.length == 10) { controller.submitPhone(digits) }
    if (debug) {
        Text(
            "Debug skip",
            color = AiColors.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { controller.debugSkipPhone() }
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun ColumnScope.OtpStep(controller: AiCfoController, tick: Int) {
    var code by remember { mutableStateOf("") }
    var pulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            pulse += 1
        }
    }
    val sentTo = remember(tick) { controller.maskedPhone() }
    val error = remember(tick) { controller.otpError() }
    val seconds = remember(tick, pulse) { controller.resendSeconds() }
    val debugCode = remember(tick) { controller.debugOtpCode() }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Text("Enter the code", color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 32.sp)
        Spacer(Modifier.height(8.dp))
        Text("Sent to $sentTo", color = AiColors.Muted, fontSize = 16.sp)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth()) {
            OtpBoxes(code)
            BasicTextField(
                value = code,
                onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.matchParentSize(),
                textStyle = TextStyle(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
            )
        }
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = AiColors.Danger, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (seconds > 0) {
                Text(resendLabel(seconds), color = AiColors.Muted, fontSize = 14.sp)
            } else {
                Text(
                    "Resend code",
                    color = AiColors.Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { controller.resendOtp() },
                )
            }
            Text(
                "Change number",
                color = AiColors.Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { controller.changePhoneNumber() },
            )
        }
        if (debugCode.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Fill debug code",
                color = AiColors.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { code = debugCode },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Verify", enabled = code.length == 6) { controller.verifyOtp(code) }
}

@Composable
private fun ColumnScope.EmailStep(controller: AiCfoController, tick: Int) {
    var email by remember { mutableStateOf("") }
    val error = remember(tick) { controller.emailError() }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Text(
            "Where should we send your wins?",
            color = AiColors.Text,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Monthly email: how much you saved this month — calm summary, not spam.",
            color = AiColors.Muted,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
        Spacer(Modifier.height(22.dp))
        Text("Email for reports", color = AiColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Email, contentDescription = null, tint = AiColors.Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (email.isEmpty()) {
                    Text("maya@studio.example", color = Color(0xFFC5CAD6), fontSize = 16.sp)
                }
                BasicTextField(
                    value = email,
                    onValueChange = { email = it.take(120) },
                    textStyle = TextStyle(color = AiColors.Text, fontSize = 16.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(AiColors.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = AiColors.Danger, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().softCard(radius = 22.dp).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(AiColors.Success))
                Spacer(Modifier.width(8.dp))
                Text("Monthly savings report", color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "One note each month on moves completed and dollars kept — not a login method.",
                color = AiColors.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Account login stays phone + OTP. Email is only for reports.",
            color = AiColors.Muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Continue", enabled = email.isNotBlank()) { controller.saveReportEmail(email) }
    Spacer(Modifier.height(10.dp))
    SecondaryButton("Skip for now") { controller.skipReportEmail() }
}

@Composable
private fun ColumnScope.PinSetupStep(controller: AiCfoController, tick: Int) {
    val hardware = remember(tick) { controller.settings().biometricHardware }
    val error = remember(tick) { controller.pinError() }
    val activity = LocalContext.current.findActivity()
    var first by remember { mutableStateOf<String?>(null) }
    var entry by remember { mutableStateOf("") }
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlatformPill("ANDROID · DEVICE UNLOCK")
        Spacer(Modifier.height(18.dp))
        Text(
            "Create a 6-digit PIN",
            color = AiColors.Text,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Unlocks the app on this phone. Not your account password — login stays phone + OTP.",
            color = AiColors.Muted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Lavender),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = AiColors.Accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(16.dp))
        PinDots(entry.length)
        Spacer(Modifier.height(8.dp))
        Text(
            if (first == null) "Enter PIN · confirm next" else "Confirm PIN",
            color = AiColors.Muted,
            fontSize = 13.sp,
        )
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(error, color = AiColors.Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        PinKeypad(
            onDigit = { digit ->
                if (entry.length >= 6) return@PinKeypad
                val next = entry + digit
                entry = next
                if (next.length == 6) {
                    val pending = first
                    if (pending == null) {
                        first = next
                        entry = ""
                    } else if (!controller.saveDevicePin(pending, next)) {
                        entry = ""
                    }
                }
            },
            onDelete = { if (entry.isNotEmpty()) entry = entry.dropLast(1) },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Release requires an unlock path — biometric preferred, PIN is the fallback.",
            color = AiColors.Accent,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Lavender)
                .padding(14.dp),
        )
        if (hardware) {
            Spacer(Modifier.height(14.dp))
            Text(
                "Use fingerprint instead",
                color = AiColors.Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.clickable {
                    val host = activity as? FragmentActivity ?: return@clickable
                    promptBiometric(host) { ok ->
                        if (ok) controller.enableBiometricUnlock()
                    }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text("F", color = AiColors.Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Finwise", color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Your AI CFO", color = AiColors.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PlatformPill(text: String) {
    Text(
        text,
        color = AiColors.Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE8EAF0))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun OtpBoxes(code: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(6) { index ->
            val char = code.getOrNull(index)?.toString().orEmpty()
            val active = code.length < 6 && index == code.length
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(0.82f)
                    .clip(shape)
                    .background(Color.White)
                    .border(if (active) 2.dp else 1.dp, if (active) AiColors.Accent else FieldStroke, shape),
                contentAlignment = Alignment.Center,
            ) {
                Text(char, color = AiColors.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun PinDots(filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(6) { index ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (index < filled) AiColors.Accent else DotIdle),
            )
        }
    }
}

@Composable
internal fun PinKeypad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "del"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (key.isEmpty()) Color.Transparent else Color.White)
                            .clickable(enabled = key.isNotEmpty()) {
                                if (key == "del") onDelete() else onDigit(key)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (key) {
                            "" -> Unit
                            "del" -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete", tint = AiColors.Text)
                            else -> Text(key, color = AiColors.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun resendLabel(seconds: Int): String {
    val m = seconds / 60
    val s = (seconds % 60).toString().padStart(2, '0')
    return "Resend in $m:$s"
}
