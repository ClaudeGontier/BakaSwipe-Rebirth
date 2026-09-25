package com.bakaswipe.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private data class Dest(val label: String, val icon: ImageVector)

private val DESTS = listOf(
    Dest("Swipe", Icons.Filled.Favorite),
    Dest("Listes", Icons.AutoMirrored.Filled.List),
    Dest("Stats", Icons.Filled.Star),
    Dest("Réglages", Icons.Filled.Settings),
)

@Composable
fun App(vm: AppViewModel) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.toast) {
        val t = vm.toast
        if (t != null) {
            vm.toast = null
            snackbar.showSnackbar(t)
        }
    }

    if (!vm.loggedIn) {
        LoginScreen(vm)
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                DESTS.forEachIndexed { i, d ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> SwipeScreen(vm)
                1 -> ListScreen(vm)
                2 -> StatsScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}
