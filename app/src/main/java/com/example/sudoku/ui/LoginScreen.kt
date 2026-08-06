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
fun LoginRoot(
    showRegister: Boolean,
    onGoRegister: () -> Unit,
    onBackFromRegister: () -> Unit,
    onLoggedIn: (String, String) -> Unit,
) {
    if (showRegister) {
        RegisterScreen(onBack = onBackFromRegister, onLoggedIn = onLoggedIn)
    } else {
        LoginScreen(onGoRegister = onGoRegister, onLoggedIn = onLoggedIn)
    }
}

@Composable
fun LoginScreen(onGoRegister: () -> Unit, onLoggedIn: (String, String) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var obscure by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var showServerAddress by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
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
                Text("数独", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Sudoku", style = MaterialTheme.typography.bodySmall, color = GreyBlue)
                Spacer(Modifier.height(40.dp))
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("用户名 / 手机号") },
                    leadingIcon = { Icon(AppIcons.Person, contentDescription = null) },
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
                        IconButton(onClick = { obscure = !obscure }) {
                            Icon(
                                if (obscure) AppIcons.VisibilityOff else AppIcons.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            if (account.isBlank() || password.isBlank()) {
                                snackbar.showSnackbar("请输入账号和密码")
                                return@launch
                            }
                            loading = true
                            try {
                                val res = ApiClient.login(account.trim(), password.trim())
                                if (res.optBoolean("success")) {
                                    val u = res.optString("username")
                                    val p = res.optString("phone")
                                    Session.saveLogin(u, p)
                                    onLoggedIn(u, p)
                                } else {
                                    snackbar.showSnackbar(res.optString("message", "登录失败"))
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
                        Text("登录", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onGoRegister) { Text("没有账号？去注册") }
            }
            IconButton(
                onClick = { showServerAddress = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp),
            ) {
                Icon(AppIcons.Settings, contentDescription = "服务器设置", tint = GreyBlue)
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