package com.example.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.sudoku.data.Session

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
    BackHandler(enabled = stack.size > 1) {
        stack.removeAt(stack.lastIndex)
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
                    selectedTab = homeTab,
                    onTabChange = { homeTab = it },
                    onOpenSettings = { stack.add(Screen.Settings(s.username, s.phone)) },
                    onSessionEnd = {
                        Session.autoResumeChecked = false
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
                        stack.clear()
                        stack.add(Screen.Login())
                    },
                )
            }
        }
    }
}