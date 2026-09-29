package com.aicfo.app.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.BuildConfig
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.domain.AiCfoController

@Composable
fun SettingsScreen(controller: AiCfoController, tick: Int) {
    val model = remember(tick) { controller.settings() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Settings", color = AiColors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().softCard().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(AiColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(model.initials, color = AiColors.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(model.name, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(model.meta, color = AiColors.Muted, fontSize = 13.sp)
            }
        }
        Column(Modifier.fillMaxWidth().softCard().padding(16.dp)) {
            Text(model.planLabel, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(model.planDetail, color = AiColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        ToggleRow("Notifications", "A dot on Home when a move is waiting.", model.notificationsEnabled) {
            controller.setNotifications(it)
        }
        ToggleRow(
            "Biometric lock",
            if (model.biometricHardware) "Ask for biometrics each time the app opens." else "No biometric hardware on this device. The gate still shows a continue path.",
            model.biometricEnabled,
        ) { controller.setBiometricEnabled(it) }
        if (model.biometricEnabled) {
            SecondaryButton("Lock now") { controller.lockNow() }
        }
        Column(Modifier.fillMaxWidth().softCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("About", color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(model.brandTagline, color = AiColors.Text, fontSize = 15.sp)
        }
        Column(Modifier.fillMaxWidth().softCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Privacy", color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Read-only linking. Bank passwords are never stored.", color = AiColors.Muted, fontSize = 14.sp)
            Text("Link tokens are encrypted in the Android Keystore.", color = AiColors.Muted, fontSize = 14.sp)
            Text("Logs redact tokens, passwords, and card numbers.", color = AiColors.Muted, fontSize = 14.sp)
            Text(
                if (model.banksLinked) "Sample institutions are linked." else "No institutions linked.",
                color = AiColors.Muted,
                fontSize = 14.sp,
            )
        }
        if (model.banksLinked) {
            SecondaryButton("Disconnect institutions") { controller.disconnectAll() }
        }
        if (BuildConfig.DEBUG && model.qaEnabled) {
            val qaTitle = "QA · trial / paywall"
            Column(
                Modifier
                    .fillMaxWidth()
                    .automationNode(AutomationTags.QA_TRIAL_PAYWALL, qaTitle)
                    .softCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(qaTitle, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "Debug tools for this build. Force the hard paywall, restore the 30-day trial, or simulate a bank that needs reconnect.",
                    color = AiColors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                QaAction("Show paywall", AutomationTags.QA_SHOW_PAYWALL, primary = true) {
                    controller.debugForcePaywall()
                }
                QaAction("Restore trial", AutomationTags.QA_RESTORE_TRIAL) {
                    controller.debugForceTrial()
                }
                QaAction("Simulate Pro", AutomationTags.QA_SIMULATE_PRO) {
                    controller.debugForcePro()
                }
                QaAction("Clear QA override", AutomationTags.QA_CLEAR_OVERRIDE) {
                    controller.debugClearOverride()
                }
                QaAction("Replay onboarding", AutomationTags.QA_REPLAY_ONBOARDING) {
                    controller.debugReplayOnboarding()
                }
                QaAction("Simulate bank reconnect", AutomationTags.QA_SIMULATE_RECONNECT) {
                    controller.debugSimulateNeedsReauth()
                }
                QaAction("Simulate sync failure", AutomationTags.QA_SIMULATE_FAILURE) {
                    controller.debugSimulateSyncFailure()
                }
            }
        }
    }
}

@Composable
private fun QaAction(
    label: String,
    tag: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val modifier = Modifier.tappableAutomationNode(tag, label, onClick)
    if (primary) {
        PrimaryButton(label, modifier, onClick = onClick)
    } else {
        SecondaryButton(label, modifier, onClick)
    }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().softCard().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = AiColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AiColors.Accent,
                checkedThumbColor = AiColors.White,
            ),
        )
    }
}
