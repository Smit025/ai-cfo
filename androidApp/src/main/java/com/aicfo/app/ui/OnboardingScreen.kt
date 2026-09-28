package com.aicfo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.presentation.OnboardingCard
import com.aicfo.shared.presentation.OnboardingModel

private val DotIdle = Color(0xFFD5D8E0)
private val NoteBg = Color(0xFFE7E9EE)
private val Hairline = Color(0xFFE6E8EE)

/**
 * Four-step onboarding matched to Sofia's v1.1 boards:
 * welcome, actions-not-charts, read-only connect, 30-day Pro trial.
 */
@Composable
fun OnboardingScreen(controller: AiCfoController, tick: Int) {
    val model = remember(tick) { controller.onboarding() }
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
        ) {
            if (model.step == 0) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WelcomeStep(model)
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(18.dp))
                    when (model.step) {
                        1 -> ValueStep(model)
                        2 -> ConnectStep(model)
                        else -> TrialStep(model)
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            ProgressDots(model.step, model.stepCount)
            Spacer(Modifier.height(18.dp))
            if (model.linkError.isNotEmpty()) {
                Text(
                    model.linkError,
                    color = AiColors.Danger,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }
            PrimaryButton(label = model.primaryCta, onClick = { controller.primaryOnboarding() })
            when (model.step) {
                2 -> if (model.secondaryCta.isNotEmpty()) {
                    Text(
                        model.secondaryCta,
                        color = AiColors.Muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { controller.secondaryOnboarding() }
                            .padding(vertical = 14.dp),
                    )
                }
                3 -> if (model.secondaryCta.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(label = model.secondaryCta, onClick = { controller.secondaryOnboarding() })
                    if (model.footnote.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            model.footnote,
                            color = AiColors.Muted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(if (model.step == 2) 4.dp else 16.dp))
        }
    }
}

@Composable
private fun WelcomeStep(model: OnboardingModel) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BrandMark()
        Spacer(Modifier.height(28.dp))
        Text(
            model.kicker,
            color = AiColors.Accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = 0.3.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            model.title,
            color = AiColors.Text,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            model.body,
            color = AiColors.Muted,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun BrandMark() {
    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(148.dp)
                .offset(x = 26.dp, y = (-6).dp)
                .clip(CircleShape)
                .background(AiColors.Accent.copy(alpha = 0.55f)),
        )
        Box(
            Modifier
                .size(118.dp)
                .offset(x = (-34).dp, y = 22.dp)
                .clip(CircleShape)
                .background(Color(0xFFB7B4FF).copy(alpha = 0.72f)),
        )
        Box(
            Modifier
                .size(96.dp)
                .offset(x = 46.dp, y = 34.dp)
                .clip(CircleShape)
                .background(AiColors.Accent.copy(alpha = 0.28f)),
        )
        Box(
            Modifier
                .size(84.dp)
                .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x33635BFF), spotColor = Color(0x26635BFF))
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = AiColors.Accent, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun ValueStep(model: OnboardingModel) {
    Kicker(model.kicker)
    Spacer(Modifier.height(12.dp))
    Text(model.title, color = AiColors.Text, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(model.body, color = AiColors.Muted, fontSize = 16.sp, lineHeight = 23.sp)
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (index in 0 until model.cardCount()) {
            MoveMiniCard(model.cardAt(index))
        }
    }
    if (model.footnote.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NoteBg)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = null, tint = AiColors.Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(model.footnote, color = AiColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun MoveMiniCard(card: OnboardingCard) {
    Row(
        Modifier.fillMaxWidth().softCard(radius = 22.dp).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(card.icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(card.title, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(card.subtitle, color = AiColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(8.dp))
        ImpactPill(card.impact)
    }
}

@Composable
private fun ConnectStep(model: OnboardingModel) {
    Kicker(model.kicker)
    Spacer(Modifier.height(12.dp))
    Text(model.title, color = AiColors.Text, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(model.body, color = AiColors.Muted, fontSize = 16.sp, lineHeight = 23.sp)
    Spacer(Modifier.height(18.dp))
    Column(Modifier.fillMaxWidth().softCard(radius = 22.dp).padding(18.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(AiColors.SuccessSoft)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = AiColors.Success, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(model.badge, color = AiColors.Success, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(model.trustTitle, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(model.trustBody, color = AiColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
    }
    Spacer(Modifier.height(22.dp))
    SectionLabel(model.sectionLabel)
    Spacer(Modifier.height(12.dp))
    val count = model.typeCount()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var index = 0
        while (index < count) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ConnectTypeCard(model.typeAt(index), Modifier.weight(1f))
                if (index + 1 < count) {
                    ConnectTypeCard(model.typeAt(index + 1), Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            index += 2
        }
    }
}

@Composable
private fun ConnectTypeCard(card: OnboardingCard, modifier: Modifier) {
    Column(modifier.softCard(radius = 22.dp).padding(16.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AiColors.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(connectIcon(card.icon), contentDescription = null, tint = AiColors.Accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(card.title, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(2.dp))
        Text(card.subtitle, color = AiColors.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun TrialStep(model: OnboardingModel) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            model.badge,
            color = AiColors.Accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(AiColors.AccentSoft)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            model.title,
            color = AiColors.Text,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            model.body,
            color = AiColors.Muted,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
    Spacer(Modifier.height(20.dp))
    Column(Modifier.fillMaxWidth().softCard(radius = 22.dp).padding(horizontal = 18.dp, vertical = 16.dp)) {
        SectionLabel(model.sectionLabel)
        Spacer(Modifier.height(8.dp))
        val features = model.featureCount()
        for (index in 0 until features) {
            FeatureRow(model.featureAt(index))
            if (index < features - 1) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            }
        }
    }
    if (model.chipCount() > 0) {
        Spacer(Modifier.height(16.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (index in 0 until model.chipCount()) {
                Text(
                    model.chipAt(index),
                    color = AiColors.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AiColors.Chip)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(card: OnboardingCard) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(AiColors.Success),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(card.title, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(card.subtitle, color = AiColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ProgressDots(step: Int, count: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            if (index == step) {
                Box(Modifier.size(width = 22.dp, height = 6.dp).clip(CircleShape).background(AiColors.Accent))
            } else {
                Box(Modifier.size(6.dp).clip(CircleShape).background(DotIdle))
            }
        }
    }
}

private fun connectIcon(code: String): ImageVector = when (code) {
    "BANK" -> Icons.Outlined.AccountBalance
    "CARD" -> Icons.Outlined.CreditCard
    "LOAN" -> Icons.Outlined.Add
    "INVEST" -> Icons.AutoMirrored.Outlined.TrendingUp
    "CLOCK" -> Icons.Outlined.Schedule
    else -> Icons.Outlined.Payments
}
