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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.presentation.Tone
import com.aicfo.shared.sync.SyncCode
import kotlinx.coroutines.delay

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

data class NavTab(val id: String, val label: String, val icon: ImageVector, val testTag: String)

val NavTabs = listOf(
    NavTab("HOME", "Home", Icons.Outlined.Home, AutomationTags.NAV_HOME),
    NavTab("MOVES", "Moves", Icons.AutoMirrored.Outlined.FormatListBulleted, AutomationTags.NAV_MOVES),
    NavTab("ACCOUNTS", "Accounts", Icons.Outlined.AccountBalanceWallet, AutomationTags.NAV_ACCOUNTS),
    NavTab("SETTINGS", "Settings", Icons.Outlined.Person, AutomationTags.NAV_SETTINGS),
)

/**
 * Resource-ids UiAutomator / Appium see when [testTagsAsResourceId] is set.
 * `viewIdResourceName` is the tag itself (for example `nav_moves`), not `package:id/…`.
 */
internal object AutomationTags {
    const val NAV_HOME = "nav_home"
    const val NAV_MOVES = "nav_moves"
    const val NAV_ACCOUNTS = "nav_accounts"
    const val NAV_SETTINGS = "nav_settings"

    const val QA_TRIAL_PAYWALL = "qa_trial_paywall"
    const val QA_SHOW_PAYWALL = "qa_show_paywall"
    const val QA_RESTORE_TRIAL = "qa_restore_trial"
    const val QA_SIMULATE_PRO = "qa_simulate_pro"
    const val QA_CLEAR_OVERRIDE = "qa_clear_override"
    const val QA_REPLAY_ONBOARDING = "qa_replay_onboarding"
    const val QA_SIMULATE_RECONNECT = "qa_simulate_reconnect"
    const val QA_SIMULATE_FAILURE = "qa_simulate_failure"
    const val QA_LINK_MAYA_SAMPLE = "qa_link_maya_sample"
    const val QA_RETURN_TO_TRIAL = "qa_return_to_trial"
    const val BANK_FRESHNESS = "bank_freshness"
    const val BANK_FRESHNESS_ACTION = "bank_freshness_action"
    const val ACCOUNTS_PULL = "accounts_pull"

    const val PAYWALL_CONTINUE_YEARLY = "paywall_continue_yearly"
    const val PAYWALL_CONTINUE_MONTHLY = "paywall_continue_monthly"
}

/**
 * Resource-id and content description for a container. Descendants stay in the tree.
 */
internal fun Modifier.automationNode(tag: String, description: String): Modifier =
    semantics {
        testTagsAsResourceId = true
        testTag = tag
        contentDescription = description
    }

/**
 * Identifiers on the clickable node itself.
 *
 * [Modifier.clickable] merges descendants, and Compose then moves [contentDescription]
 * onto a non-clickable child. [clearAndSetSemantics] keeps the description, the label
 * text, and the click action on this node so UiAutomator can tap it. Place this
 * modifier outside [androidx.compose.foundation.clickable]; touch handling stays there.
 */
internal fun Modifier.tappableAutomationNode(
    tag: String,
    description: String,
    onActivate: () -> Unit,
): Modifier = clearAndSetSemantics {
    testTagsAsResourceId = true
    testTag = tag
    contentDescription = description
    text = AnnotatedString(description)
    onClick {
        onActivate()
        true
    }
}

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
                        .tappableAutomationNode(tab.testTag, tab.label) { onSelect(tab.id) }
                        .clip(RoundedCornerShape(999.dp))
                        .background(AiColors.Nav)
                        .clickable { onSelect(tab.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(tab.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            } else {
                Box(
                    Modifier
                        .tappableAutomationNode(tab.testTag, tab.label) { onSelect(tab.id) }
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onSelect(tab.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = AiColors.Muted,
                        modifier = Modifier.size(22.dp),
                    )
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
fun rememberFreshnessTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick += 1
        }
    }
    return tick
}

@Composable
fun FreshnessLine(
    label: String,
    action: String,
    code: String,
    onAction: () -> Unit,
) {
    if (label.isEmpty()) return
    val color = when (code) {
        SyncCode.NEEDS_REAUTH -> AiColors.Danger
        SyncCode.FAILED -> AiColors.Warning
        else -> AiColors.Muted
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = color,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    testTagsAsResourceId = true
                    testTag = AutomationTags.BANK_FRESHNESS
                },
        )
        if (action.isNotEmpty()) {
            Text(
                action,
                color = AiColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .tappableAutomationNode(AutomationTags.BANK_FRESHNESS_ACTION, action, onAction)
                    .clickable(onClick = onAction)
                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
            )
        }
    }
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
