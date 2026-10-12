package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.data.entity.CoinTransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletScreen(
    repository: GameRepository,
    onNavigatePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by repository.getCurrentUserFlow().collectAsState(initial = null)
    val transactions by repository.getTransactionsForCurrentUser().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "REWARDS" -> transactions.filter { it.amount > 0 }
            "BOOSTERS" -> transactions.filter { it.type == "BOOSTER_PURCHASE" || it.type == "SECOND_CHANCE" }
            "ADMIN" -> transactions.filter { it.type == "ADMIN_GRANT" }
            else -> transactions
        }
    }

    val totalEarned = remember(transactions) {
        transactions.filter { it.amount > 0 }.sumOf { it.amount }
    }
    val totalSpent = remember(transactions) {
        transactions.filter { it.amount < 0 }.sumOf { -it.amount }
    }

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
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Virtual Coin Wallet",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // --- 3D GOLDEN COIN BALANCE CARD ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFFDE047), Color(0xFFD97706))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .testTag("wallet_balance_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AVAILABLE BALANCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE047),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🪙", fontSize = 42.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${user?.totalCoins ?: 0}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFEF08A)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary Stats: Lifetime Earned & Spent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Earned", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                                Text(text = "+$totalEarned", fontWeight = FontWeight.Bold, color = Color(0xFF34D399), fontSize = 15.sp)
                            }
                        }

                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Spent", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                                Text(text = "-$totalSpent", fontWeight = FontWeight.Bold, color = Color(0xFFF87171), fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- DISCLAIMER NOTICE ---
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PuzzlePop coins are virtual in-game points used for boosters and puzzle progression.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // --- TRANSACTION HISTORY HEADER & FILTERS ---
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ledger & Transaction History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("ALL", "REWARDS", "BOOSTERS", "ADMIN")
                    items(filters) { f ->
                        FilterChip(
                            selected = selectedFilter == f,
                            onClick = { selectedFilter = f },
                            label = { Text(text = f, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // --- TRANSACTION LIST ---
        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found in this category.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredTransactions) { tx ->
                TransactionCard(transaction = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TransactionCard(transaction: CoinTransactionEntity) {
    val isCredit = transaction.amount > 0
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dateText = remember(transaction.timestamp) {
        dateFormat.format(Date(transaction.timestamp))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCredit) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f),
                RoundedCornerShape(14.dp)
            )
            .testTag("tx_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCredit) Color(0xFF10B981).copy(alpha = 0.2f)
                            else Color(0xFFEF4444).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (transaction.type) {
                            "LEVEL_REWARD" -> "🏆"
                            "DAILY_REWARD" -> "🎁"
                            "BOOSTER_PURCHASE" -> "⚡"
                            "SECOND_CHANCE" -> "💖"
                            "ADMIN_GRANT" -> "👑"
                            "WELCOME_BONUS" -> "🎉"
                            else -> "🪙"
                        },
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = transaction.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$dateText • ${transaction.status}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = if (isCredit) "+${transaction.amount}" else "${transaction.amount}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isCredit) Color(0xFF34D399) else Color(0xFFF87171)
            )
        }
    }
}
