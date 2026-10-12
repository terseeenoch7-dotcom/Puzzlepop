package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.TileItem
import com.example.game.model.TileType

/**
 * 3D Cartoon Glossy Puzzle Tile View.
 */
@Composable
fun GlossyTileView(
    tile: TileItem,
    sizeDp: Dp = 56.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCovered = tile.isCovered
    val isHighlighted = tile.isHighlighted
    val type = tile.type

    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            scaleAnim.animateTo(1.1f, animationSpec = spring())
            scaleAnim.animateTo(1.0f, animationSpec = spring())
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val baseGradient = if (isCovered) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFCBD5E1),
                Color(0xFF94A3B8)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                type.primaryColor.copy(alpha = 0.95f),
                type.secondaryColor
            )
        )
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .scale(if (isHighlighted) scaleAnim.value * pulseGlow else 1f)
            .testTag("tile_${tile.id}")
            .shadow(
                elevation = if (isCovered) 2.dp else (if (isHighlighted) 12.dp else 6.dp),
                shape = RoundedCornerShape(12.dp),
                ambientColor = if (isHighlighted) Color(0xFFFBBF24) else Color.Black.copy(alpha = 0.35f),
                spotColor = if (isHighlighted) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(baseGradient)
            .border(
                width = if (isHighlighted) 2.5.dp else 1.5.dp,
                color = if (isHighlighted) Color(0xFFFEF08A) else (if (isCovered) Color(0xFF64748B) else Color.White.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = !isCovered,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Specular 3D Gloss Highlight on top half
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizeDp / 2.2f)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isCovered) 0.15f else 0.55f),
                            Color.White.copy(alpha = 0.0f)
                        )
                    )
                )
        )

        // 3D Bottom Lip Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = if (isCovered) 0.3f else 0.25f))
        )

        // Cartoon Emoji Symbol
        Text(
            text = type.emoji,
            fontSize = (sizeDp.value * 0.48f).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-1).dp)
        )

        // Dim overlay and lock indicator if covered
        if (isCovered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f)),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = "🔒",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

/**
 * 3D Cartoon Button with realistic bottom bevel depth and tactile feel.
 */
@Composable
fun Cartoon3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF10B981),
    shadowColor: Color = Color(0xFF047857),
    textColor: Color = Color.White,
    icon: String? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    testTag: String = "cartoon_button"
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .height(height)
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Bottom bevel shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 4.dp)
                .background(
                    if (enabled) shadowColor else Color(0xFF94A3B8),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // Top button face
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.verticalGradient(
                            listOf(
                                containerColor.copy(alpha = 0.95f),
                                containerColor
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = if (enabled) 0.6f else 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Specular top highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2.5f)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (icon != null) {
                    Text(text = icon, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                }
                Text(
                    text = text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * 3D Golden Coin Pill Badge with tap action to view Wallet.
 */
@Composable
fun CoinBadge(
    coins: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1B4B).copy(alpha = 0.85f),
        modifier = modifier
            .border(1.5.dp, Color(0xFFFBBF24), RoundedCornerShape(20.dp))
            .testTag("coin_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "🪙",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 5.dp)
            )
            Text(
                text = coins.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFFEF08A)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 3D Bamboo/Wooden Collection Tray.
 */
@Composable
fun CollectionTrayView(
    tiles: List<TileItem>,
    maxSlots: Int = 7,
    slotSize: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val isWarning = tiles.size >= maxSlots - 1

    Box(
        modifier = modifier
            .testTag("collection_tray")
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isWarning) {
                        listOf(Color(0xFF991B1B), Color(0xFF450A0A))
                    } else {
                        listOf(Color(0xFF451A03), Color(0xFF290E02))
                    }
                )
            )
            .border(
                width = 3.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (isWarning) Color(0xFFEF4444) else Color(0xFFD97706),
                        if (isWarning) Color(0xFF7F1D1D) else Color(0xFF78350F)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until maxSlots) {
                val tile = tiles.getOrNull(i)
                if (tile != null) {
                    GlossyTileView(
                        tile = tile,
                        sizeDp = slotSize,
                        onClick = {} // Tray tiles are not clickable
                    )
                } else {
                    // Empty recessed slot
                    Box(
                        modifier = Modifier
                            .size(slotSize)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color(0xFF78350F).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(slotSize / 3)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                }
            }
        }
    }
}
