package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameRepository
import com.example.data.entity.DailyRewardEntity
import com.example.data.entity.LevelEntity
import com.example.data.entity.MissionEntity
import com.example.ui.components.Cartoon3DButton
import com.example.ui.components.CoinBadge
import com.example.util.SoundEffects
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: GameRepository,
    onStartGame: (Int) -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by repository.getCurrentUserFlow().collectAsState(initial = null)
    val dailyRewards by repository.getDailyRewardsFlow().collectAsState(initial = emptyList())
    val missions by repository.getMissionsFlow().collectAsState(initial = emptyList())
    val announcements by repository.getAnnouncementsFlow().collectAsState(initial = emptyList())
    val allLevels by repository.getAllLevelsFlow().collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    var showLevelPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- TOP BAR: Profile Shortcut, Title, Coin Badge ---
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onNavigateProfile() }
                        .testTag("home_profile_shortcut")
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF818CF8), Color(0xFF4338CA))
                                )
                            )
                            .border(2.dp, Color(0xFFFDE047), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = user?.avatarEmoji ?: "🐼", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user?.username ?: "PopMaster",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.25f),
                            modifier = Modifier.border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "Lvl ${user?.currentLevel ?: 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateAdmin,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFBBF24), CircleShape)
                            .testTag("home_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Dashboard",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    CoinBadge(
                        coins = user?.totalCoins ?: 0,
                        onClick = onNavigateWallet
                    )
                }
            }
        }

        // --- HERO BANNER ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .border(2.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .testTag("hero_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.puzzlepop_hero),
                        contentDescription = "PuzzlePop 3D Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay to keep text readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF0F172A).copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "PUZZLEPOP",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFEF08A),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Casual 3D Tile Matching Adventure",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- MAIN PLAY & LEVEL SELECT BUTTONS ---
        item {
            val currentLvl = user?.currentLevel ?: 1
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Cartoon3DButton(
                    text = "PLAY LEVEL $currentLvl",
                    icon = "▶️",
                    onClick = { onStartGame(currentLvl) },
                    containerColor = Color(0xFF10B981),
                    shadowColor = Color(0xFF047857),
                    height = 58.dp,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "home_play_button"
                )

                Spacer(modifier = Modifier.height(4.dp))

                Cartoon3DButton(
                    text = "LEVEL SELECTOR (${allLevels.size} STAGES)",
                    icon = "🗺️",
                    onClick = { showLevelPicker = true },
                    containerColor = Color(0xFF3B82F6),
                    shadowColor = Color(0xFF1D4ED8),
                    height = 46.dp,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "home_level_selector_button"
                )
            }
        }

        // --- 7-DAY LOGIN STREAK REWARDS ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .testTag("daily_streak_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎁 7-Day Login Streak",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFFDE047)
                        )
                        Text(
                            text = "Daily Gifts",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dailyRewards) { reward ->
                            DailyRewardItem(
                                reward = reward,
                                onClaim = {
                                    coroutineScope.launch {
                                        SoundEffects.playCoinReward()
                                        repository.claimDailyReward(reward.dayNumber, reward.coinAmount)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- DAILY MISSIONS ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .testTag("daily_missions_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 Daily Missions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "Earn Extra Coins",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    missions.forEach { mission ->
                        MissionRowItem(
                            mission = mission,
                            onClaim = {
                                coroutineScope.launch {
                                    SoundEffects.playCoinReward()
                                    repository.claimMissionReward(mission.id, mission.rewardCoins)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // --- ANNOUNCEMENT BANNER ---
        if (announcements.isNotEmpty()) {
            item {
                val activeAnnouncements = announcements.filter { it.isActive }
                if (activeAnnouncements.isNotEmpty()) {
                    val first = activeAnnouncements.first()
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = first.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFFEF08A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = first.content,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // --- LEVEL PICKER BOTTOM SHEET ---
    if (showLevelPicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showLevelPicker = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1B4B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "🗺️ Choose Level",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFEF08A)
                )
                Text(
                    text = "Replay completed levels or start your next milestone!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val maxUnlocked = user?.highestLevelReached ?: 1
                    items(allLevels) { lvl ->
                        val isUnlocked = lvl.levelNumber <= maxUnlocked
                        LevelRowItem(
                            level = lvl,
                            isUnlocked = isUnlocked,
                            onSelect = {
                                showLevelPicker = false
                                onStartGame(lvl.levelNumber)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DailyRewardItem(
    reward: DailyRewardEntity,
    onClaim: () -> Unit
) {
    val isClaimed = reward.isClaimed
    Surface(
        onClick = { if (!isClaimed) onClaim() },
        enabled = !isClaimed,
        shape = RoundedCornerShape(14.dp),
        color = if (isClaimed) Color(0xFF1E293B) else Color(0xFF334155),
        modifier = Modifier
            .width(66.dp)
            .border(
                1.dp,
                if (!isClaimed) Color(0xFFFBBF24) else Color.Gray.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .testTag("daily_reward_day_${reward.dayNumber}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Day ${reward.dayNumber}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isClaimed) Color(0xFFFEF08A) else Color.Gray
            )
            Text(
                text = if (isClaimed) "✅" else (reward.boosterType?.let { "🎁" } ?: "🪙"),
                fontSize = 20.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = if (isClaimed) "Done" else "+${reward.coinAmount}",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isClaimed) Color.Gray else Color(0xFF34D399)
            )
        }
    }
}

@Composable
private fun MissionRowItem(
    mission: MissionEntity,
    onClaim: () -> Unit
) {
    val isComplete = mission.currentProgress >= mission.targetProgress
    val isClaimed = mission.isClaimed

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mission.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = mission.description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        (mission.currentProgress.toFloat() / mission.targetProgress.coerceAtLeast(1))
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF0EA5E9),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }

            Surface(
                onClick = { if (isComplete && !isClaimed) onClaim() },
                enabled = isComplete && !isClaimed,
                shape = RoundedCornerShape(10.dp),
                color = when {
                    isClaimed -> Color(0xFF334155)
                    isComplete -> Color(0xFF10B981)
                    else -> Color(0xFF1E293B)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isClaimed) "Claimed" else "+${mission.rewardCoins} 🪙",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelRowItem(
    level: LevelEntity,
    isUnlocked: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = { if (isUnlocked) onSelect() },
        enabled = isUnlocked,
        shape = RoundedCornerShape(14.dp),
        color = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isUnlocked) Color(0xFF38BDF8).copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                RoundedCornerShape(14.dp)
            )
            .testTag("level_row_${level.levelNumber}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) Color(0xFF0284C7) else Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isUnlocked) "${level.levelNumber}" else "🔒",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = level.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isUnlocked) Color.White else Color.Gray
                    )
                    Text(
                        text = "${level.themeName} • ${level.difficulty} • ${level.baseReward} 🪙",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            if (isUnlocked && level.highStars > 0) {
                Row {
                    repeat(level.highStars) {
                        Text(text = "⭐", fontSize = 12.sp)
                    }
                }
            } else if (isUnlocked) {
                Text(text = "▶️", fontSize = 16.sp)
            }
        }
    }
}
