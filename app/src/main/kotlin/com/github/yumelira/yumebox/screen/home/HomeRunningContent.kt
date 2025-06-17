package com.github.yumelira.yumebox.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import com.github.yumelira.yumebox.domain.model.TrafficData

/**
 * 运行中内容区域（节点/IP/速度图表）。
 * TrafficDisplay 已移至 Home.kt 统一处理，此组件仅保留节点信息部分。
 */
@Composable
fun HomeRunningContent(
    trafficNow: TrafficData,
    profileName: String?,
    tunnelMode: TunnelState.Mode?,
    serverName: String?,
    serverPing: Int?,
    ipMonitoringState: IpMonitoringState,
    speedHistory: List<Long>,
    onChartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NodeInfoDisplay(
                serverName = serverName, serverPing = serverPing
            )

            IpInfoDisplay(state = ipMonitoringState)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeedChart(
                speedHistory = speedHistory, onClick = onChartClick
            )
        }
    }
}

