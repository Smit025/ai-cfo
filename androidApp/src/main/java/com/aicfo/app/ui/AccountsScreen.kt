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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.app.theme.parseHex
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.sync.SyncCode
import com.aicfo.shared.sync.SyncTrigger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(controller: AiCfoController, tick: Int) {
    val freshnessTick = rememberFreshnessTick()
    val model = remember(tick, freshnessTick) { controller.accounts() }
    PullToRefreshBox(
        isRefreshing = model.syncCode == SyncCode.SYNCING,
        onRefresh = { controller.refreshAccounts(SyncTrigger.PullToRefresh) },
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
                testTag = AutomationTags.ACCOUNTS_PULL
            },
    ) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(model.title, color = AiColors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(model.subtitle, color = AiColors.Muted, fontSize = 14.sp)
            if (model.freshnessLabel.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FreshnessLine(
                    label = model.freshnessLabel,
                    action = model.syncActionLabel,
                    code = model.syncCode,
                    onAction = {
                        when (model.syncCode) {
                            SyncCode.NEEDS_REAUTH -> controller.reconnectBank()
                            SyncCode.FAILED -> controller.refreshAccounts(SyncTrigger.Manual)
                        }
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().softCard(radius = 18.dp).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = AiColors.Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(10.dp))
                Text(model.trust, color = AiColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (!model.linked) {
            item {
                Column(Modifier.fillMaxWidth().softCard().padding(18.dp)) {
                    Text(model.emptyTitle, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(model.emptyBody, color = AiColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    if (model.linkError.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(model.linkError, color = AiColors.Danger, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(model.emptyCta) { controller.connectReadOnlyStub() }
                }
            }
        } else {
            for (groupIndex in 0 until model.groupCount()) {
                val group = model.groupAt(groupIndex)
                item(key = group.title) {
                    SectionLabel(group.title)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (index in 0 until group.accountCount()) {
                            val account = group.accountAt(index)
                            Row(
                                Modifier.fillMaxWidth().softCard(radius = 22.dp).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(parseHex(account.colorHex)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(account.initials, color = AiColors.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(account.name, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(account.detail, color = AiColors.Muted, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(account.balance, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(Modifier.height(4.dp))
                                    ReadOnlyChip()
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
    }
}
