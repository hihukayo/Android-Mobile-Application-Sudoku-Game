package com.example.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.data.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class RankItem(
    val username: String,
    val total: Int,
    val completed: Int,
    val totalScore: Int,
    val winRate: Double,
)

@Composable
fun RankScreen(username: String, refreshTick: Int) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<RankItem>()) }
    var retryTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick, retryTick) {
        loading = true
        error = ""
        try {
            val res = withContext(Dispatchers.IO) { ApiClient.getRankList() }
            if (res.optBoolean("success")) {
                val arr = res.optJSONArray("data") ?: org.json.JSONArray()
                val list = mutableListOf<RankItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        RankItem(
                            username = o.optString("username", ""),
                            total = o.optInt("total", 0),
                            completed = o.optInt("completed", 0),
                            totalScore = o.optInt("totalScore", 0),
                            winRate = o.optDouble("winRate", 0.0),
                        )
                    )
                }
                items = list
            } else {
                error = res.optString("message", "加载失败")
            }
        } catch (_: Exception) {
            error = "网络错误，请稍后重试"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 标题栏
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F7FA))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(4.dp))
                Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                    Text("排名", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GreyBlue)
                }
                Spacer(Modifier.width(4.dp))
                Text("玩家", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GreyBlue, modifier = Modifier.weight(1f))
                Text("积分", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate, modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("胜率", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GreyBlue, modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        HorizontalDivider(thickness = 1.dp)

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(AppIcons.CloudOff, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = Color(0xFF757575))
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { retryTick++ }) {
                        Icon(AppIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重试")
                    }
                }
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(AppIcons.EmojiEvents, contentDescription = null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("暂无排行数据", fontSize = 16.sp, color = Color(0xFF9E9E9E))
                    Spacer(Modifier.height(4.dp))
                    Text("完成一局游戏后数据将自动记录", fontSize = 13.sp, color = Color(0xFFBDBDBD))
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(items) { index, item ->
                    RankItemRow(rank = index + 1, item = item, isMe = item.username == username)
                }
            }
        }
    }
}

@Composable
private fun RankItemRow(rank: Int, item: RankItem, isMe: Boolean) {
    val bg = if (isMe) Color(0xFFF0F4FF) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(4.dp))
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            RankBadge(rank, isMe)
        }
        Spacer(Modifier.width(4.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.username,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isMe) Blue else Color(0xDD000000),
            )
            if (isMe) {
                Spacer(Modifier.width(6.dp))
                Text("我", fontSize = 12.sp, color = Blue, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            formatScore(item.totalScore),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkSlate,
            modifier = Modifier.width(72.dp),
        )
        Text(
            "${String.format("%.1f", item.winRate)}%",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = winRateColor(item.winRate),
            modifier = Modifier.width(64.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun RankBadge(rank: Int, isMe: Boolean) {
    when (rank) {
        1 -> Icon(AppIcons.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(26.dp))
        2 -> Icon(AppIcons.EmojiEvents, contentDescription = null, tint = Color(0xFFC0C0C0), modifier = Modifier.size(26.dp))
        3 -> Icon(AppIcons.EmojiEvents, contentDescription = null, tint = Color(0xFFCD7F32), modifier = Modifier.size(26.dp))
        else -> Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isMe) Blue else Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$rank",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMe) Color.White else DarkSlate,
            )
        }
    }
}

private fun formatScore(s: Int): String = when {
    s >= 1000000 -> String.format("%.1fm", s / 1000000.0)
    s >= 1000 -> String.format("%.1fk", s / 1000.0)
    else -> "$s"
}

private fun winRateColor(rate: Double): Color = when {
    rate >= 70 -> Color(0xFF2E7D32)
    rate >= 40 -> Blue
    rate > 0 -> Color(0xFFE65100)
    else -> Color(0xFF9E9E9E)
}
