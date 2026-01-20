package com.github.yumelira.yumebox.presentation.screen.home

import android.graphics.Region
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.domain.model.Connection
import kotlin.math.log10
import kotlin.math.max
import timber.log.Timber
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TopologyChart(
    connections: List<Connection>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val colorScheme = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val sankeyData: SankeyData = remember(connections) {
        runCatching { processConnections(connections) }
            .onFailure { Timber.e(it, "TopologyChart: failed to process connections") }
            .getOrElse { SankeyData(emptyList(), emptyList()) }
    }
    if (sankeyData.nodes.isEmpty()) {
        Box(modifier = modifier.height(200.dp).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "暂无连接或内核未运行，点击查看连接。",
                    style = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击刷新/打开连接页面",
                    style = TextStyle(fontSize = 12.sp, color = colorScheme.primary),
                    modifier = Modifier.clickable { onClick() }
                )
            }
        }
        return
    }
    var selectedLink by remember { mutableStateOf<RenderLink?>(null) }
    var tapPosition by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(modifier = modifier.height(300.dp).fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val layoutResult = remember(sankeyData, widthPx, heightPx) {
            calculateSankeyLayout(sankeyData, Size(widthPx, heightPx), textMeasurer, colorScheme.onSurface, density)
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(layoutResult) {
                    detectTapGestures(
                        onTap = { offset ->
                            val clickedLink = hitTest(layoutResult.links, offset)
                            if (clickedLink != null) {
                                selectedLink = clickedLink
                                tapPosition = offset
                            } else {
                                if (selectedLink != null) {
                                    selectedLink = null
                                } else {
                                    onClick()
                                }
                            }
                        }
                    )
                }
        ) {
            val (links, nodes) = layoutResult
            val nodeWidth = 12.dp.toPx()
            // Pre-calculate highlighted state if something is selected
            val highlightedConnections = selectedLink?.linkData?.connections?.toSet()
            links.forEach { renderLink ->
                val isHighlighted = selectedLink == null || 
                    (highlightedConnections != null && renderLink.linkData.connections.any { it in highlightedConnections })
                val alpha = if (isHighlighted) 1f else 0.1f
                drawPath(path = renderLink.path, brush = renderLink.brush, alpha = alpha)
            }
            // Pre-calculate highlighted nodes (nodes touched by any highlighted link)
            val highlightedNodeIds = if (selectedLink == null) null else {
                val activeLinks = links.filter { link -> 
                   highlightedConnections != null && link.linkData.connections.any { it in highlightedConnections }
                }
                activeLinks.flatMap { listOf(it.source.id, it.target.id) }.toSet()
            }
            nodes.forEach { (node, color, textLayout) ->
                val isHighlighted = selectedLink == null || (highlightedNodeIds != null && node.id in highlightedNodeIds)
                val nodeAlpha = if (isHighlighted) 1f else 0.2f
                drawRect(
                    color = color.copy(alpha = nodeAlpha),
                    topLeft = Offset(node.x, node.y),
                    size = Size(nodeWidth, node.height)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        node.x + nodeWidth + 5f,
                        node.y + (node.height - textLayout.size.height) / 2
                    ),
                    alpha = nodeAlpha
                )
            }
        }
        if (selectedLink != null) {
            val link = selectedLink!!
            // Simple positioning logic to ensure it stays somewhat on screen
            val xOffset = tapPosition.x.toInt()
            val yOffset = max(0, tapPosition.y.toInt() - 200) // Place above
            // Get chain info from the first connection in the link
            val exampleConnection = link.linkData.connections.firstOrNull()
            val chainText = exampleConnection?.chains?.joinToString(" → ") ?: "${link.source.name} → ${link.target.name}"
            Box(
                modifier = Modifier
                    .offset { IntOffset(xOffset, yOffset) }
                    .background(colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = chainText,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "连接数: ${link.linkData.originalCount}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

private fun calculateSankeyLayout(
    sankeyData: SankeyData,
    size: Size,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textColor: Color,
    density: Density
): SankeyLayoutResult {
    val width = size.width
    val height = size.height
    val layerWidth = width / 4
    val nodeWidth = with(density) { 12.dp.toPx() }
    val nodeGap = with(density) { 8.dp.toPx() }
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
    val maxLayerTotalValue = layers.values.maxOfOrNull { layerNodes -> layerNodes.sumOf { it.value.toDouble() } }?.toFloat() ?: 0f
    val maxLayerNodeCount = layers.values.maxOfOrNull { it.size } ?: 0
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
    // 3. Prepare Drawables (Links)
    val nodeOutY = mutableMapOf<Int, Float>()
    val nodeInY = mutableMapOf<Int, Float>()
    sankeyData.nodes.forEach {
        nodeOutY[it.id] = it.y
        nodeInY[it.id] = it.y
    }
    // Sort links to minimize crossing
    val sortedLinks = sankeyData.links.sortedWith(compareBy<Link> {
        sankeyData.nodes.find { n -> n.id == it.source }?.layer ?: 0
    }.thenBy {
        sankeyData.nodes.find { n -> n.id == it.source }?.y ?: 0f
    }.thenBy {
        sankeyData.nodes.find { n -> n.id == it.target }?.y ?: 0f
    })
    val renderLinks = sortedLinks.mapNotNull { link ->
        val source = sankeyData.nodes.find { it.id == link.source }
        val target = sankeyData.nodes.find { it.id == link.target }
        if (source != null && target != null) {
            val linkHeight = link.value * scaleFactor
            val startY = nodeOutY[source.id]!!
            val endY = nodeInY[target.id]!!
            val startX = source.x + nodeWidth
            val endX = target.x
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
            val brush = Brush.horizontalGradient(
                colors = listOf(
                    getColorForLayer(source.layer).copy(alpha = 0.4f),
                    getColorForLayer(target.layer).copy(alpha = 0.4f)
                ),
                startX = startX,
                endX = endX
            )
            nodeOutY[source.id] = startY + linkHeight
            nodeInY[target.id] = endY + linkHeight
            RenderLink(link, path, brush, source, target)
        } else null
    }
    // 4. Prepare Drawables (Nodes & Text)
    val renderNodes = sankeyData.nodes.map { node ->
        val textLayoutResult = textMeasurer.measure(
            text = node.name,
            style = TextStyle(fontSize = 10.sp, color = textColor)
        )
        RenderNode(node, getColorForLayer(node.layer), textLayoutResult)
    }
    return SankeyLayoutResult(renderLinks, renderNodes)
}

private fun hitTest(links: List<RenderLink>, tapOffset: Offset): RenderLink? {
    val region = Region()
    val clip = Region(0, 0, 10000, 10000) // Large enough clip
    return links.reversed().find { link ->
        val path = link.path.asAndroidPath()
        region.setPath(path, clip)
        region.contains(tapOffset.x.toInt(), tapOffset.y.toInt())
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
    val value: Float,
    val originalCount: Int,
    val connections: List<Connection>
)

private data class SankeyData(
    val nodes: List<Node>,
    val links: List<Link>
)

private data class RenderLink(
    val linkData: Link,
    val path: Path,
    val brush: Brush,
    val source: Node,
    val target: Node
)

private data class RenderNode(
    val node: Node,
    val color: Color,
    val textLayoutResult: TextLayoutResult
)

private data class SankeyLayoutResult(
    val links: List<RenderLink>,
    val nodes: List<RenderNode>
)

private fun processConnections(connections: List<Connection>): SankeyData {
    val nodeMap = mutableMapOf<String, Int>()
    val nodes = mutableListOf<Node>()
    val linkMap = mutableMapOf<String, MutableList<Connection>>()
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
            // Helper to add connection to link
            fun addLink(src: Int, dst: Int) {
                val linkKey = "$src-$dst"
                linkMap.getOrPut(linkKey) { mutableListOf() }.add(conn)
            }
            if (chainFirst == chainLast) {
                val chainExitNode = addNode(chainFirst, 3)
                addLink(sourceNode, ruleNode)
                addLink(ruleNode, chainExitNode)
            } else {
                val chainLastNode = addNode(chainLast, 2)
                val chainFirstNode = addNode(chainFirst, 3)
                addLink(sourceNode, ruleNode)
                addLink(ruleNode, chainLastNode)
                addLink(chainLastNode, chainFirstNode)
            }
        }
    }
    // Sort nodes by name within layers
    val sortedNodes = nodes.sortedWith(compareBy<Node> { it.layer }.thenBy { it.name })
    val links = linkMap.map { (key, connList) ->
        val parts = key.split("-")
        val source = parts[0].toInt()
        val target = parts[1].toInt()
        val value = connList.size
        val scaledValue = log10(value.toDouble() + 1) * 10
        Link(source, target, scaledValue.toFloat(), value, connList)
    }
    return SankeyData(sortedNodes, links)
}
