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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
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
import com.aicfo.shared.presentation.HomeMoveModel

@Composable
fun HomeScreen(
    controller: AiCfoController,
    tick: Int,
    wide: Boolean,
    onOpen: (String) -> Unit,
) {
    val home = remember(tick) { controller.home() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = if (wide) 480.dp else 900.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(home.greeting, color = AiColors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(home.subtitle, color = AiColors.Muted, fontSize = 15.sp)
                }
                Box {
                    Box(
                        Modifier
                            .size(44.dp)
                            .softCard(radius = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = AiColors.Text)
                    }
                    if (home.showNotificationDot) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AiColors.Danger),
                        )
                    }
                }
                Spacer(Modifier.size(10.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AiColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(home.initials, color = AiColors.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().softCard(radius = 18.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(AiColors.Accent))
                Spacer(Modifier.size(10.dp))
                Text(home.pulse, color = AiColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(home.sectionTitle, color = AiColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    home.seeAllLabel,
                    color = AiColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { controller.selectTab("MOVES") },
                )
            }
            Spacer(Modifier.height(14.dp))
            if (home.moveCount() == 0) {
                Column(Modifier.fillMaxWidth().softCard().padding(20.dp)) {
                    Text(home.emptyTitle, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(home.emptyBody, color = AiColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (index in 0 until home.moveCount()) {
                        HomeMoveCard(home.moveAt(index)) { onOpen(home.moveAt(index).id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMoveCard(move: HomeMoveModel, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .softCard()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(move.icon)
            Spacer(Modifier.weight(1f))
            ImpactPill(move.impact)
        }
        Spacer(Modifier.height(14.dp))
        Text(move.title, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(move.body, color = AiColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(12.dp))
        Text("${move.cta}  →", color = AiColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
