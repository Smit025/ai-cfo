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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.aicfo.shared.domain.AiCfoController

/**
 * Four-step soft-card onboarding. Sofia boards can replace the step layouts
 * without moving tokens or the shared copy in [AiCfoController.onboarding].
 */
@Composable
fun OnboardingScreen(controller: AiCfoController, tick: Int) {
    val model = remember(tick) { controller.onboarding() }
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(model.stepCount) { index ->
                    Box(
                        Modifier
                            .size(if (index == model.step) 22.dp else 7.dp, 7.dp)
                            .clip(CircleShape)
                            .background(if (index == model.step) AiColors.Accent else AiColors.AccentSoft),
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(28.dp))
                Kicker(model.kicker)
                Spacer(Modifier.height(10.dp))
                Text(model.title, color = AiColors.Text, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(model.body, color = AiColors.Muted, fontSize = 16.sp, lineHeight = 23.sp)
                Spacer(Modifier.height(22.dp))
                when (model.step) {
                    0 -> WelcomePreview(model.previewTitle, model.previewImpact, model.previewBody)
                    1 -> BulletCards(model)
                    2 -> TrustStep(model) { controller.connectReadOnlyStub() }
                    else -> PricePair(model.priceLeft, model.priceRight, model.priceNote)
                }
                Spacer(Modifier.height(24.dp))
            }
            PrimaryButton(
                label = model.primaryCta,
                enabled = model.canAdvance,
                onClick = { controller.advanceOnboarding() },
            )
            if (model.canGoBack) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(label = "Back", onClick = { controller.backOnboarding() })
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun WelcomePreview(title: String, impact: String, body: String) {
    Column(Modifier.fillMaxWidth().softCard().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble("CLOCK")
            Spacer(Modifier.weight(1f))
            ImpactPill(impact)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(body, color = AiColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(12.dp))
        Text("Cancel · save \$47/mo  →", color = AiColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun BulletCards(model: com.aicfo.shared.presentation.OnboardingModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (index in 0 until model.bulletCount()) {
            Text(
                model.bulletAt(index),
                color = AiColors.Text,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().softCard(radius = 20.dp).padding(16.dp),
            )
        }
    }
}

@Composable
private fun TrustStep(model: com.aicfo.shared.presentation.OnboardingModel, onConnect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().softCard().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (index in 0 until model.bulletCount()) {
                Text("•  ${model.bulletAt(index)}", color = AiColors.Text, fontSize = 15.sp, lineHeight = 21.sp)
            }
        }
        if (model.banksLinked) {
            Text(
                model.linkedSummary,
                color = AiColors.Success,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().softCard(radius = 18.dp).padding(16.dp),
            )
        } else {
            SecondaryButton(label = model.connectCta, onClick = onConnect)
        }
    }
}

@Composable
private fun PricePair(monthly: String, yearly: String, note: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PriceCard(monthly, "/ month", Modifier.weight(1f))
        PriceCard(yearly, "/ year", Modifier.weight(1f))
    }
    if (note.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Text(note, color = AiColors.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun PriceCard(price: String, period: String, modifier: Modifier) {
    Column(modifier.softCard(radius = 20.dp).padding(16.dp)) {
        Text(price, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Text(period, color = AiColors.Muted, fontSize = 13.sp)
    }
}
