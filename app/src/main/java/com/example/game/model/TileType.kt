package com.example.game.model

import androidx.compose.ui.graphics.Color

/**
 * 25 rich 3D cartoon tile varieties organized across 5 distinct puzzle categories.
 */
enum class TileType(
    val displayName: String,
    val categoryId: String,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    // 1. Fruit Matching
    STRAWBERRY("Strawberry", "fruits", "🍓", Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFF84CC16)),
    ORANGE("Orange", "fruits", "🍊", Color(0xFFF97316), Color(0xFFEA580C), Color(0xFFFDE047)),
    BANANA("Banana", "fruits", "🍌", Color(0xFFFACC15), Color(0xFFEAB308), Color(0xFFCA8A04)),
    CHERRY("Cherry", "fruits", "🍒", Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFF4ADE80)),
    WATERMELON("Watermelon", "fruits", "🍉", Color(0xFF10B981), Color(0xFF059669), Color(0xFFF43F5E)),
    GRAPE("Grape", "fruits", "🍇", Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFFA78BFA)),

    // 2. Candy Matching
    CANDY_SWIRL("Candy Swirl", "candy", "🍬", Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF38BDF8)),
    LOLLIPOP("Lollipop", "candy", "🍭", Color(0xFFFF007F), Color(0xFFD8006C), Color(0xFFFFD700)),
    CHOCOLATE("Chocolate", "candy", "🍫", Color(0xFF92400E), Color(0xFF78350F), Color(0xFFFDE68A)),
    JELLY_BEEN("Jelly Bean", "candy", "🍮", Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFFEF3C7)),
    GUMMY_STAR("Gummy Star", "candy", "⭐", Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFFFFBEB)),

    // 3. Dessert Matching
    CUPCAKE("Cupcake", "dessert", "🧁", Color(0xFFEC4899), Color(0xFFDB2777), Color(0xFFFDE047)),
    DONUT("Donut", "dessert", "🍩", Color(0xFFD97706), Color(0xFFB45309), Color(0xFFF472B6)),
    ICE_CREAM("Ice Cream", "dessert", "🍦", Color(0xFF06B6D4), Color(0xFF0891B2), Color(0xFFFBBF24)),
    CAKE("Cake", "dessert", "🍰", Color(0xFFF43F5E), Color(0xFFE11D48), Color(0xFFFFF1F2)),
    COOKIE("Cookie", "dessert", "🍪", Color(0xFFB45309), Color(0xFF92400E), Color(0xFFFEF3C7)),

    // 4. Flower Matching
    BLOSSOM("Blossom", "flower", "🌸", Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFFEE2E2)),
    SUNFLOWER("Sunflower", "flower", "🌻", Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFFEF08A)),
    ROSE("Rose", "flower", "🌹", Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFFFECDD3)),
    CLOVER("Clover", "flower", "🍀", Color(0xFF22C55E), Color(0xFF16A34A), Color(0xFFBBF7D0)),
    TULIP("Tulip", "flower", "🌷", Color(0xFFFB7185), Color(0xFFF43F5E), Color(0xFFFFE4E6)),

    // 5. Toy Matching
    TEDDY_BEAR("Teddy Bear", "toys", "🧸", Color(0xFFB45309), Color(0xFF78350F), Color(0xFFFDE68A)),
    TOY_CAR("Toy Car", "toys", "🚗", Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFFFEF08A)),
    ROCKET("Rocket", "toys", "🚀", Color(0xFF3B82F6), Color(0xFF2563EB), Color(0xFFF59E0B)),
    BALL("Ball", "toys", "⚽", Color(0xFF64748B), Color(0xFF334155), Color(0xFFF8FAFC)),
    GEM("Gem", "toys", "💎", Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFFE0F2FE));

    val category: String
        get() = when (categoryId.lowercase()) {
            "fruits" -> "Fruit"
            "candy" -> "Candy"
            "dessert" -> "Dessert"
            "flower" -> "Flower"
            else -> "Toy"
        }

    companion object {
        fun fromName(name: String): TileType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: STRAWBERRY
        }

        fun getTilesForCategory(categoryId: String): List<TileType> {
            val list = entries.filter { it.categoryId.equals(categoryId, ignoreCase = true) }
            return list.ifEmpty { entries.take(5) }
        }
    }
}
