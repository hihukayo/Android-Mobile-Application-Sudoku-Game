package com.example.sudoku.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.data.ApiClient
import com.example.sudoku.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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
        // 头像 + 相机角标
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier
                    .size(88.dp)
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

        // 操作菜单
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Settings, contentDescription = null, tint = sc.textSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text("设置", fontSize = 15.sp, modifier = Modifier.weight(1f))
                Icon(AppIcons.ChevronRight, contentDescription = null, tint = sc.textFaint, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(32.dp))

        // 退出登录
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(AppIcons.Logout, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("退出登录", fontSize = 15.sp, color = Color(0xFFEF5350))
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
