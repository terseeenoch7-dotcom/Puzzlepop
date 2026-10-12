package com.example.game.model

/**
 * Difficulty tiers for puzzle levels.
 */
enum class LevelDifficulty(val label: String, val starsNeeded: Int) {
    EASY("Easy", 1),
    MEDIUM("Medium", 2),
    HARD("Hard", 3),
    EXPERT("Expert", 3)
}

/**
 * Definition of a single tile inside a level template.
 */
data class TileConfig(
    val typeName: String,
    val layer: Int,
    val x: Float,
    val y: Float
)

/**
 * Complete level metadata and tile arrangement.
 */
data class LevelData(
    val levelNumber: Int,
    val name: String,
    val themeName: String,
    val difficulty: LevelDifficulty = LevelDifficulty.EASY,
    val baseReward: Int = 5,
    val bonusReward: Int = 2,
    val timeLimitSeconds: Int = 0, // 0 = unlimited
    val trayCapacity: Int = 7,
    val tiles: List<TileConfig>,
    val isActive: Boolean = true,
    val highStars: Int = 0,
    val isCompleted: Boolean = false
) {
    val totalTilesCount: Int get() = tiles.size
    val totalTripletsCount: Int get() = tiles.size / 3
}
