package com.example.sudoku.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sudoku.data.ApiClient
import com.example.sudoku.data.Session
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(onBack: () -> Unit, onLoggedIn: (String, String) -> Unit) {
    val sc = LocalSudokuColors.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var obscurePwd by remember { mutableStateOf(true) }
    var obscureConfirm by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var showServerAddress by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 400.dp)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(AppIcons.PersonAdd, contentDescription = null, modifier = Modifier.size(48.dp), tint = Blue)
                Spacer(Modifier.height(8.dp))
                Text("创建账号", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(40.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    leadingIcon = { Icon(AppIcons.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("手机号") },
                    leadingIcon = { Icon(AppIcons.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    leadingIcon = { Icon(AppIcons.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { obscurePwd = !obscurePwd }) {
                            Icon(
                                if (obscurePwd) AppIcons.VisibilityOff else AppIcons.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    visualTransformation = if (obscurePwd) PasswordVisualTransformation() else VisualTransformation.None,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("确认密码") },
                    leadingIcon = { Icon(AppIcons.LockOutline, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { obscureConfirm = !obscureConfirm }) {
                            Icon(
                                if (obscureConfirm) AppIcons.VisibilityOff else AppIcons.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    visualTransformation = if (obscureConfirm) PasswordVisualTransformation() else VisualTransformation.None,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            if (username.isBlank() || phone.isBlank() || password.isBlank() || confirm.isBlank()) {
                                snackbar.showSnackbar("请填写所有字段")
                                return@launch
                            }
                            if (!Regex("^\\d{11}$").matches(phone.trim())) {
                                snackbar.showSnackbar("手机号格式不正确（需 11 位数字）")
                                return@launch
                            }
                            if (password != confirm) {
                                snackbar.showSnackbar("两次密码不一致")
                                return@launch
                            }
                            if (password.length < 6) {
                                snackbar.showSnackbar("密码至少 6 位")
                                return@launch
                            }
                            loading = true
                            try {
                                val res = ApiClient.register(username.trim(), phone.trim(), password)
                                if (res.optBoolean("success")) {
                                    snackbar.showSnackbar("注册成功，请登录")
                                    onBack()
                                } else {
                                    snackbar.showSnackbar(res.optString("message", "注册失败"))
                                }
                            } catch (_: Exception) {
                                snackbar.showSnackbar("连接失败，请稍后重试")
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("注册")
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack) { Text("已有账号？去登录") }
            }
            IconButton(
                onClick = { showServerAddress = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp),
            ) {
                Icon(AppIcons.Settings, contentDescription = "服务器设置", tint = sc.textFaint)
            }
        }
    }

    if (showServerAddress) {
        ServerAddressDialog(
            onDismiss = { showServerAddress = false },
            onConfirm = { addr ->
                Session.setServerAddress(addr)
                showServerAddress = false
                scope.launch {
                    snackbar.showSnackbar(if (addr.isEmpty()) "已恢复默认连接" else "服务器地址已保存")
                }
            },
        )
    }
}