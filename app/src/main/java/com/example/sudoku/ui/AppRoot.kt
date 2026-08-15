package com.example.sudoku.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.example.sudoku.data.Session
import com.example.sudoku.ui.game.GameController

sealed interface Screen {
    data class Login(val showRegister: Boolean = false) : Screen
    data class Home(val username: String, val phone: String) : Screen
    data class Settings(val username: String, val phone: String) : Screen
}

/** 页面层级：数值越大越靠上层，用于判断过渡动画方向（压栈/出栈） */
private fun Screen.depth(): Int = when (this) {
    is Screen.Login -> if (showRegister) 1 else 0
    is Screen.Home -> 2
    is Screen.Settings -> 3
}

@Composable
fun AppRoot() {
    // Session.init 已在 MainActivity.onCreate 同步完成，无需转圈屏，直接进入登录页/首页（启动更丝滑）
    val initial = if (Session.username != null && Session.phone != null) {
        Screen.Home(Session.username!!, Session.phone!!)
    } else {
        Screen.Login()
    }
    val stack = remember { mutableStateListOf(initial) }
    var homeTab by rememberSaveable { mutableIntStateOf(0) }
    val tabHistory = remember { mutableStateListOf<Int>() } // tab 返回栈：记录访问过的 tab，返回键逐级回退
    var showExitConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 数独控制器提升到 AppRoot 层：进出设置页/切换页面不重建，保留当前棋局与计时
    val gameController = remember(Session.username) { GameController(Session.username ?: "") }
    DisposableEffect(gameController) {
        onDispose { gameController.dispose() }
    }
    // 返回键层级：设置/注册页→返回上一页；主页 tab→逐级回退（我的→排行榜→数独）；数独页→弹确认退出；登录页保持默认退出
    val topScreen = stack.last()
    BackHandler(enabled = stack.size > 1 || topScreen is Screen.Home) {
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            tabHistory.isNotEmpty() -> homeTab = tabHistory.removeAt(tabHistory.lastIndex)
            else -> showExitConfirm = true
        }
    }
    SudokuTheme {
        val sc = LocalSudokuColors.current
        AnimatedContent(
            targetState = stack.last(),
            modifier = Modifier.fillMaxSize().background(sc.background),
            transitionSpec = {
                // 翻页式左右滑动：压栈新页从右往左滑入，出栈新页从左往右滑入（纯滑动最流畅）
                val duration = 240
                val forward = targetState.depth() > initialState.depth()
                val enter = if (forward) {
                    slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) { it }
                } else {
                    slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) { -it }
                }
                val exit = if (forward) {
                    slideOutHorizontally(tween(duration, easing = FastOutSlowInEasing)) { -it }
                } else {
                    slideOutHorizontally(tween(duration, easing = FastOutSlowInEasing)) { it }
                }
                enter.togetherWith(exit)
            },
            label = "screenTransition",
        ) { s ->
            when (s) {
                is Screen.Login -> LoginRoot(
                    showRegister = s.showRegister,
                    onGoRegister = { stack.add(Screen.Login(showRegister = true)) },
                    onBackFromRegister = { stack.removeAt(stack.lastIndex) },
                    onLoggedIn = { u, p ->
                        stack.clear()
                        stack.add(Screen.Home(u, p))
                    },
                )
                is Screen.Home -> HomeScreen(
                    username = s.username,
                    phone = s.phone,
                    gameController = gameController,
                    selectedTab = homeTab,
                    onTabChange = { newTab ->
                        if (newTab != homeTab) {
                            tabHistory.add(homeTab)
                            homeTab = newTab
                        }
                    },
                    onOpenSettings = { stack.add(Screen.Settings(s.username, s.phone)) },
                    onSessionEnd = {
                        Session.autoResumeChecked = false
                        homeTab = 0
                        tabHistory.clear()
                        stack.clear()
                        stack.add(Screen.Login())
                    },
                )
                is Screen.Settings -> SettingsScreen(
                    username = s.username,
                    phone = s.phone,
                    onBack = { stack.removeAt(stack.lastIndex) },
                    onSessionEnd = {
                        Session.autoResumeChecked = false
                        homeTab = 0
                        tabHistory.clear()
                        stack.clear()
                        stack.add(Screen.Login())
                    },
                )
            }
        }
        if (showExitConfirm) {
            Dialog(onDismissRequest = { showExitConfirm = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = sc.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(300.dp),
                ) {
                    Column(
                        Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            AppIcons.Logout,
                            contentDescription = null,
                            tint = sc.textSecondary,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("确定要退出吗？", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = sc.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "未完成的棋局将自动保存",
                            fontSize = 13.sp,
                            color = sc.textFaint,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = { showExitConfirm = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, sc.divider),
                                contentPadding = PaddingValues(vertical = 12.dp),
                            ) {
                                Text("取消", fontSize = 15.sp, color = sc.textSecondary)
                            }
                            Button(
                                onClick = {
                                    showExitConfirm = false
                                    (context as? Activity)?.finish()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = sc.primary,
                                    contentColor = sc.onPrimary,
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                            ) {
                                Text("退出", fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}