package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserAuthManager
import com.example.data.DatabaseService
import com.example.data.GameRepository
import com.example.ui.components.Cartoon3DButton
import com.example.util.SoundEffects
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: GameRepository,
    userAuthManager: UserAuthManager,
    databaseService: DatabaseService,
    onNavigateAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by repository.getCurrentUserFlow().collectAsState(initial = null)
    val allUsers by repository.getAllUsersFlow().collectAsState(initial = emptyList())
    val firebaseUser by userAuthManager.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showEditDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(SoundEffects.isSoundEnabled) }
    var hapticEnabled by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    val avatars = listOf("🐼", "🐱", "🦊", "🐰", "🦁", "🦄", "🐸", "👑")

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
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Player Profile",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // --- AVATAR & USER PROFILE CARD ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF818CF8).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .testTag("profile_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF818CF8), Color(0xFF3730A3))
                                )
                            )
                            .border(3.dp, Color(0xFFFDE047), CircleShape)
                            .clickable { showEditDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentUser?.avatarEmoji ?: "🐼", fontSize = 42.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentUser?.username ?: "PopMaster",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            modifier = Modifier.border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "ID: ${currentUser?.id ?: "player_main"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7DD3FC),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentUser?.role == "SUPER_ADMIN") Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.2f),
                            modifier = Modifier.border(
                                1.dp,
                                if (currentUser?.role == "SUPER_ADMIN") Color(0xFFF59E0B) else Color(0xFF10B981),
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(
                                text = currentUser?.role ?: "PLAYER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser?.role == "SUPER_ADMIN") Color(0xFFFDE047) else Color(0xFF6EE7B7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- FIREBASE AUTH & CLOUD SYNC CARD ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .testTag("firebase_auth_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (firebaseUser != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = if (firebaseUser != null) Color(0xFF34D399) else Color(0xFFFBBF24),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Firebase Cloud Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        if (firebaseUser != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                modifier = Modifier.border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "Connected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (firebaseUser != null) {
                        Text(
                            text = "Signed in as: ${firebaseUser?.email ?: firebaseUser?.displayName ?: "User"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFEF08A)
                        )
                        Text(
                            text = "Cloud UID: ${firebaseUser?.uid?.take(16)}...",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Cartoon3DButton(
                                text = if (isSyncing) "Syncing..." else "SYNC PROGRESS",
                                icon = "☁️",
                                enabled = !isSyncing,
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncing = true
                                        currentUser?.let { local ->
                                            val res = databaseService.syncLocalUserToCloud(local)
                                            isSyncing = false
                                            if (res.isSuccess) {
                                                syncMessage = "Progression successfully synced to Firestore!"
                                                Toast.makeText(context, "Progress synced to Firestore!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                syncMessage = "Sync failed: ${res.exceptionOrNull()?.message}"
                                            }
                                        }
                                    }
                                },
                                containerColor = Color(0xFF0EA5E9),
                                shadowColor = Color(0xFF0284C7),
                                height = 44.dp,
                                modifier = Modifier.weight(1f)
                            )

                            Cartoon3DButton(
                                text = "LOGOUT",
                                icon = "🚪",
                                onClick = {
                                    userAuthManager.signOut()
                                    Toast.makeText(context, "Signed out of Firebase", Toast.LENGTH_SHORT).show()
                                },
                                containerColor = Color(0xFF64748B),
                                shadowColor = Color(0xFF334155),
                                height = 44.dp,
                                modifier = Modifier.weight(0.7f)
                            )
                        }

                        syncMessage?.let {
                            Text(
                                text = it,
                                fontSize = 11.sp,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Connect your account to backup coin balances, levels, and achievements to Firebase Cloud Firestore.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Cartoon3DButton(
                            text = "SIGN IN WITH GOOGLE",
                            icon = "🌐",
                            onClick = {
                                coroutineScope.launch {
                                    val result = userAuthManager.signInWithGoogle(context)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Google Sign-In successful!", Toast.LENGTH_SHORT).show()
                                        currentUser?.let { databaseService.syncLocalUserToCloud(it) }
                                    } else {
                                        Toast.makeText(context, "Sign-in cancelled or failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            containerColor = Color(0xFF4285F4),
                            shadowColor = Color(0xFF1E3A8A),
                            modifier = Modifier.fillMaxWidth(),
                            height = 48.dp,
                            testTag = "google_sign_in_button"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Cartoon3DButton(
                            text = "EMAIL LOGIN / REGISTER",
                            icon = "✉️",
                            onClick = { showAuthDialog = true },
                            containerColor = Color(0xFF8B5CF6),
                            shadowColor = Color(0xFF6D28D9),
                            modifier = Modifier.fillMaxWidth(),
                            height = 44.dp,
                            testTag = "email_auth_button"
                        )
                    }
                }
            }
        }

        // --- GAMEPLAY STATISTICS GRID ---
        item {
            Text(
                text = "📊 Career Statistics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            val user = currentUser
            val winRate = if ((user?.levelsWon ?: 0) + (user?.levelsFailed ?: 0) > 0) {
                ((user?.levelsWon ?: 0) * 100) / ((user?.levelsWon ?: 0) + (user?.levelsFailed ?: 0))
            } else 100

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(label = "Current Level", value = "${user?.currentLevel ?: 1}", icon = "🗺️", modifier = Modifier.weight(1f))
                    StatCard(label = "Highest Stage", value = "${user?.highestLevelReached ?: 1}", icon = "🏆", modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(label = "Total Stars", value = "${user?.totalStars ?: 0} ⭐", icon = "✨", modifier = Modifier.weight(1f))
                    StatCard(label = "Matches Made", value = "${user?.matchesCount ?: 0}", icon = "🎯", modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(label = "Stages Won", value = "${user?.levelsWon ?: 0}", icon = "🥇", modifier = Modifier.weight(1f))
                    StatCard(label = "Win Rate", value = "$winRate%", icon = "📈", modifier = Modifier.weight(1f))
                }
            }
        }

        // --- SETTINGS & SOUND PREFERENCES ---
        item {
            Text(
                text = "⚙️ Game Preferences",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔊 Sound Effects & Pops", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                SoundEffects.isSoundEnabled = it
                                if (it) SoundEffects.playTilePop()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFF59E0B),
                                checkedTrackColor = Color(0xFF78350F)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📳 Haptic Touch Vibrations", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { hapticEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF065F46)
                            )
                        )
                    }
                }
            }
        }

        // --- SWITCH ACCOUNT & ADMIN SHORTCUT ---
        item {
            Text(
                text = "👥 Account Management",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Account Switcher",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allUsers.forEach { u ->
                            val isSelected = u.id == currentUser?.id
                            Surface(
                                onClick = { repository.switchUser(u.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF4338CA) else Color(0xFF0F172A),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFDE047) else Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = u.avatarEmoji, fontSize = 22.sp)
                                    Text(
                                        text = u.username,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFFEF08A) else Color.White
                                    )
                                    Text(
                                        text = u.role,
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Cartoon3DButton(
                        text = "ADMINISTRATOR CONSOLE",
                        icon = "🛡️",
                        onClick = onNavigateAdmin,
                        containerColor = Color(0xFF7C3AED),
                        shadowColor = Color(0xFF5B21B6),
                        modifier = Modifier.fillMaxWidth(),
                        height = 48.dp,
                        testTag = "profile_admin_console_button"
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // --- EDIT PROFILE DIALOG ---
    if (showEditDialog) {
        var editName by remember { mutableStateOf(currentUser?.username ?: "") }
        var selectedAvatar by remember { mutableStateOf(currentUser?.avatarEmoji ?: "🐼") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color(0xFF1E1B4B),
            title = {
                Text(text = "Customize Profile", fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Choose Cartoon Avatar:", fontSize = 13.sp, color = Color.White)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(avatars) { av ->
                            val isSel = av == selectedAvatar
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) Color(0xFFF59E0B) else Color(0xFF334155))
                                    .border(2.dp, if (isSel) Color.White else Color.Transparent, CircleShape)
                                    .clickable { selectedAvatar = av },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = av, fontSize = 24.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.updateProfile(editName.trim(), selectedAvatar)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // --- EMAIL AUTH & REGISTRATION DIALOG ---
    if (showAuthDialog) {
        var isRegisterMode by remember { mutableStateOf(false) }
        var emailInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }
        var authError by remember { mutableStateOf<String?>(null) }
        var authLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!authLoading) showAuthDialog = false },
            containerColor = Color(0xFF1E1B4B),
            title = {
                Text(
                    text = if (isRegisterMode) "Create Firebase Account" else "Sign In to Firebase",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    authError?.let {
                        Text(text = it, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            authError = null
                        }
                    ) {
                        Text(
                            text = if (isRegisterMode) "Already have an account? Sign In" else "Need an account? Register",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !authLoading,
                    onClick = {
                        coroutineScope.launch {
                            authLoading = true
                            authError = null
                            val res = if (isRegisterMode) {
                                userAuthManager.registerWithEmail(emailInput.trim(), passwordInput.trim(), nameInput.trim())
                            } else {
                                userAuthManager.signInWithEmail(emailInput.trim(), passwordInput.trim())
                            }
                            authLoading = false
                            if (res.isSuccess) {
                                showAuthDialog = false
                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                currentUser?.let { databaseService.syncLocalUserToCloud(it) }
                            } else {
                                authError = res.exceptionOrNull()?.localizedMessage ?: "Authentication failed"
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (isRegisterMode) "Register" else "Sign In",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        modifier = modifier.border(1.dp, Color(0xFF64748B).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}
