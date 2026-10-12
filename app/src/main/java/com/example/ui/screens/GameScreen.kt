package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.ui.components.Cartoon3DButton
import com.example.ui.components.CoinBadge
import com.example.ui.components.CollectionTrayView
import com.example.ui.components.GlossyTileView

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateHome: () -> Unit,
    onNavigateWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.repository.getCurrentUserFlow().collectAsState(initial = null)

    BackHandler {
        onNavigateHome()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF311042)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP AREA: Header, Level Info, Timer, Coin Badge ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateHome,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit to Home",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LEVEL ${uiState.levelData?.levelNumber ?: 1}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFFFDE047)
                        )
                        Text(
                            text = uiState.levelData?.name ?: "Sunny Orchard",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.askGeminiOracle() },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.3f))
                                .border(1.dp, Color(0xFFA78BFA), CircleShape)
                                .testTag("ai_oracle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI Oracle",
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        CoinBadge(
                            coins = currentUser?.totalCoins ?: 0,
                            onClick = onNavigateWallet
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar (% tiles cleared)
                val totalInitial = uiState.levelData?.totalTilesCount ?: 1
                val remaining = uiState.boardTiles.size + uiState.trayTiles.size
                val progress = ((totalInitial - remaining).toFloat() / totalInitial.coerceAtLeast(1))
                    .coerceIn(0f, 1f)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "⏱️ ${uiState.elapsedSeconds}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // --- CENTER AREA: Interactive 3D Puzzle Board ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color(0xFFFBBF24))
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val boardWidth = maxWidth
                        val boardHeight = maxHeight

                        // Dynamic scale to fit tiles within available space
                        val tileUnitDp = (boardWidth.value / 5.2f).coerceAtMost(62f).dp

                        // Sort tiles so lower layers render first, higher layers render on top
                        val sortedTiles = uiState.boardTiles.sortedBy { it.layer }

                        Box(
                            modifier = Modifier
                                .size(tileUnitDp * 4.6f, tileUnitDp * 5.2f)
                        ) {
                            sortedTiles.forEach { tile ->
                                val isHighlighted = tile.id == uiState.hintTileId
                                GlossyTileView(
                                    tile = tile.copy(isHighlighted = isHighlighted),
                                    sizeDp = tileUnitDp,
                                    onClick = { viewModel.onTileClicked(tile) },
                                    modifier = Modifier.offset(
                                        x = tileUnitDp * tile.x,
                                        y = tileUnitDp * tile.y
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // --- BOTTOM AREA: Collection Tray & Boosters ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3D Collection Tray (7 or 8 slots)
                val maxSlots = uiState.maxTraySlots + (if (uiState.extraSlotActive) 1 else 0)
                CollectionTrayView(
                    tiles = uiState.trayTiles,
                    maxSlots = maxSlots,
                    slotSize = 42.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Boosters Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoosterButton(
                        emoji = "💡",
                        label = "Hint",
                        cost = 15,
                        onClick = { viewModel.useHint() },
                        testTag = "booster_hint"
                    )
                    BoosterButton(
                        emoji = "🔀",
                        label = "Shuffle",
                        cost = 20,
                        onClick = { viewModel.useShuffle() },
                        testTag = "booster_shuffle"
                    )
                    BoosterButton(
                        emoji = "↩️",
                        label = "Undo",
                        cost = 10,
                        onClick = { viewModel.useUndo() },
                        testTag = "booster_undo"
                    )
                    BoosterButton(
                        emoji = "➕",
                        label = if (uiState.extraSlotActive) "Active" else "+Slot",
                        cost = 25,
                        enabled = !uiState.extraSlotActive,
                        onClick = { viewModel.useExtraSlot() },
                        testTag = "booster_extra_slot"
                    )
                }
            }
        }

        // --- VICTORY CELEBRATION MODAL ---
        AnimatedVisibility(
            visible = uiState.isVictory,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            VictoryDialog(
                stars = uiState.starsAwarded,
                coins = uiState.coinsAwarded,
                levelNumber = uiState.levelData?.levelNumber ?: 1,
                timeSeconds = uiState.elapsedSeconds,
                onNextLevel = {
                    val nextLvl = (uiState.levelData?.levelNumber ?: 1) + 1
                    viewModel.loadLevel(nextLvl)
                },
                onHome = onNavigateHome
            )
        }

        // --- GAME OVER MODAL (Tray Full) ---
        AnimatedVisibility(
            visible = uiState.isGameOver,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            GameOverDialog(
                playerCoins = currentUser?.totalCoins ?: 0,
                onSecondChance = { viewModel.useSecondChance() },
                onRetry = {
                    uiState.levelData?.levelNumber?.let { viewModel.loadLevel(it) }
                },
                onHome = onNavigateHome
            )
        }

        // --- AI ORACLE BANNER ---
        if (uiState.aiMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { viewModel.dismissAiMessage() }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    modifier = Modifier.border(2.dp, Color(0xFFA78BFA), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "✨ Gemini Puzzle Oracle", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFFDE047))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.aiMessage ?: "",
                            fontSize = 15.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Cartoon3DButton(
                            text = "Got It!",
                            onClick = { viewModel.dismissAiMessage() },
                            containerColor = Color(0xFF8B5CF6),
                            shadowColor = Color(0xFF6D28D9),
                            height = 44.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoosterButton(
    emoji: String,
    label: String,
    cost: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.9f),
        modifier = Modifier
            .border(
                1.5.dp,
                if (enabled) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🪙", fontSize = 10.sp)
                Text(text = "$cost", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
            }
        }
    }
}

@Composable
private fun VictoryDialog(
    stars: Int,
    coins: Int,
    levelNumber: Int,
    timeSeconds: Int,
    onNextLevel: () -> Unit,
    onHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(2.5.dp, Color(0xFFF59E0B), RoundedCornerShape(28.dp))
                .testTag("victory_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 LEVEL CLEARED! 🎉",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFEF08A),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Stars Display
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..3) {
                        val isLit = i <= stars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (isLit) Color(0xFFFACC15) else Color(0xFF475569),
                            modifier = Modifier
                                .size(if (i == 2) 48.dp else 40.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Coin Reward Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.border(1.dp, Color(0xFFFDE047).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "+$coins COINS", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFFDE047))
                            Text(text = "Time: ${timeSeconds}s", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Cartoon3DButton(
                    text = "NEXT LEVEL",
                    onClick = onNextLevel,
                    containerColor = Color(0xFF10B981),
                    shadowColor = Color(0xFF047857),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "victory_next_level_button"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Cartoon3DButton(
                    text = "RETURN HOME",
                    onClick = onHome,
                    containerColor = Color(0xFF64748B),
                    shadowColor = Color(0xFF334155),
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp,
                    testTag = "victory_home_button"
                )
            }
        }
    }
}

@Composable
private fun GameOverDialog(
    playerCoins: Int,
    onSecondChance: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(2.5.dp, Color(0xFFEF4444), RoundedCornerShape(28.dp))
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💔 TRAY FULL!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF87171)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "No more room in the collection tray. Don't give up!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Second Chance option
                val canAfford = playerCoins >= 20
                Cartoon3DButton(
                    text = "SECOND CHANCE (20 🪙)",
                    icon = "💖",
                    enabled = canAfford,
                    onClick = onSecondChance,
                    containerColor = Color(0xFFEC4899),
                    shadowColor = Color(0xFFBE185D),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "second_chance_button"
                )
                if (!canAfford) {
                    Text(
                        text = "Need 20 coins (Balance: $playerCoins)",
                        fontSize = 11.sp,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Cartoon3DButton(
                    text = "RETRY LEVEL",
                    icon = "🔄",
                    onClick = onRetry,
                    containerColor = Color(0xFF3B82F6),
                    shadowColor = Color(0xFF1D4ED8),
                    modifier = Modifier.fillMaxWidth(),
                    height = 50.dp,
                    testTag = "game_over_retry_button"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Cartoon3DButton(
                    text = "RETURN HOME",
                    onClick = onHome,
                    containerColor = Color(0xFF64748B),
                    shadowColor = Color(0xFF334155),
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp,
                    testTag = "game_over_home_button"
                )
            }
        }
    }
}
