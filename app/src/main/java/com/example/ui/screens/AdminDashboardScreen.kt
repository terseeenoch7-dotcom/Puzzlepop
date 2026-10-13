package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiLevelService
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.LevelEntity
import com.example.data.entity.MissionEntity
import com.example.game.engine.TileEngine
import com.example.game.model.LevelData
import com.example.game.model.LevelDifficulty
import com.example.ui.components.Cartoon3DButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    repository: GameRepository,
    onNavigateBack: () -> Unit,
    onTestLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allLevels by repository.getAllLevelsFlow().collectAsState(initial = emptyList())
    val allUsers by repository.getAllUsersFlow().collectAsState(initial = emptyList())
    val allTransactions by repository.getAllTransactionsFlow().collectAsState(initial = emptyList())
    val allMissions by repository.getMissionsFlow().collectAsState(initial = emptyList())
    val auditLogs by repository.getAuditLogsFlow().collectAsState(initial = emptyList())
    val adminConfigs by repository.getAdminConfigsFlow().collectAsState(initial = emptyList())
    val syncStatus by repository.syncStatus.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Overview", "Levels", "Tasks", "Economy", "Announcements", "Audit Log")

    // Dialog States
    var showCreateLevelDialog by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    var showGrantCoinsDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var actionStatusMessage by remember { mutableStateOf<String?>(null) }

    fun showFeedback(msg: String) {
        actionStatusMessage = msg
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Box(
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
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin Control Center",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "PuzzlePop Central Engine • Real-Time Cloud Sync",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                // Cloud Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (syncStatus.isCloudConnected) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                    modifier = Modifier.border(
                        1.dp,
                        if (syncStatus.isCloudConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (syncStatus.isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = if (syncStatus.isCloudConnected) Color(0xFF34D399) else Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (syncStatus.isCloudConnected) "Cloud Active" else "Offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (syncStatus.isCloudConnected) Color(0xFF6EE7B7) else Color(0xFFFDE047)
                        )
                    }
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFFFDE047),
                edgePadding = 16.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> AdminOverviewTab(
                        repository = repository,
                        allUsers = allUsers,
                        allLevels = allLevels,
                        allTransactions = allTransactions,
                        allMissions = allMissions,
                        onActionFeedback = { showFeedback(it) }
                    )
                    1 -> AdminLevelsTab(
                        levels = allLevels,
                        onTestLevel = onTestLevel,
                        onCreateLevel = { showCreateLevelDialog = true },
                        onAiGenerate = { showAiGeneratorDialog = true },
                        onToggleActive = { lvlNum, active ->
                            coroutineScope.launch {
                                val res = repository.adminToggleLevelActive(lvlNum, active, "Admin")
                                showFeedback(res.getOrDefault("Updated level $lvlNum"))
                            }
                        }
                    )
                    2 -> AdminTasksTab(
                        tasks = allMissions,
                        onCreateTask = { showCreateTaskDialog = true },
                        onDeleteTask = { taskId ->
                            coroutineScope.launch {
                                val res = repository.adminDeleteTask(taskId, "Admin")
                                showFeedback(res.getOrDefault("Deleted task"))
                            }
                        }
                    )
                    3 -> AdminEconomyTab(
                        configs = adminConfigs,
                        allTransactions = allTransactions,
                        allUsers = allUsers,
                        onUpdateDefaultReward = { newReward ->
                            coroutineScope.launch {
                                val res = repository.adminUpdateDefaultReward(newReward, "Admin")
                                showFeedback(res.getOrDefault("Updated reward"))
                            }
                        },
                        onOpenGrantCoins = { showGrantCoinsDialog = true }
                    )
                    4 -> AdminAnnouncementsTab(
                        repository = repository,
                        onActionFeedback = { showFeedback(it) }
                    )
                    5 -> AdminAuditLogsTab(
                        logs = auditLogs
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // --- AI LEVEL GENERATOR DIALOG ---
        if (showAiGeneratorDialog) {
            var promptTheme by remember { mutableStateOf("Candy Wonderland") }
            var selectedDifficulty by remember { mutableStateOf("Medium") }
            var isGenerating by remember { mutableStateOf(false) }
            var generatedMessage by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { if (!isGenerating) showAiGeneratorDialog = false },
                containerColor = Color(0xFF1E1B4B),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFDE047))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI Level Studio", color = Color(0xFFFEF08A), fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Generates multi-layer puzzle layouts with valid triplet combinations, then synchronizes to Cloud Firestore.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = promptTheme,
                            onValueChange = { promptTheme = it },
                            label = { Text("Level Theme / Description") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(text = "Difficulty: $selectedDifficulty", fontSize = 12.sp, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Easy", "Medium", "Hard").forEach { diff ->
                                Surface(
                                    onClick = { selectedDifficulty = diff },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedDifficulty == diff) Color(0xFF8B5CF6) else Color(0xFF334155)
                                ) {
                                    Text(
                                        text = diff,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (isGenerating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFFBBF24))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Designing level with Gemini...", fontSize = 12.sp, color = Color(0xFFFDE047))
                            }
                        }

                        generatedMessage?.let {
                            Text(text = it, fontSize = 12.sp, color = Color(0xFF34D399))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isGenerating,
                        onClick = {
                            coroutineScope.launch {
                                isGenerating = true
                                val result = GeminiLevelService.generateThemedLevel(promptTheme, selectedDifficulty)
                                isGenerating = false
                                result.onSuccess { gen ->
                                    val newLevelNumber = allLevels.size + 1
                                    val levelData = LevelData(
                                        levelNumber = newLevelNumber,
                                        name = gen.title,
                                        themeName = gen.theme,
                                        difficulty = try { LevelDifficulty.valueOf(selectedDifficulty.uppercase()) } catch (_: Exception) { LevelDifficulty.MEDIUM },
                                        baseReward = 5,
                                        bonusReward = 2,
                                        trayCapacity = 7,
                                        tiles = gen.tiles,
                                        isActive = true
                                    )
                                    val saveRes = repository.adminSaveLevel(levelData, "Gemini AI")
                                    val msg = "Created Level $newLevelNumber: ${gen.title} (Synced to Cloud ☁️)"
                                    generatedMessage = msg
                                    showFeedback(msg)
                                }
                            }
                        }
                    ) {
                        Text("Generate & Publish", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAiGeneratorDialog = false }) {
                        Text("Close", color = Color.Gray)
                    }
                }
            )
        }

        // --- MANUAL CREATE LEVEL DIALOG ---
        if (showCreateLevelDialog) {
            var levelName by remember { mutableStateOf("Sweet Adventure") }
            var themeName by remember { mutableStateOf("Sugar Valley") }
            var baseRewardText by remember { mutableStateOf("5") }

            AlertDialog(
                onDismissRequest = { showCreateLevelDialog = false },
                containerColor = Color(0xFF1E1B4B),
                title = { Text("Create New Level", fontWeight = FontWeight.Bold, color = Color(0xFFFDE047)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = levelName,
                            onValueChange = { levelName = it },
                            label = { Text("Level Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = themeName,
                            onValueChange = { themeName = it },
                            label = { Text("Theme") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = baseRewardText,
                            onValueChange = { baseRewardText = it },
                            label = { Text("Base Coin Reward") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val nextLvl = allLevels.size + 1
                                val reward = baseRewardText.toIntOrNull() ?: 5
                                val gen = TileEngine.generateLevel(nextLvl, themeName, LevelDifficulty.MEDIUM, baseReward = reward)
                                val finalLevel = gen.copy(name = levelName)
                                val res = repository.adminSaveLevel(finalLevel, "Admin")
                                showFeedback(res.getOrDefault("Level $nextLvl published & synced"))
                                showCreateLevelDialog = false
                            }
                        }
                    ) {
                        Text("Publish Level", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateLevelDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // --- CREATE TASK DIALOG (Admin Tasks Feature) ---
        if (showCreateTaskDialog) {
            var taskTitle by remember { mutableStateOf("") }
            var taskDescription by remember { mutableStateOf("") }
            var targetProgressText by remember { mutableStateOf("10") }
            var rewardCoinsText by remember { mutableStateOf("25") }

            AlertDialog(
                onDismissRequest = { showCreateTaskDialog = false },
                containerColor = Color(0xFF1E1B4B),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Task, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create In-App Task", fontWeight = FontWeight.Bold, color = Color(0xFFFEF08A))
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Admin tasks automatically synchronize across all player devices in real time.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Task Title (e.g. Tile Striker)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = taskDescription,
                            onValueChange = { taskDescription = it },
                            label = { Text("Description (e.g. Match 15 Fruit tiles)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = targetProgressText,
                                onValueChange = { targetProgressText = it },
                                label = { Text("Target Goal") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rewardCoinsText,
                                onValueChange = { rewardCoinsText = it },
                                label = { Text("Coin Reward") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                coroutineScope.launch {
                                    val target = targetProgressText.toIntOrNull() ?: 1
                                    val reward = rewardCoinsText.toIntOrNull() ?: 10
                                    val res = repository.adminCreateTask(
                                        title = taskTitle.trim(),
                                        description = taskDescription.trim(),
                                        targetProgress = target,
                                        rewardCoins = reward,
                                        adminName = "Admin"
                                    )
                                    showFeedback(res.getOrDefault("Task created and synced to Cloud ☁️"))
                                    showCreateTaskDialog = false
                                }
                            }
                        }
                    ) {
                        Text("Create & Sync", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateTaskDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // --- GRANT COINS DIALOG ---
        if (showGrantCoinsDialog) {
            var selectedUser by remember { mutableStateOf(allUsers.firstOrNull()?.id ?: "player_main") }
            var coinAmountText by remember { mutableStateOf("50") }
            var grantReason by remember { mutableStateOf("Player loyalty appreciation bonus") }

            AlertDialog(
                onDismissRequest = { showGrantCoinsDialog = false },
                containerColor = Color(0xFF1E1B4B),
                title = { Text("Grant Virtual Coins", fontWeight = FontWeight.Bold, color = Color(0xFFFDE047)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Target User ID: $selectedUser", fontSize = 12.sp, color = Color.White)
                        OutlinedTextField(
                            value = coinAmountText,
                            onValueChange = { coinAmountText = it },
                            label = { Text("Coin Amount") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = grantReason,
                            onValueChange = { grantReason = it },
                            label = { Text("Audit Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val amt = coinAmountText.toIntOrNull() ?: 50
                                val res = repository.adminGrantCoins(selectedUser, amt, grantReason, "Admin")
                                showFeedback(res.getOrDefault("Granted $amt coins"))
                                showGrantCoinsDialog = false
                            }
                        }
                    ) {
                        Text("Grant Coins", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGrantCoinsDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

// ==========================================
// Sub-Tabs
// ==========================================

@Composable
private fun AdminOverviewTab(
    repository: GameRepository,
    allUsers: List<com.example.data.entity.UserAccountEntity>,
    allLevels: List<LevelEntity>,
    allTransactions: List<com.example.data.entity.CoinTransactionEntity>,
    allMissions: List<MissionEntity>,
    onActionFeedback: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSeeding by remember { mutableStateOf(false) }

    val totalCoinsAwarded = allTransactions.filter { it.amount > 0 }.sumOf { it.amount }
    val totalCoinsSpent = allTransactions.filter { it.amount < 0 }.sumOf { -it.amount }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // Cloud Synchronization Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cross-Device Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Text("Real-Time", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Changes saved in this admin dashboard are broadcast to Cloud Firestore and received immediately by all connected player devices.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Cartoon3DButton(
                        text = if (isSeeding) "Seeding Cloud..." else "SEED 30 LEVELS TO CLOUD",
                        icon = "☁️",
                        enabled = !isSeeding,
                        onClick = {
                            coroutineScope.launch {
                                isSeeding = true
                                val res = repository.seedInitialDataToCloud("Admin")
                                isSeeding = false
                                if (res.isSuccess) {
                                    onActionFeedback(res.getOrDefault("Successfully seeded levels to Cloud!"))
                                } else {
                                    onActionFeedback("Seed error: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        containerColor = Color(0xFF3B82F6),
                        shadowColor = Color(0xFF1D4ED8),
                        height = 42.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatMetric(title = "Total Users", value = "${allUsers.size}", icon = "👥", modifier = Modifier.weight(1f))
                AdminStatMetric(title = "Catalog Levels", value = "${allLevels.size}", icon = "🧩", modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatMetric(title = "Active Tasks", value = "${allMissions.size}", icon = "🎯", modifier = Modifier.weight(1f))
                AdminStatMetric(title = "Transactions", value = "${allTransactions.size}", icon = "📜", modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatMetric(title = "Coins Awarded", value = "$totalCoinsAwarded 🪙", icon = "🎁", modifier = Modifier.weight(1f))
                AdminStatMetric(title = "Coins Spent", value = "$totalCoinsSpent 🪙", icon = "🛒", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminLevelsTab(
    levels: List<LevelEntity>,
    onTestLevel: (Int) -> Unit,
    onCreateLevel: () -> Unit,
    onAiGenerate: () -> Unit,
    onToggleActive: (Int, Boolean) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Puzzle Level Catalog (${levels.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)

                Row {
                    Surface(
                        onClick = onAiGenerate,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gemini AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Surface(
                        onClick = onCreateLevel,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        items(levels) { lvl ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Level ${lvl.levelNumber}: ${lvl.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${lvl.themeName} • ${lvl.difficulty} • Reward: ${lvl.baseReward} 🪙",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = lvl.isActive,
                            onCheckedChange = { onToggleActive(lvl.levelNumber, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { onTestLevel(lvl.levelNumber) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF3B82F6)
                        ) {
                            Text(
                                text = "Preview",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tasks Page where admin can create, view, and manage in-app tasks.
 */
@Composable
private fun AdminTasksTab(
    tasks: List<MissionEntity>,
    onCreateTask: () -> Unit,
    onDeleteTask: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Synchronized Tasks (${tasks.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Created tasks appear instantly on user devices",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Surface(
                    onClick = onCreateTask,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0EA5E9),
                    modifier = Modifier.testTag("admin_create_task_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Task", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No Tasks Yet", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Click 'Create Task' above to publish a task for players", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        } else {
            items(tasks) { task ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth().testTag("admin_task_card_${task.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = task.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFEF08A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "+${task.rewardCoins} 🪙",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6EE7B7),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.description,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Target Goal: ${task.targetProgress} completions",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        IconButton(
                            onClick = { onDeleteTask(task.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminEconomyTab(
    configs: List<com.example.data.entity.AdminConfigEntity>,
    allTransactions: List<com.example.data.entity.CoinTransactionEntity>,
    allUsers: List<com.example.data.entity.UserAccountEntity>,
    onUpdateDefaultReward: (Int) -> Unit,
    onOpenGrantCoins: () -> Unit
) {
    val defaultReward = configs.firstOrNull { it.configKey == "default_reward_per_level" }?.configValue ?: "5"
    var rewardInput by remember(defaultReward) { mutableStateOf(defaultReward) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(text = "Coin Economy & Booster Pricing", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Default Reward Per Completed Level", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    Text(text = "Configurable baseline coins credited upon finishing any level. Synchronized across devices.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = rewardInput,
                            onValueChange = { rewardInput = it },
                            label = { Text("Coins") },
                            modifier = Modifier.width(120.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Cartoon3DButton(
                            text = "Save & Sync",
                            onClick = {
                                val num = rewardInput.toIntOrNull() ?: 5
                                onUpdateDefaultReward(num)
                            },
                            containerColor = Color(0xFF10B981),
                            shadowColor = Color(0xFF047857),
                            height = 44.dp
                        )
                    }
                }
            }
        }

        item {
            Cartoon3DButton(
                text = "GRANT COINS TO USER",
                icon = "🎁",
                onClick = onOpenGrantCoins,
                containerColor = Color(0xFFF59E0B),
                shadowColor = Color(0xFFB45309),
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp
            )
        }

        item {
            Text(text = "Active Booster Price Table", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BoosterCostRow(name = "💡 Hint", cost = 15)
                    BoosterCostRow(name = "🔀 Shuffle", cost = 20)
                    BoosterCostRow(name = "↩️ Undo", cost = 10)
                    BoosterCostRow(name = "➕ Extra Slot", cost = 25)
                    BoosterCostRow(name = "💖 Second Chance", cost = 20)
                }
            }
        }
    }
}

@Composable
private fun BoosterCostRow(name: String, cost: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, fontSize = 13.sp, color = Color.White)
        Text(text = "$cost Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
    }
}

@Composable
private fun AdminAnnouncementsTab(
    repository: GameRepository,
    onActionFeedback: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val announcements by repository.getAnnouncementsFlow().collectAsState(initial = emptyList())
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(text = "Broadcast Announcement", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Announcement Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newContent,
                onValueChange = { newContent = it },
                label = { Text("Content / Details") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Cartoon3DButton(
                text = "PUBLISH & SYNC TO ALL DEVICES",
                onClick = {
                    if (newTitle.isNotBlank()) {
                        coroutineScope.launch {
                            val res = repository.adminCreateAnnouncement(newTitle.trim(), newContent.trim(), "Admin")
                            onActionFeedback(res.getOrDefault("Announcement broadcasted ☁️"))
                            newTitle = ""
                            newContent = ""
                        }
                    }
                },
                containerColor = Color(0xFF0EA5E9),
                shadowColor = Color(0xFF0369A1),
                height = 46.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Published Announcements (${announcements.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
        }

        items(announcements) { a ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = a.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFEF08A))
                        Text(text = a.content, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val res = repository.adminDeleteAnnouncement(a.title, "Admin")
                                onActionFeedback(res.getOrDefault("Announcement deleted"))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAuditLogsTab(
    logs: List<AdminAuditLogEntity>
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(text = "Security Audit Trail (${logs.size} Entries)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }

        items(logs) { log ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "[${log.actionType}]", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "by ${log.adminName}", fontSize = 11.sp, color = Color(0xFFFEF08A))
                        }
                        Text(text = log.details, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Text(
                        text = dateFormat.format(Date(log.timestamp)),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminStatMetric(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}
