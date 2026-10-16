package com.example.burpeelog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.burpeelog.ui.navigation.NavTab
import com.example.burpeelog.ui.screens.DadosScreen
import com.example.burpeelog.ui.screens.HistoryScreen
import com.example.burpeelog.ui.screens.HomeScreen
import com.example.burpeelog.ui.screens.WorkoutScreen

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.INICIO) }
    var mostrandoDados by rememberSaveable { mutableStateOf(false) }

    // If on DadosScreen or a secondary tab, pressing back navigates back appropriately
    BackHandler(enabled = mostrandoDados || selectedTab != NavTab.INICIO) {
        if (mostrandoDados) {
            mostrandoDados = false
        } else {
            selectedTab = NavTab.INICIO
        }
    }

    if (mostrandoDados) {
        DadosScreen(
            onNavigateBack = { mostrandoDados = false },
            modifier = modifier
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            modifier = Modifier.testTag(tab.testTag),
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = androidx.compose.ui.res.stringResource(tab.titleRes)
                                )
                            },
                            label = {
                                Text(text = androidx.compose.ui.res.stringResource(tab.titleRes))
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = selectedTab,
                label = "tab_crossfade",
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) { tab ->
                when (tab) {
                    NavTab.INICIO -> HomeScreen(
                        onIniciarTreino = { selectedTab = NavTab.TREINO },
                        onAbrirDados = { mostrandoDados = true }
                    )
                    NavTab.TREINO -> WorkoutScreen()
                    NavTab.HISTORICO -> HistoryScreen()
                }
            }
        }
    }
}
