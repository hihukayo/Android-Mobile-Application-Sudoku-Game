package com.example.sudoku.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.data.ApiClient
import com.example.sudoku.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    username: String,
    phone: String,
    refreshTick: Int,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val sc = LocalSudokuColors.current
    val context = LocalContext.current
    var avatar by remember { mutableStateOf<Bitmap?>(null) }
    var totalGames by remember { mutableStateOf(0) }
    var totalScore by remember { mutableStateOf(0) }
    var winRate by remember { mutableStateOf(0.0) }
    var contribMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var statsLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // 加载头像：先本地缓存，再从服务器同步（跨设备/跨 App 恢复）
    LaunchedEffect(Unit) {
        val local = Session.getAvatar(username)
        if (local != null) {
            avatar = BitmapFactory.decodeByteArray(local, 0, local.size)
        }
        try {
            val res = withContext(Dispatchers.IO) { ApiClient.getAvatar(username) }
            val serverAvatar = res.optString("avatar", "")
            if (serverAvatar.isNotEmpty()) {
                val bytes = android.util.Base64.decode(serverAvatar, android.util.Base64.NO_WRAP)
                avatar = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Session.setAvatar(username, bytes)
            }
        } catch (_: Exception) {
        }
    }

    // 加载统计
    LaunchedEffect(refreshTick) {
        statsLoading = true
        try {
            val res = withContext(Dispatchers.IO) { ApiClient.getUserStats(username) }
            if (res.optBoolean("success")) {
                totalGames = res.optInt("totalGames", 0)
                totalScore = res.optInt("totalScore", 0)
                winRate = res.optDouble("winRate", 0.0)
            }
        } catch (_: Exception) {
        } finally {
            statsLoading = false
        }
        try {
            val res = withContext(Dispatchers.IO) { ApiClient.getContributions(username, 365) }
            if (res.optBoolean("success")) {
                val arr = res.optJSONArray("data")
                val map = HashMap<String, Int>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i)
                        map[o.optString("date")] = o.optInt("count", 0)
                    }
                }
                contribMap = map
            }
        } catch (_: Exception) {
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val bmp = decodeSampled(bytes, 512)
                    val out = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    val saved = out.toByteArray()
                    Session.setAvatar(username, saved)
                    avatar = bmp
                    // 上传服务器同步
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                ApiClient.uploadAvatar(username, android.util.Base64.encodeToString(saved, android.util.Base64.NO_WRAP))
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 头像 + 相机角标（设置图标悬浮右上角，不占位）
        Box(Modifier.fillMaxWidth()) {
            // 头像 + 相机角标（组合居中，角标探出头像右下角）
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(88.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Blue)
                        .clickable {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatar != null) {
                        Image(
                            bitmap = avatar!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            username.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                Box(
                    Modifier
                        .padding(3.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(sc.chipBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.CameraAlt, contentDescription = null, tint = sc.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
            // 设置图标（右上角悬浮，不占位）
            Icon(
                AppIcons.Settings,
                contentDescription = "设置",
                tint = sc.textSecondary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(26.dp)
                    .clickable(onClick = onOpenSettings),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(username, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = sc.textPrimary)
        Spacer(Modifier.height(24.dp))

        // 统计卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = sc.surfaceAlt),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatItem(AppIcons.SportsEsports, "总局数", if (statsLoading) "..." else "$totalGames", Blue)
                StatDivider()
                StatItem(AppIcons.EmojiEvents, "总积分", if (statsLoading) "..." else "$totalScore", Color(0xFFE65100))
                StatDivider()
                StatItem(AppIcons.TrendingUp, "胜率", if (statsLoading) "..." else String.format("%.1f%%", winRate), winRateColor(winRate))
            }
        }
        Spacer(Modifier.height(12.dp))

        // 完成日历（GitHub 贡献图风格）
        ContributionCard(contribMap)
        Spacer(Modifier.height(20.dp))


        // 退出登录
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = sc.danger,
                contentColor = sc.onPrimary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Icon(AppIcons.Logout, contentDescription = null, tint = sc.onPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("退出登录", fontSize = 15.sp, color = sc.onPrimary)
        }
    }
}

@Composable
private fun ContributionCard(map: Map<String, Int>) {
    val sc = LocalSudokuColors.current
    val fontScale = LocalDensity.current.fontScale
    val dark = when (AppThemeMode.value) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    // GitHub 风格配色：深色模式用更亮的绿色，深底上更清晰
    val emptyColor = if (dark) Color(0xFF2D333B) else Color(0xFFEBEDF0)
    val levels = if (dark) {
        listOf(Color(0xFF0F5D30), Color(0xFF1B8A41), Color(0xFF2EA44F), Color(0xFF3FB950))
    } else {
        listOf(Color(0xFF9BE9A8), Color(0xFF40C463), Color(0xFF30A14E), Color(0xFF216E39))
    }
    fun cellColor(count: Int): Color = when {
        count <= 0 -> emptyColor
        count == 1 -> levels[0]
        count <= 3 -> levels[1]
        count <= 6 -> levels[2]
        else -> levels[3]
    }

    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayCal = Calendar.getInstance()
    val start = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -364)
        add(Calendar.DAY_OF_YEAR, -(get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
    }
    val weeks = 53
    val total = map.values.sum()
    val pitch = 17
    val scrollState = rememberScrollState()
    // 首次进入滚到最右，展示当前月份（与 GitHub 一致）
    LaunchedEffect(Unit) {
        withFrameNanos { }
        scrollState.scrollTo(scrollState.maxValue)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = sc.surfaceAlt),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("完成日历", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = sc.textPrimary)
                Spacer(Modifier.weight(1f))
                Text("近 1 年共 $total 局", fontSize = 11.sp, color = sc.textFaint)
                Spacer(Modifier.width(10.dp))
Text("少", fontSize = (10 / fontScale).sp, lineHeight = (10 / fontScale).sp, color = sc.textFaint)
                Spacer(Modifier.width(3.dp))
                for (c in listOf(emptyColor) + levels) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    Spacer(Modifier.width(2.dp))
                }
                Spacer(Modifier.width(3.dp))
Text("多", fontSize = (10 / fontScale).sp, lineHeight = (10 / fontScale).sp, color = sc.textFaint)
            }
            Spacer(Modifier.height(12.dp))
            Row {
                // 左侧星期标签（固定，日 二 四 六）
                Column(Modifier.width(22.dp)) {
                    Spacer(Modifier.height(20.dp))
                    for (row in 0 until 7) {
                        val label = when (row) {
                            0 -> "日"
                            2 -> "二"
                            4 -> "四"
                            6 -> "六"
                            else -> ""
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(17.dp),
                            contentAlignment = Alignment.Center,
                        ) {
if (label.isNotEmpty()) Text(label, fontSize = (11 / fontScale).sp, lineHeight = (11 / fontScale).sp, color = sc.textFaint, maxLines = 1)
                        }
                    }
                }
                // 右侧：整年 53 周，横向可翻阅（月份标签与网格同步滚动）
                Row(Modifier.horizontalScroll(scrollState)) {
                    Column {
                        Row {
                            var w = 0
                            while (w < weeks) {
                                val colStart = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, w * 7) }
                                val m = colStart.get(Calendar.MONTH)
                                var end = w
                                while (end + 1 < weeks) {
                                    val next = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, (end + 1) * 7) }
                                    if (next.get(Calendar.MONTH) != m) break
                                    end++
                                }
                                val span = end - w + 1
                                Box(
                                    Modifier
.width(maxOf(span * pitch - 3, 36).dp)
                                        .height(16.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
Text("${m + 1}月", fontSize = (11 / fontScale).sp, lineHeight = (11 / fontScale).sp, color = sc.textFaint, maxLines = 1)
                                }
                                w = end + 1
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row {
                            for (w in 0 until weeks) {
                                Column {
                                    for (row in 0 until 7) {
                                        val day = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, w * 7 + row) }
                                        val count = map[fmt.format(day.time)] ?: 0
                                        if (day.after(todayCal)) continue
                                        Box(
                                            Modifier
                                                .size(14.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(cellColor(count)),
                                        )
                                        if (row < 6) Spacer(Modifier.height(3.dp))
                                    }
                                }
                                if (w < weeks - 1) Spacer(Modifier.width(3.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodeSampled(bytes: ByteArray, maxSize: Int): Bitmap {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    var sample = 1
    while (opts.outWidth / sample > maxSize || opts.outHeight / sample > maxSize) {
        sample *= 2
    }
    val decode = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decode) ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val w = bmp.width
    val h = bmp.height
    val side = if (w > h) h else w
    val cropped = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side)
    if (cropped !== bmp) bmp.recycle()
    return if (side > maxSize) Bitmap.createScaledBitmap(cropped, maxSize, maxSize, true) else cropped
}

@Composable
private fun winRateColor(rate: Double): Color {
    val sc = LocalSudokuColors.current
    return when {
        rate >= 70 -> sc.userInput
        rate >= 40 -> Blue
        rate > 0 -> Color(0xFFFFA726)
        else -> sc.textFaint
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    val sc = LocalSudokuColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = sc.textFaint)
    }
}

@Composable
private fun StatDivider() {
    val sc = LocalSudokuColors.current
    Box(Modifier.width(1.dp).height(40.dp).background(sc.divider))
}
