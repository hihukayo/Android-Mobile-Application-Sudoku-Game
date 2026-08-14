package com.example.sudoku.ui

import androidx.activity.compose.BackHandler
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

@Composable
fun AppRoot() {
    // Session.init 已在 MainActivity.onCreate 同步完成，无需转圈闪屏，直接进入登录页/首页（启动更丝滑）
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
        when (val s = stack.last()) {
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
                    stack.clear()
                    stack.add(Screen.Login())
                },
            )
            is Screen.Settings -> SettingsScreen(
                username = s.username,
                phone = s.phone,
                onBack = { stack.removeAt(stack.lastIndex) },
                onSessionEnd = {
                    stack.clear()
                    stack.add(Screen.Login())
                },
            )
        }
    }
}

