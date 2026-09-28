package com.aicfo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
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
import com.aicfo.shared.domain.AiCfoController

@Composable
fun LockScreen(controller: AiCfoController, tick: Int, onBiometric: () -> Unit) {
    val model = remember(tick) { controller.lock() }
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AiColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = AiColors.Accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text(model.title, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(Modifier.height(10.dp))
            Text(model.body, color = AiColors.Muted, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(28.dp))
            PrimaryButton(model.primaryCta) {
                if (model.hardwareAvailable) onBiometric() else controller.unlockWithoutHardware()
            }
        }
    }
}
