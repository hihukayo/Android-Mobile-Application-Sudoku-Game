package com.example.sudoku.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sudoku.ui.game.GameController
import com.example.sudoku.ui.game.GameScreen

@Composable
fun HomeScreen(
    username: String,
    phone: String,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onSessionEnd: () -> Unit,
) {
    val sc = LocalSudokuColors.current
    val gameController = remember { GameController(username) }
    var rankTick by remember { mutableIntStateOf(0) }
    var profileTick by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = sc.background, tonalElevation = 0.dp) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Blue,
                    selectedTextColor = Blue,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = sc.textFaint,
                    unselectedTextColor = sc.textFaint,
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabChange(0) },
                    icon = { Icon(AppIcons.GridOn, contentDescription = null) },
                    label = { Text("数独") },
                    colors = itemColors,
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        onTabChange(1)
                        rankTick++
                    },
                    icon = { Icon(AppIcons.EmojiEvents, contentDescription = null) },
                    label = { Text("排行榜") },
                    colors = itemColors,
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        onTabChange(2)
                        profileTick++
                    },
                    icon = { Icon(AppIcons.Person, contentDescription = null) },
                    label = { Text("我的") },
                    colors = itemColors,
                )
            }
        },
    ) { padding ->
        // 与 Flutter 版一致：整体限制最大宽度 480dp 居中
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxSize().background(sc.background),
                    transitionSpec = {
                        // 翻页式左右滑动：tab 向右点时新页从右往左滑入，向左点则相反（纯滑动最流畅）
                        val duration = 200
                        if (targetState > initialState) {
                            (slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) { it })
                                .togetherWith(slideOutHorizontally(tween(duration, easing = FastOutSlowInEasing)) { -it })
                        } else {
                            (slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) { -it })
                                .togetherWith(slideOutHorizontally(tween(duration, easing = FastOutSlowInEasing)) { it })
                        }
                    },
                    label = "tabTransition",
                ) { tab ->
                    when (tab) {
                        0 -> GameScreen(gameController)
                        1 -> RankScreen(username = username, refreshTick = rankTick)
                        else -> ProfileScreen(
                            username = username,
                            phone = phone,
                            refreshTick = profileTick,
                            onOpenSettings = onOpenSettings,
                            onLogout = onSessionEnd,
                        )
                    }
                }
            }
        }
    }
}