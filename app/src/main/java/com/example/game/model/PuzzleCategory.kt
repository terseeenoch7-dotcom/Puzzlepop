package com.example.game.model

import androidx.compose.ui.graphics.Color

data class PuzzleCategory(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val startingLevel: Int = 1,
    val isEnabled: Boolean = true
) {
    companion object {
        val ALL_CATEGORIES = listOf(
            PuzzleCategory(
                id = "fruits",
                name = "Fruit Matching",
                description = "Juicy berries, tropical fruits, and fresh citrus slices",
                iconEmoji = "🍓",
                primaryColor = Color(0xFFEF4444),
                secondaryColor = Color(0xFFEA580C)
            ),
            PuzzleCategory(
                id = "candy",
                name = "Candy Matching",
                description = "Sugary swirls, lollipops, and magical candy twists",
                iconEmoji = "🍬",
                primaryColor = Color(0xFF8B5CF6),
                secondaryColor = Color(0xFF7C3AED)
            ),
            PuzzleCategory(
                id = "dessert",
                name = "Dessert Matching",
                description = "Frosted cupcakes, glazed donuts, and sweet ice cream",
                iconEmoji = "🧁",
                primaryColor = Color(0xFFEC4899),
                secondaryColor = Color(0xFFDB2777)
            ),
            PuzzleCategory(
                id = "flower",
                name = "Flower Matching",
                description = "Blooming blossoms, roses, sunflowers, and lucky clovers",
                iconEmoji = "🌸",
                primaryColor = Color(0xFF10B981),
                secondaryColor = Color(0xFF059669)
            ),
            PuzzleCategory(
                id = "toys",
                name = "Toy Matching",
                description = "Playful teddy bears, sports balls, rockets, and shiny gems",
                iconEmoji = "🧸",
                primaryColor = Color(0xFF3B82F6),
                secondaryColor = Color(0xFF1D4ED8)
            )
        )

        fun findById(id: String): PuzzleCategory {
            return ALL_CATEGORIES.firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: ALL_CATEGORIES.first()
        }
    }
}
