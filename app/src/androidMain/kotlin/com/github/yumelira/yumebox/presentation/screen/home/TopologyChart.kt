package com.github.yumelira.yumebox.presentation.screen.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.domain.model.Connection
import top.yukonga.miuix.kmp.theme.MiuixTheme
import timber.log.Timber
import kotlin.math.max
import kotlin.math.log10

@Composable
fun TopologyChart(
    connections: List<Connection>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val colorScheme = MiuixTheme.colorScheme
    val sankeyData: SankeyData = remember(connections) {
        runCatching { processConnections(connections) }
            .onFailure { Timber.e(it, "TopologyChart: failed to process connections") }
            .getOrElse { SankeyData(emptyList(), emptyList()) }
    }

    if (sankeyData.nodes.isEmpty()) {
        Box(modifier = modifier.height(200.dp).fillMaxWidth()) {
            // Empty state
        }
        return
    }

    Canvas(modifier = modifier.height(300.dp).fillMaxWidth().clickable { onClick() }) {
        runCatching {
            val width = size.width
            val height = size.height
            val layerWidth = width / 4
            val nodeWidth = 12.dp.toPx()
            val nodeGap = 8.dp.toPx()
            // 1. Calculate Node Values
            sankeyData.nodes.forEach {
                it.inValue = 0f
                it.outValue = 0f
            }
            sankeyData.links.forEach { link ->
                val source = sankeyData.nodes.find { it.id == link.source }
                val target = sankeyData.nodes.find { it.id == link.target }
                if (source != null && target != null) {
                    source.outValue += link.value
                    target.inValue += link.value
                }
            }
            sankeyData.nodes.forEach {
                it.value = max(it.inValue, it.outValue)
                if (it.value < 1f) it.value = 1f // Min value
            }
            // 2. Layout Nodes
            val layers = sankeyData.nodes.groupBy { it.layer }
            // Calculate scale factor
            val maxLayerTotalValue = layers.values.maxOf { layerNodes -> layerNodes.sumOf { it.value.toDouble() } }.toFloat()
            val maxLayerNodeCount = layers.values.maxOf { it.size }
            val availableHeight = height * 0.85f // Leave some padding
            val totalGapHeight = (maxLayerNodeCount - 1) * nodeGap
            // Avoid division by zero
            val scaleFactor = if (maxLayerTotalValue > 0) (availableHeight - totalGapHeight) / maxLayerTotalValue else 1f
            layers.forEach { (layerIndex, nodes) ->
                val layerTotalHeight = nodes.sumOf { (it.value * scaleFactor).toDouble() }.toFloat() + (nodes.size - 1) * nodeGap
                var currentY = (height - layerTotalHeight) / 2
                nodes.forEach { node ->
                    node.x = layerIndex * layerWidth
                    node.y = currentY
                    node.height = node.value * scaleFactor
                    currentY += node.height + nodeGap
                }
            }
            // 3. Draw Links (Ribbons)
            val nodeOutY = mutableMapOf<Int, Float>()
            val nodeInY = mutableMapOf<Int, Float>()
            sankeyData.nodes.forEach {
                nodeOutY[it.id] = it.y
                nodeInY[it.id] = it.y
            }
            // Sort links to minimize crossing
            // Sort by source layer, then source Y, then target Y
            val sortedLinks = sankeyData.links.sortedWith(compareBy<Link> {
                sankeyData.nodes.find { n -> n.id == it.source }?.layer ?: 0
            }.thenBy {
                sankeyData.nodes.find { n -> n.id == it.source }?.y ?: 0f
            }.thenBy {
                sankeyData.nodes.find { n -> n.id == it.target }?.y ?: 0f
            })
            sortedLinks.forEach { link ->
                val source = sankeyData.nodes.find { it.id == link.source }
                val target = sankeyData.nodes.find { it.id == link.target }
                if (source != null && target != null) {
                    val linkHeight = link.value * scaleFactor
                    val startY = nodeOutY[source.id]!!
                    val endY = nodeInY[target.id]!!
                    val startX = source.x + nodeWidth
                    val endX = target.x
                    // Draw Ribbon
                    val path = Path().apply {
                        moveTo(startX, startY)
                        cubicTo(
                            startX + (endX - startX) / 2, startY,
                            startX + (endX - startX) / 2, endY,
                            endX, endY
                        )
                        lineTo(endX, endY + linkHeight)
                        cubicTo(
                            startX + (endX - startX) / 2, endY + linkHeight,
                            startX + (endX - startX) / 2, startY + linkHeight,
                            startX, startY + linkHeight
                        )
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                getColorForLayer(source.layer).copy(alpha = 0.4f),
                                getColorForLayer(target.layer).copy(alpha = 0.4f)
                            ),
                            startX = startX,
                            endX = endX
                        )
                    )
                    nodeOutY[source.id] = startY + linkHeight
                    nodeInY[target.id] = endY + linkHeight
                }
            }
            // 4. Draw Nodes and Text
            sankeyData.nodes.forEach { node ->
                drawRect(
                    color = getColorForLayer(node.layer),
                    topLeft = Offset(node.x, node.y),
                    size = Size(nodeWidth, node.height)
                )
                val textLayoutResult = textMeasurer.measure(
                    text = node.name,
                    style = TextStyle(fontSize = 10.sp, color = colorScheme.onSurface)
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        node.x + nodeWidth + 5f, 
                        node.y + (node.height - textLayoutResult.size.height) / 2
                    )
                )
            }
        }.onFailure { Timber.e(it, "TopologyChart: drawing failed") }
    }
}

