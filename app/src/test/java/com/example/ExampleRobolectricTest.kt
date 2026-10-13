package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FirestoreLevel
import com.example.data.FirestoreTask
import com.example.data.GameRepository
import com.example.game.engine.TileEngine
import com.example.game.model.LevelDifficulty
import com.example.game.model.TileType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    @Test
    fun `firestore task and level models serialization to map`() {
        val task = FirestoreTask(
            id = "task_test_1",
            title = "Test Quest",
            description = "Match 10 gems",
            targetProgress = 10,
            rewardCoins = 50,
            isActive = true
        )
        val map = task.toMap()
        assertEquals("task_test_1", map["id"])
        assertEquals("Test Quest", map["title"])
        assertEquals(50, map["rewardCoins"])

        val level = FirestoreLevel(
            levelNumber = 1,
            name = "Test Level",
            baseReward = 10
        )
        val lvlMap = level.toMap()
        assertEquals(1, lvlMap["levelNumber"])
        assertEquals(10, lvlMap["baseReward"])
    }

    @Test
    fun `admin create and delete task in repository`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repository = GameRepository(db, null)

        val createResult = repository.adminCreateTask(
            title = "Daily Challenge",
            description = "Clear 3 boards",
            targetProgress = 3,
            rewardCoins = 30,
            adminName = "TestAdmin"
        )
        assertTrue(createResult.isSuccess)

        val missions = repository.getMissionsFlow().first()
        val found = missions.any { it.title == "Daily Challenge" }
        assertTrue("Newly created mission should be present", found)
    }
}
