package com.aicfo.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.domain.AiCfoController

@Composable
fun PaywallScreen(controller: AiCfoController, tick: Int) {
    val model = remember(tick) { controller.paywall() }
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(36.dp))
            Kicker("AI CFO Pro")
            Spacer(Modifier.height(10.dp))
            Text(model.title, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp)
            Spacer(Modifier.height(12.dp))
            Text(model.lede, color = AiColors.Muted, fontSize = 16.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(22.dp))
            Column(Modifier.fillMaxWidth().softCard().padding(18.dp)) {
                Text(model.yearlyNote, color = AiColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(model.yearlyPrice, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                Text(model.yearlyPeriod, color = AiColors.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(model.yearlyCta) { controller.purchaseYearly() }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.softCard().padding(18.dp)) {
                Text(model.monthlyPrice, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text(model.monthlyPeriod, color = AiColors.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                SecondaryButton(model.monthlyCta) { controller.purchaseMonthly() }
            }
            Spacer(Modifier.height(16.dp))
            Text(model.finePrint, color = AiColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(18.dp))
            Text("QA", color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            SecondaryButton("QA: return to trial") { controller.debugForceTrial() }
            Spacer(Modifier.height(24.dp))
        }
    }
}
