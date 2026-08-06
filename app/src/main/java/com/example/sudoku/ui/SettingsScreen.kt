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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.data.ApiClient
import com.example.sudoku.data.Session
import kotlinx.coroutines.launch

private fun maskPhone(phone: String): String =
    if (phone.length < 7) phone else "${phone.substring(0, 3)}****${phone.substring(phone.length - 4)}"

@Composable
fun SettingsScreen(username: String, phone: String, onBack: () -> Unit, onSessionEnd: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEditUsername by remember { mutableStateOf(false) }
    var showEditPhone by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showServerAddress by remember { mutableStateOf(false) }
    var serverAddr by remember { mutableStateOf(Session.getServerAddress().orEmpty()) }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", fontSize = 22.sp, color = Ink)
                }
                Text("设置", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            SettingCard(
                icon = AppIcons.Person,
                title = "修改用户名",
                subtitle = username,
                onClick = { showEditUsername = true },
            )
            Spacer(Modifier.height(12.dp))
            SettingCard(
                icon = AppIcons.Lock,
                title = "修改密码",
                subtitle = "******",
                onClick = { showPassword = true },
            )
            Spacer(Modifier.height(12.dp))
            SettingCard(
                icon = AppIcons.Phone,
                title = "修改手机号",
                subtitle = maskPhone(phone),
                onClick = { showEditPhone = true },
            )
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            SettingCard(
                icon = AppIcons.CloudOff,
                title = "服务器地址",
                subtitle = if (serverAddr.isEmpty()) "自动（USB/模拟器）" else serverAddr,
                onClick = { showServerAddress = true },
            )
            Spacer(Modifier.height(12.dp))
            SettingCard(
                icon = AppIcons.DeleteForever,
                title = "注销账号",
                subtitle = "永久删除所有数据",
                danger = true,
                onClick = { showDelete = true },
            )
        }
    }

    if (showEditUsername) {
        EditValueDialog(
            title = "修改用户名",
            label = "请输入新用户名",
            onDismiss = { showEditUsername = false },
            onConfirm = { value, pwd ->
                scope.launch {
                    val res = ApiClient.updateUsername(username, value.trim(), pwd)
                    snackbar.showSnackbar(res.optString("message", "操作完成"))
                }
                showEditUsername = false
            },
        )
    }
    if (showEditPhone) {
        EditValueDialog(
            title = "修改手机号",
            label = "请输入新手机号",
            phoneKeyboard = true,
            onDismiss = { showEditPhone = false },
            onConfirm = { value, pwd ->
                scope.launch {
                    val res = ApiClient.updatePhone(username, value.trim(), pwd)
                    snackbar.showSnackbar(res.optString("message", "操作完成"))
                }
                showEditPhone = false
            },
        )
    }
    if (showPassword) {
        PasswordDialog(
            onDismiss = { showPassword = false },
            onConfirm = { old, new, confirm ->
                if (new != confirm) {
                    scope.launch { snackbar.showSnackbar("两次密码不一致") }
                    return@PasswordDialog
                }
                scope.launch {
                    val res = ApiClient.updatePassword(username, old, new)
                    snackbar.showSnackbar(res.optString("message", "操作完成"))
                }
                showPassword = false
            },
        )
    }
    if (showDelete) {
        DeleteAccountDialog(
            onDismiss = { showDelete = false },
            onConfirm = { phoneInput, pwd, confirmPwd ->
                if (pwd != confirmPwd) {
                    scope.launch { snackbar.showSnackbar("两次密码不一致") }
                    return@DeleteAccountDialog
                }
                scope.launch {
                    val res = ApiClient.deleteAccount(username, phoneInput.trim(), pwd)
                    snackbar.showSnackbar(res.optString("message", "操作完成"))
                    if (res.optBoolean("success")) {
                        Session.clearLogin()
                        showDelete = false
                        onSessionEnd()
                    } else {
                        showDelete = false
                    }
                }
            },
        )
    }
    if (showServerAddress) {
        ServerAddressDialog(
            onDismiss = { showServerAddress = false },
            onConfirm = { addr ->
                Session.setServerAddress(addr)
                serverAddr = addr
                showServerAddress = false
                scope.launch { snackbar.showSnackbar(if (addr.isEmpty()) "已恢复默认连接" else "服务器地址已保存") }
            },
        )
    }
}

@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = if (danger) Red else DarkSlate, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, color = if (danger) Red else Ink, fontWeight = if (danger) FontWeight.SemiBold else FontWeight.Normal)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, fontSize = 13.sp, color = GreyBlue)
                }
            }
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ServerAddressDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(Session.getServerAddress().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器地址", fontSize = 18.sp) },
        text = {
            Column {
                Text("留空则使用默认（USB：localhost / 模拟器：10.0.2.2）", fontSize = 13.sp, color = GreyBlue)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("例如 192.168.1.100:8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun EditValueDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    phoneKeyboard: Boolean = false,
) {
    var value by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 18.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    keyboardOptions = if (phoneKeyboard) KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("当前密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value, pwd) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码", fontSize = 18.sp) },
        text = {
            Column {
                OutlinedTextField(value = old, onValueChange = { old = it }, label = { Text("当前密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = new, onValueChange = { new = it }, label = { Text("新密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("确认新密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(old, new, confirm) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("注销账号", fontSize = 18.sp, color = Red) },
        text = {
            Column {
                Text("此操作不可恢复，所有数据将被永久删除。", fontSize = 13.sp, color = Red)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("手机号") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = confirmPwd, onValueChange = { confirmPwd = it }, label = { Text("确认密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(phone, pwd, confirmPwd) },
                colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text("确认注销")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
