package com.example.game.model

/**
 * Represents a single tile on the puzzle board.
 * Coordinates (x, y) are in grid units (e.g. 0f to 6f),
 * width and height default to 1f (each tile spans 1 unit).
 * layer is the vertical stacking level (0 = bottom, 1, 2...).
 */
data class TileItem(
    val id: String,
    val type: TileType,
    val layer: Int,
    val x: Float,
    val y: Float,
    val width: Float = 1.0f,
    val height: Float = 1.0f,
    val isCovered: Boolean = false,
    val isHighlighted: Boolean = false
) {
    /**
     * Checks if this tile overlaps horizontally and vertically with another tile.
     * Overlap occurs when horizontal distance < 0.9 and vertical distance < 0.9.
     */
    fun overlapsWith(other: TileItem): Boolean {
        val dx = kotlin.math.abs(this.x - other.x)
        val dy = kotlin.math.abs(this.y - other.y)
        return dx < 0.9f && dy < 0.9f
    }
}

/**
 * Move action record used for Undo booster.
 */
data class MoveAction(
    val tile: TileItem,
    val trayIndex: Int
)

/**
 * Booster types available to players.
 */
enum class BoosterType(
    val id: String,
    val displayName: String,
    val description: String,
    val iconEmoji: String,
    val defaultCost: Int
) {
    HINT("hint", "Hint", "Reveals an available matching tile", "💡", 15),
    SHUFFLE("shuffle", "Shuffle", "Rearranges remaining board tiles", "🔀", 20),
    UNDO("undo", "Undo", "Returns last tile from tray to board", "↩️", 10),
    EXTRA_SLOT("extra_slot", "Extra Slot", "Expands tray to 8 slots this round", "➕", 25),
    REMOVE_3("remove_3", "Clear 3", "Clears 3 tiles from full tray", "🧹", 30)
}
