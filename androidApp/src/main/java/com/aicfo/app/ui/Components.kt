package com.aicfo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.presentation.Tone

fun Modifier.softCard(radius: Dp = 24.dp, color: Color = AiColors.Card): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(0x14101828),
            spotColor = Color(0x1A101828),
        )
        .background(color, shape)
}

@Composable
fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = if (enabled) AiColors.Accent else AiColors.Accent.copy(alpha = 0.4f)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x40635BFF), spotColor = Color(0x33635BFF))
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .softCard(radius = 18.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun ImpactPill(text: String, tone: String = Tone.POSITIVE) {
    val (bg, fg) = when (tone) {
        Tone.WARNING -> AiColors.WarningSoft to AiColors.Warning
        Tone.DANGER -> AiColors.DangerSoft to AiColors.Danger
        else -> AiColors.SuccessSoft to AiColors.Success
    }
    Text(
        text = text,
        color = fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun MetaPill(text: String, tone: String) {
    val (bg, fg) = when (tone) {
        "P1", Tone.DANGER -> AiColors.DangerSoft to AiColors.Danger
        "P2", Tone.WARNING -> AiColors.WarningSoft to AiColors.Warning
        "P3" -> AiColors.AccentSoft to AiColors.Accent
        "DONE" -> AiColors.SuccessSoft to AiColors.Success
        else -> AiColors.Chip to Color(0xFF64708A)
    }
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun IconBubble(icon: String) {
    val (bg, fg, vector) = when (icon) {
        "CLOCK" -> Triple(AiColors.DangerSoft, AiColors.Danger, Icons.Outlined.Schedule)
        "CARD" -> Triple(AiColors.WarningSoft, AiColors.Warning, Icons.Outlined.CreditCard)
        else -> Triple(AiColors.SuccessSoft, AiColors.Success, Icons.Outlined.Payments)
    }
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(vector, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

data class NavTab(val id: String, val label: String, val icon: ImageVector)

val NavTabs = listOf(
    NavTab("HOME", "Home", Icons.Outlined.Home),
    NavTab("MOVES", "Moves", Icons.AutoMirrored.Outlined.FormatListBulleted),
    NavTab("ACCOUNTS", "Accounts", Icons.Outlined.AccountBalanceWallet),
    NavTab("SETTINGS", "Settings", Icons.Outlined.Person),
)

@Composable
fun PillNav(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .softCard(radius = 32.dp)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        NavTabs.forEach { tab ->
            val active = tab.id == selected
            if (active) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(AiColors.Nav)
                        .clickable { onSelect(tab.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(tab.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            } else {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onSelect(tab.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = AiColors.Muted, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun ReadOnlyChip() {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AiColors.SuccessSoft)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(AiColors.Success))
        Spacer(Modifier.size(6.dp))
        Text("Read-only", color = AiColors.Success, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AccountLine(text: String) {
    if (text.isEmpty()) return
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AiColors.Chip)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(AiColors.Success))
        Spacer(Modifier.size(8.dp))
        Text(text, color = AiColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = AiColors.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.1.sp,
    )
}

@Composable
fun Kicker(text: String) {
    Text(text, color = AiColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp)
}

@Composable
fun Banner(text: String) {
    if (text.isEmpty()) return
    Text(
        text = text,
        color = AiColors.Accent,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AiColors.AccentSoft)
            .padding(14.dp),
    )
}
