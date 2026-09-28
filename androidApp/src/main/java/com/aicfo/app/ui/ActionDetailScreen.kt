package com.aicfo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.app.theme.parseHex
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.presentation.DetailModel
import com.aicfo.shared.presentation.LayoutCode
import com.aicfo.shared.presentation.Tone

@Composable
fun ActionDetailScreen(
    controller: AiCfoController,
    tick: Int,
    moveId: String,
    onBack: () -> Unit,
    showBack: Boolean,
) {
    val detail = remember(tick, moveId) { controller.detail(moveId) } ?: return
    Column(Modifier.fillMaxSize().background(AiColors.Bg)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            if (showBack) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onBack)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AiColors.Accent)
                    Spacer(Modifier.size(4.dp))
                    Text(detail.backLabel, color = AiColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MetaPill(detail.priority, detail.priority)
                Text(detail.category, color = AiColors.Danger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                MetaPill(detail.statusLabel, detail.status)
            }
            Spacer(Modifier.height(14.dp))
            Text(detail.title, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(detail.lede, color = AiColors.Muted, fontSize = 16.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(18.dp))
            when (detail.layout) {
                LayoutCode.SUBSCRIPTION -> SubscriptionBody(detail)
                LayoutCode.DEBT, LayoutCode.CASH -> MathBody(detail, dangerHero = detail.layout == LayoutCode.DEBT)
                else -> SimpleBody(detail)
            }
            Spacer(Modifier.height(16.dp))
            Text(detail.body, color = AiColors.Text, fontSize = 16.sp, lineHeight = 24.sp)
            if (detail.freeUpValue.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().softCard(radius = 20.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(detail.freeUpLabel, color = AiColors.Text, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(
                        detail.freeUpValue,
                        color = AiColors.Success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(AiColors.SuccessSoft)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            if (detail.accountLine.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                AccountLine(detail.accountLine)
            }
            if (detail.banner.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Banner(detail.banner)
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(bottom = if (showBack) 0.dp else 84.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            PrimaryButton(detail.primaryCta) { controller.performPrimary(detail.id) }
            Spacer(Modifier.height(10.dp))
            SecondaryButton(detail.secondaryCta) { controller.performSecondary(detail.id) }
        }
    }
}

@Composable
private fun MathBody(detail: DetailModel, dangerHero: Boolean) {
    val heroBg = if (dangerHero) AiColors.DangerSoft else AiColors.AccentSoft
    val heroFg = if (dangerHero) AiColors.Danger else AiColors.Accent
    Column(Modifier.fillMaxWidth().softCard().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                detail.heroValue,
                color = heroFg,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(heroBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(detail.heroCaption, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(detail.heroSub, color = AiColors.Muted, fontSize = 13.sp)
            }
        }
    }
    if (detail.sectionLabel.isEmpty()) return
    Spacer(Modifier.height(14.dp))
    Column(Modifier.fillMaxWidth().softCard().padding(16.dp)) {
        Text(detail.sectionLabel, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(detail.statLeftValue, detail.statLeftLabel, Modifier.weight(1f))
            StatTile(detail.statRightValue, detail.statRightLabel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AiColors.Bg)
            .padding(14.dp),
    ) {
        Text(value, color = AiColors.Success, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = AiColors.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun SubscriptionBody(detail: DetailModel) {
    Column(Modifier.fillMaxWidth().softCard().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(parseHex(detail.merchantColorHex.ifBlank { "#16A34A" })),
                contentAlignment = Alignment.Center,
            ) {
                Text(detail.merchantInitial, color = AiColors.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(detail.merchantName, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(detail.merchantMeta, color = AiColors.Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        val facts = detail.facts
        if (facts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in facts.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { fact ->
                            FactTile(fact.value, fact.label, fact.tone, Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FactTile(value: String, label: String, tone: String, modifier: Modifier) {
    val color = if (tone == Tone.WARNING) AiColors.Warning else AiColors.Text
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AiColors.Bg)
            .padding(14.dp),
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = AiColors.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SimpleBody(detail: DetailModel) {
    if (detail.heroValue.isEmpty()) return
    Column(Modifier.fillMaxWidth().softCard().padding(16.dp)) {
        Text(detail.heroValue, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Text(detail.heroCaption, color = AiColors.Text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Text(detail.heroSub, color = AiColors.Muted, fontSize = 13.sp)
    }
}
