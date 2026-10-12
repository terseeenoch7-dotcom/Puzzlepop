package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GameRepository
import com.example.game.GameViewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WalletScreen

sealed class ScreenState {
    object Home : ScreenState()
    object Wallet : ScreenState()
    object Profile : ScreenState()
    object Admin : ScreenState()
    data class Gameplay(val levelNumber: Int) : ScreenState()
}

@Composable
fun MainAppContainer(
    gameViewModel: GameViewModel = viewModel()
) {
    val repository = gameViewModel.repository
    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Home) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is ScreenState.Gameplay -> {
                // Immersive gameplay environment: Completely hides normal navigation and bottom bar
                GameScreen(
                    viewModel = gameViewModel,
                    onNavigateHome = { currentScreen = ScreenState.Home },
                    onNavigateWallet = { currentScreen = ScreenState.Wallet }
                )
            }
            is ScreenState.Admin -> {
                AdminDashboardScreen(
                    repository = repository,
                    onNavigateBack = { currentScreen = ScreenState.Home },
                    onTestLevel = { lvl ->
                        gameViewModel.loadLevel(lvl)
                        currentScreen = ScreenState.Gameplay(lvl)
                    }
                )
            }
            else -> {
                // Normal App Shell with 3-tab Bottom Navigation (Home, Wallet, Profile)
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF0F172A),
                            modifier = Modifier
                                .navigationBarsPadding()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF334155),
                                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                )
                                .testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = screen is ScreenState.Home,
                                onClick = { currentScreen = ScreenState.Home },
                                icon = {
                                    Icon(
                                        imageVector = if (screen is ScreenState.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Home",
                                        fontWeight = if (screen is ScreenState.Home) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF0F172A),
                                    selectedTextColor = Color(0xFFFDE047),
                                    indicatorColor = Color(0xFFFBBF24),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag("nav_home")
                            )

                            NavigationBarItem(
                                selected = screen is ScreenState.Wallet,
                                onClick = { currentScreen = ScreenState.Wallet },
                                icon = {
                                    Icon(
                                        imageVector = if (screen is ScreenState.Wallet) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = "Wallet"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Wallet",
                                        fontWeight = if (screen is ScreenState.Wallet) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF0F172A),
                                    selectedTextColor = Color(0xFFFDE047),
                                    indicatorColor = Color(0xFFFBBF24),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag("nav_wallet")
                            )

                            NavigationBarItem(
                                selected = screen is ScreenState.Profile,
                                onClick = { currentScreen = ScreenState.Profile },
                                icon = {
                                    Icon(
                                        imageVector = if (screen is ScreenState.Profile) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Profile",
                                        fontWeight = if (screen is ScreenState.Profile) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF0F172A),
                                    selectedTextColor = Color(0xFFFDE047),
                                    indicatorColor = Color(0xFFFBBF24),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag("nav_profile")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (screen) {
                            ScreenState.Home -> HomeScreen(
                                repository = repository,
                                onStartGame = { lvl ->
                                    gameViewModel.loadLevel(lvl)
                                    currentScreen = ScreenState.Gameplay(lvl)
                                },
                                onNavigateWallet = { currentScreen = ScreenState.Wallet },
                                onNavigateProfile = { currentScreen = ScreenState.Profile },
                                onNavigateAdmin = { currentScreen = ScreenState.Admin }
                            )
                            ScreenState.Wallet -> WalletScreen(
                                repository = repository,
                                onNavigatePlay = {
                                    val lvl = 1
                                    gameViewModel.loadLevel(lvl)
                                    currentScreen = ScreenState.Gameplay(lvl)
                                }
                            )
                            ScreenState.Profile -> ProfileScreen(
                                repository = repository,
                                userAuthManager = gameViewModel.userAuthManager,
                                databaseService = gameViewModel.databaseService,
                                onNavigateAdmin = { currentScreen = ScreenState.Admin }
                            )
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