private fun getColorForLayer(layer: Int): Color {
    return when (layer) {
        0 -> Color(0xFF6A6FC5)
        1 -> Color(0xFFA8D4A0)
        2 -> Color(0xFFFDDB8A)
        3 -> Color(0xFFF2A0A0)
        else -> Color.Gray
    }
}

private data class Node(
    val id: Int,
    val name: String,
    val layer: Int,
    var value: Float = 0f,
    var inValue: Float = 0f,
    var outValue: Float = 0f,
    var x: Float = 0f,
    var y: Float = 0f,
    var height: Float = 0f
)

private data class Link(
    val source: Int,
    val target: Int,
    val value: Float
)

private data class SankeyData(
    val nodes: List<Node>,
    val links: List<Link>
)

private fun processConnections(connections: List<Connection>): SankeyData {
    val nodeMap = mutableMapOf<String, Int>()
    val nodes = mutableListOf<Node>()
    val linkMap = mutableMapOf<String, Int>()
    var nodeIndex = 0

    fun addNode(name: String, layer: Int): Int {
        val safeName = name.ifBlank { "<unknown>" }
        val key = "$layer-$safeName"
        if (!nodeMap.containsKey(key)) {
            nodeMap[key] = nodeIndex
            nodes.add(Node(nodeIndex, safeName, layer))
            nodeIndex++
        }
        return nodeMap[key]!!
    }

    connections.forEach { conn ->
        val sourceIP = conn.metadata.sourceIP
        val rulePayload = if (conn.rulePayload.isNotEmpty()) "${conn.rule}: ${conn.rulePayload}" else conn.rule
        val chains = conn.chains
        if (chains.isNotEmpty()) {
            val chainLast = chains.last()
            val chainFirst = chains.first()
            val sourceNode = addNode(sourceIP, 0)
            val ruleNode = addNode(rulePayload, 1)
            if (chainFirst == chainLast) {
                val chainExitNode = addNode(chainFirst, 3)
                val link1 = "$sourceNode-$ruleNode"
                val link2 = "$ruleNode-$chainExitNode"
                linkMap[link1] = (linkMap[link1] ?: 0) + 1
                linkMap[link2] = (linkMap[link2] ?: 0) + 1
            } else {
                val chainLastNode = addNode(chainLast, 2)
                val chainFirstNode = addNode(chainFirst, 3)
                val link1 = "$sourceNode-$ruleNode"
                val link2 = "$ruleNode-$chainLastNode"
                val link3 = "$chainLastNode-$chainFirstNode"
                linkMap[link1] = (linkMap[link1] ?: 0) + 1
                linkMap[link2] = (linkMap[link2] ?: 0) + 1
                linkMap[link3] = (linkMap[link3] ?: 0) + 1
            }
        }
    }
    // Sort nodes by name within layers
    val sortedNodes = nodes.sortedWith(compareBy<Node> { it.layer }.thenBy { it.name })
    val links = linkMap.map { (key, value) ->
        val parts = key.split("-")
        val source = parts[0].toInt()
        val target = parts[1].toInt()
        val scaledValue = log10(value.toDouble() + 1) * 10
        Link(source, target, scaledValue.toFloat())
    }
    return SankeyData(sortedNodes, links)
}
