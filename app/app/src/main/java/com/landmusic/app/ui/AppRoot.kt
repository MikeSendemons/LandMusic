package com.landmusic.app.ui

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.landmusic.app.LandMusicApp
import com.landmusic.app.ui.components.MiniPlayer

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "主页", Icons.Rounded.Home),
    Tab("library", "资料库", Icons.Rounded.LibraryMusic),
    Tab("search", "搜索", Icons.Rounded.Search),
)

@Composable
fun AppRoot(
    hasAllFilesAccess: Boolean,
    onGrantAllFiles: () -> Unit,
) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as LandMusicApp
    val playerManager = app.playerManager
    val viewModel: LibraryViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentSong by playerManager.currentSong.collectAsState()

    val openPlayer = { navController.navigate("player") }

    Scaffold(
        bottomBar = {
            if (currentRoute != "player") {
                Column {
                    if (currentSong != null) {
                        MiniPlayer(
                            playerManager = playerManager,
                            onExpand = openPlayer,
                        )
                    }
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    playerManager = playerManager,
                    hasAllFilesAccess = hasAllFilesAccess,
                    onGrantAllFiles = onGrantAllFiles,
                    onOpenPlayer = openPlayer,
                )
            }
            composable("library") {
                LibraryScreen(
                    viewModel = viewModel,
                    playerManager = playerManager,
                    onOpenPlayer = openPlayer,
                )
            }
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    playerManager = playerManager,
                    onOpenPlayer = openPlayer,
                )
            }
            composable(
                route = "player",
                enterTransition = { slideInVertically(initialOffsetY = { it }) },
                exitTransition = { slideOutVertically(targetOffsetY = { it }) },
                popEnterTransition = { slideInVertically(initialOffsetY = { it }) },
                popExitTransition = { slideOutVertically(targetOffsetY = { it }) },
            ) {
                PlayerScreen(
                    playerManager = playerManager,
                    onCollapse = { navController.popBackStack() },
                )
            }
        }
    }
}
