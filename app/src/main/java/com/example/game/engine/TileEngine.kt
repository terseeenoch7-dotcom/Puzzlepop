package com.example.game.engine

import com.example.game.model.LevelData
import com.example.game.model.LevelDifficulty
import com.example.game.model.TileConfig
import com.example.game.model.TileItem
import com.example.game.model.TileType
import kotlin.math.abs
import kotlin.random.Random

object TileEngine {

    /**
     * Recomputes isCovered for all board tiles.
     * Tile A is covered if any Tile B with layer > A.layer overlaps with A.
     */
    fun computeCoveredStates(tiles: List<TileItem>): List<TileItem> {
        return tiles.map { tileA ->
            val isCovered = tiles.any { tileB ->
                tileB.layer > tileA.layer &&
                        abs(tileA.x - tileB.x) < 0.82f &&
                        abs(tileA.y - tileB.y) < 0.82f
            }
            tileA.copy(isCovered = isCovered)
        }
    }

    /**
     * Finds an available tile that pairs with one already in the tray,
     * or any available tile that has matching counterparts.
     */
    fun findHint(boardTiles: List<TileItem>, trayTiles: List<TileItem>): TileItem? {
        val available = boardTiles.filter { !it.isCovered }
        if (available.isEmpty()) return null

        // 1. If tray has a tile of type X, prefer an available tile of type X
        val trayTypes = trayTiles.map { it.type }
        val matchingTray = available.firstOrNull { it.type in trayTypes }
        if (matchingTray != null) return matchingTray

        // 2. Otherwise pick an available tile that has at least 2 remaining on board
        val typeCounts = boardTiles.groupingBy { it.type }.eachCount()
        return available.firstOrNull { (typeCounts[it.type] ?: 0) >= 3 } ?: available.firstOrNull()
    }

    /**
     * Shuffles the types of all remaining tiles on board,
     * ensuring the exact same inventory counts are preserved.
     */
    fun shuffleBoardTypes(boardTiles: List<TileItem>): List<TileItem> {
        val shuffledTypes = boardTiles.map { it.type }.shuffled(Random(System.currentTimeMillis()))
        return boardTiles.mapIndexed { index, tile ->
            tile.copy(type = shuffledTypes[index])
        }
    }

    /**
     * Procedurally generates a solvable level layout with specified theme and difficulty.
     */
    fun generateLevel(
        levelNumber: Int,
        themeName: String = "Sweet Valley",
        difficulty: LevelDifficulty = LevelDifficulty.EASY,
        baseReward: Int = 5,
        categoryId: String = "fruits"
    ): LevelData {
        val tripletsCount = when (difficulty) {
            LevelDifficulty.EASY -> 4 + (levelNumber % 3) // 12 - 18 tiles
            LevelDifficulty.MEDIUM -> 7 + (levelNumber % 3) // 21 - 27 tiles
            LevelDifficulty.HARD -> 10 + (levelNumber % 4) // 30 - 39 tiles
            LevelDifficulty.EXPERT -> 13 + (levelNumber % 4) // 39 - 48 tiles
        }
        val totalTiles = tripletsCount * 3

        val categoryTiles = TileType.getTilesForCategory(categoryId)
        val selectedTypes = categoryTiles.shuffled(Random(levelNumber * 7919))
        val poolSize = selectedTypes.size.coerceAtLeast(3)

        // Distribute types evenly into triplets
        val tileTypesList = mutableListOf<TileType>()
        for (i in 0 until tripletsCount) {
            val type = selectedTypes[i % poolSize]
            repeat(3) { tileTypesList.add(type) }
        }
        tileTypesList.shuffle(Random(levelNumber * 31))

        // Position coordinates in multi-layer pyramid/cross/diamond
        val configs = mutableListOf<TileConfig>()
        val maxLayers = when (difficulty) {
            LevelDifficulty.EASY -> 2
            LevelDifficulty.MEDIUM -> 3
            else -> 4
        }

        var tileIdx = 0
        // Layer 0: base layer (wide grid)
        val l0Cols = 4
        val l0Rows = (totalTiles * 0.5f).toInt() / l0Cols + 1
        for (r in 0 until l0Rows) {
            for (c in 0 until l0Cols) {
                if (tileIdx < totalTiles && configs.size < totalTiles * 0.55f) {
                    configs.add(
                        TileConfig(
                            typeName = tileTypesList[tileIdx].name,
                            layer = 0,
                            x = c * 0.95f + 0.3f,
                            y = r * 0.95f + 0.3f
                        )
                    )
                    tileIdx++
                }
            }
        }

        // Layer 1: overlapping middle layer (offset by 0.45f)
        if (maxLayers >= 2) {
            for (r in 0 until 4) {
                for (c in 0 until 3) {
                    if (tileIdx < totalTiles && configs.size < totalTiles * 0.85f) {
                        configs.add(
                            TileConfig(
                                typeName = tileTypesList[tileIdx].name,
                                layer = 1,
                                x = c * 0.95f + 0.75f,
                                y = r * 0.95f + 0.75f
                            )
                        )
                        tileIdx++
                    }
                }
            }
        }

        // Layer 2+: peak pyramid layers
        var curLayer = 2
        while (tileIdx < totalTiles && curLayer <= maxLayers) {
            for (r in 0 until 3) {
                for (c in 0 until 2) {
                    if (tileIdx < totalTiles) {
                        configs.add(
                            TileConfig(
                                typeName = tileTypesList[tileIdx].name,
                                layer = curLayer,
                                x = c * 0.95f + 1.0f + (curLayer * 0.2f),
                                y = r * 0.95f + 1.0f + (curLayer * 0.2f)
                            )
                        )
                        tileIdx++
                    }
                }
            }
            curLayer++
        }

        // Fill any remaining in layer 0
        while (tileIdx < totalTiles) {
            val r = (tileIdx % 5)
            val c = (tileIdx / 5)
            configs.add(
                TileConfig(
                    typeName = tileTypesList[tileIdx].name,
                    layer = 0,
                    x = c * 0.95f + 0.2f,
                    y = r * 0.95f + 0.2f
                )
            )
            tileIdx++
        }

        val levelName = when (levelNumber) {
            1 -> "Sunny Orchard"
            2 -> "Berry Meadow"
            3 -> "Sweet Treats"
            4 -> "Candy Wonderland"
            5 -> "Starry Garden"
            6 -> "Magic Forest"
            7 -> "Panda's Bamboo"
            8 -> "Gemstone Mine"
            9 -> "Tropical Reef"
            10 -> "Cloud Kingdom"
            else -> "$themeName - Stage $levelNumber"
        }

        return LevelData(
            levelNumber = levelNumber,
            name = levelName,
            themeName = themeName,
            difficulty = difficulty,
            baseReward = baseReward,
            bonusReward = if (difficulty == LevelDifficulty.HARD || difficulty == LevelDifficulty.EXPERT) 5 else 2,
            timeLimitSeconds = 40, // Hidden game timer defaults to 40 seconds
            trayCapacity = 7,
            tiles = configs,
            isActive = true
        )
    }

    /**
     * Checks if a level's tile count is valid (must be a positive multiple of 3).
     */
    fun validateSolvabilityBasic(tiles: List<TileConfig>): Boolean {
        if (tiles.isEmpty() || tiles.size % 3 != 0) return false
        val counts = tiles.groupingBy { it.typeName }.eachCount()
        return counts.values.all { it % 3 == 0 }
    }
}
