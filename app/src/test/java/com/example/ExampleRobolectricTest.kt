package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine.TileEngine
import com.example.game.model.LevelDifficulty
import com.example.game.model.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PuzzlePop", appName)
    }

    @Test
    fun `level generation produces valid triplets`() {
        val level = TileEngine.generateLevel(1, "Sunny Orchard", LevelDifficulty.EASY, baseReward = 5)
        assertNotNull(level)
        assertTrue("Tiles size must be a multiple of 3", level.tiles.size % 3 == 0)
        assertTrue("Solvability check passes", TileEngine.validateSolvabilityBasic(level.tiles))
    }

    @Test
    fun `tile types contain valid categories and emojis`() {
        val strawberry = TileType.STRAWBERRY
        assertEquals("🍓", strawberry.emoji)
        assertEquals("Fruit", strawberry.category)
    }
}
