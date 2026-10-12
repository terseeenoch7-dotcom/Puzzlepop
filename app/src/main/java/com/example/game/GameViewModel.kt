package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiLevelService
import com.example.auth.UserAuthManager
import com.example.data.AppDatabase
import com.example.data.DatabaseService
import com.example.data.GameRepository
import com.example.game.engine.TileEngine
import com.example.game.model.LevelData
import com.example.game.model.MoveAction
import com.example.game.model.TileItem
import com.example.game.model.TileType
import com.example.util.SoundEffects
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GameUiState(
    val levelData: LevelData? = null,
    val boardTiles: List<TileItem> = emptyList(),
    val trayTiles: List<TileItem> = emptyList(),
    val isVictory: Boolean = false,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val isTimeout: Boolean = false,
    val extraTimeClaimsCount: Int = 0,
    val maxExtraTimeClaims: Int = 3,
    val extraTimeSecondsToAdd: Int = 20,
    val activeCategoryId: String = "fruits",
    val matchesCount: Int = 0,
    val coinsAwarded: Int = 0,
    val starsAwarded: Int = 0,
    val extraSlotActive: Boolean = false,
    val maxTraySlots: Int = 7,
    val elapsedSeconds: Int = 0,
    val timeLimit: Int = 40,
    val aiMessage: String? = null,
    val isLoading: Boolean = false,
    val hintTileId: String? = null,
    val adMessage: String? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val databaseService = DatabaseService(application)
    val userAuthManager = UserAuthManager(application)
    val repository = GameRepository(database, databaseService)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<MoveAction>()
    private var timerJob: Job? = null

    fun loadLevel(levelNumber: Int, categoryId: String = "fruits") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isVictory = false, isGameOver = false, isTimeout = false, isPaused = false) }
            val level = repository.getLevel(levelNumber) ?: TileEngine.generateLevel(levelNumber, categoryId = categoryId)

            // Convert TileConfig list to TileItem list with UUIDs
            val boardItems = level.tiles.mapIndexed { idx, cfg ->
                TileItem(
                    id = "t_${levelNumber}_${idx}_${UUID.randomUUID().toString().take(6)}",
                    type = TileType.fromName(cfg.typeName),
                    layer = cfg.layer,
                    x = cfg.x,
                    y = cfg.y
                )
            }

            val computed = TileEngine.computeCoveredStates(boardItems)
            undoStack.clear()

            _uiState.value = GameUiState(
                levelData = level,
                boardTiles = computed,
                trayTiles = emptyList(),
                maxTraySlots = level.trayCapacity,
                timeLimit = if (level.timeLimitSeconds > 0) level.timeLimitSeconds else 40,
                activeCategoryId = categoryId,
                isLoading = false
            )

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value
                if (current.isVictory || current.isGameOver || current.isPaused || current.isTimeout) continue

                val newElapsed = current.elapsedSeconds + 1
                if (current.timeLimit > 0 && newElapsed >= current.timeLimit) {
                    onTimeExpired()
                    break
                }
                _uiState.update { it.copy(elapsedSeconds = newElapsed) }
            }
        }
    }

    private fun onTimeExpired() {
        SoundEffects.playTrayFullAlert()
        _uiState.update { it.copy(isPaused = true, isTimeout = true) }
    }

    fun claimExtraTimeWithAd(activity: android.app.Activity) {
        val current = _uiState.value
        if (current.extraTimeClaimsCount >= current.maxExtraTimeClaims) {
            _uiState.update { it.copy(isGameOver = true, isTimeout = false) }
            return
        }

        com.example.ads.AdMobManager.showRewardedAd(
            activity = activity,
            placementId = com.example.ads.AdMobManager.PLACEMENT_EXTRA_TIME,
            onRewardEarned = { _, _ ->
                SoundEffects.playBoosterUse()
                val addedSeconds = _uiState.value.extraTimeSecondsToAdd
                val newLimit = _uiState.value.timeLimit + addedSeconds
                _uiState.update {
                    it.copy(
                        timeLimit = newLimit,
                        isTimeout = false,
                        isPaused = false,
                        extraTimeClaimsCount = it.extraTimeClaimsCount + 1,
                        adMessage = "+$addedSeconds Seconds Extra Time Added!"
                    )
                }
                startTimer()
            },
            onDismissedOrFailed = { error ->
                _uiState.update { it.copy(adMessage = error ?: "Ad dismissed.") }
            }
        )
    }

    fun declineTimeoutExtraTime() {
        _uiState.update { it.copy(isTimeout = false, isGameOver = true) }
        viewModelScope.launch {
            _uiState.value.levelData?.let { repository.recordLevelFailure(it.levelNumber) }
        }
    }

    fun claimDoubleLevelRewardWithAd(activity: android.app.Activity) {
        com.example.ads.AdMobManager.showRewardedAd(
            activity = activity,
            placementId = com.example.ads.AdMobManager.PLACEMENT_DOUBLE_LEVEL_REWARD,
            onRewardEarned = { _, _ ->
                viewModelScope.launch {
                    val currentAwarded = _uiState.value.coinsAwarded
                    if (currentAwarded > 0) {
                        SoundEffects.playCoinReward()
                        repository.getCurrentUser()?.let { u ->
                            repository.adminGrantCoins(u.id, currentAwarded, "Watched Rewarded Ad: 2x Level Coins", "AdMob")
                        }
                        _uiState.update {
                            it.copy(
                                coinsAwarded = currentAwarded * 2,
                                adMessage = "Coins Doubled! (+${currentAwarded} Extra Coins)"
                            )
                        }
                    }
                }
            },
            onDismissedOrFailed = { error ->
                _uiState.update { it.copy(adMessage = error) }
            }
        )
    }

    fun claimSecondChanceWithAd(activity: android.app.Activity) {
        com.example.ads.AdMobManager.showRewardedAd(
            activity = activity,
            placementId = com.example.ads.AdMobManager.PLACEMENT_SECOND_CHANCE,
            onRewardEarned = { _, _ ->
                SoundEffects.playBoosterUse()
                val tray = _uiState.value.trayTiles.toMutableList()
                val removed = mutableListOf<TileItem>()
                repeat(3) {
                    if (tray.isNotEmpty()) {
                        removed.add(tray.removeAt(tray.size - 1))
                    }
                }
                val newBoard = _uiState.value.boardTiles + removed
                val computed = TileEngine.computeCoveredStates(newBoard)

                _uiState.update {
                    it.copy(
                        isGameOver = false,
                        isPaused = false,
                        trayTiles = tray,
                        boardTiles = computed,
                        extraSlotActive = true,
                        adMessage = "Second Chance Activated! 3 Tiles Cleared."
                    )
                }
                startTimer()
            },
            onDismissedOrFailed = { error ->
                _uiState.update { it.copy(adMessage = error) }
            }
        )
    }

    fun onTileClicked(tile: TileItem) {
        val state = _uiState.value
        if (state.isVictory || state.isGameOver || state.isPaused) return
        if (tile.isCovered) return

        val maxSlots = state.maxTraySlots + (if (state.extraSlotActive) 1 else 0)
        if (state.trayTiles.size >= maxSlots) {
            SoundEffects.playTrayFullAlert()
            return
        }

        SoundEffects.playTilePop()

        // 1. Remove from board
        val remainingBoard = state.boardTiles.filter { it.id != tile.id }
        val updatedBoard = TileEngine.computeCoveredStates(remainingBoard)

        // 2. Insert into tray (place next to existing tile of same type for tidy stacking)
        val newTray = state.trayTiles.toMutableList()
        val existingIndex = newTray.indexOfLast { it.type == tile.type }
        val insertIndex = if (existingIndex != -1) existingIndex + 1 else newTray.size
        newTray.add(insertIndex, tile)

        // Push to undo stack
        undoStack.add(MoveAction(tile, insertIndex))

        _uiState.update {
            it.copy(
                boardTiles = updatedBoard,
                trayTiles = newTray,
                hintTileId = null
            )
        }

        // 3. Check for 3 matching tiles in tray
        viewModelScope.launch {
            checkMatchesAndStatus(maxSlots)
        }
    }

    private suspend fun checkMatchesAndStatus(maxSlots: Int) {
        val currentTray = _uiState.value.trayTiles
        val typeCount = currentTray.groupingBy { it.type }.eachCount()
        val matchedType = typeCount.entries.firstOrNull { it.value >= 3 }?.key

        if (matchedType != null) {
            delay(120) // brief beat for visual insertion
            SoundEffects.playTripleMatch()

            // Remove 3 tiles of this type
            val newTray = currentTray.toMutableList()
            var removed = 0
            val iterator = newTray.iterator()
            while (iterator.hasNext() && removed < 3) {
                if (iterator.next().type == matchedType) {
                    iterator.remove()
                    removed++
                }
            }

            val newMatchesCount = _uiState.value.matchesCount + 1
            _uiState.update {
                it.copy(
                    trayTiles = newTray,
                    matchesCount = newMatchesCount
                )
            }
        }

        // 4. Check Victory or Defeat
        val remainingBoard = _uiState.value.boardTiles
        val remainingTray = _uiState.value.trayTiles

        if (remainingBoard.isEmpty() && remainingTray.isEmpty()) {
            // VICTORY!
            timerJob?.cancel()
            SoundEffects.playVictory()
            val level = _uiState.value.levelData ?: return

            val stars = when {
                _uiState.value.elapsedSeconds <= 45 -> 3
                _uiState.value.elapsedSeconds <= 90 -> 2
                else -> 1
            }

            val coins = repository.completeLevel(
                levelNumber = level.levelNumber,
                stars = stars,
                matchesCount = _uiState.value.matchesCount,
                timeTakenSeconds = _uiState.value.elapsedSeconds
            )

            _uiState.update {
                it.copy(
                    isVictory = true,
                    starsAwarded = stars,
                    coinsAwarded = coins
                )
            }
        } else if (remainingTray.size >= maxSlots) {
            // Defeat (Tray full without match)
            delay(150)
            SoundEffects.playTrayFullAlert()
            _uiState.update { it.copy(isGameOver = true) }
            _uiState.value.levelData?.let { repository.recordLevelFailure(it.levelNumber) }
        }
    }

    fun useHint() {
        viewModelScope.launch {
            val level = _uiState.value.levelData ?: return@launch
            val cost = 15
            val success = repository.spendCoinsForBooster("Hint", cost, level.levelNumber)
            if (!success) {
                SoundEffects.playTrayFullAlert()
                return@launch
            }

            SoundEffects.playBoosterUse()
            val hintTile = TileEngine.findHint(_uiState.value.boardTiles, _uiState.value.trayTiles)
            if (hintTile != null) {
                _uiState.update { it.copy(hintTileId = hintTile.id) }
            }
        }
    }

    fun useShuffle() {
        viewModelScope.launch {
            val level = _uiState.value.levelData ?: return@launch
            val cost = 20
            val success = repository.spendCoinsForBooster("Shuffle", cost, level.levelNumber)
            if (!success) {
                SoundEffects.playTrayFullAlert()
                return@launch
            }

            SoundEffects.playBoosterUse()
            val shuffled = TileEngine.shuffleBoardTypes(_uiState.value.boardTiles)
            val computed = TileEngine.computeCoveredStates(shuffled)
            _uiState.update { it.copy(boardTiles = computed, hintTileId = null) }
        }
    }

    fun useUndo() {
        viewModelScope.launch {
            if (undoStack.isEmpty() || _uiState.value.trayTiles.isEmpty()) return@launch
            val level = _uiState.value.levelData ?: return@launch
            val cost = 10
            val success = repository.spendCoinsForBooster("Undo", cost, level.levelNumber)
            if (!success) {
                SoundEffects.playTrayFullAlert()
                return@launch
            }

            SoundEffects.playBoosterUse()
            val lastAction = undoStack.removeAt(undoStack.size - 1)
            val currentTray = _uiState.value.trayTiles.toMutableList()

            // Remove from tray
            val tileIdx = currentTray.indexOfFirst { it.id == lastAction.tile.id }
            if (tileIdx != -1) {
                val popped = currentTray.removeAt(tileIdx)
                val newBoard = _uiState.value.boardTiles + popped
                val computed = TileEngine.computeCoveredStates(newBoard)
                _uiState.update {
                    it.copy(
                        trayTiles = currentTray,
                        boardTiles = computed,
                        hintTileId = null
                    )
                }
            }
        }
    }

    fun useExtraSlot() {
        viewModelScope.launch {
            if (_uiState.value.extraSlotActive) return@launch
            val level = _uiState.value.levelData ?: return@launch
            val cost = 25
            val success = repository.spendCoinsForBooster("Extra Slot", cost, level.levelNumber)
            if (!success) {
                SoundEffects.playTrayFullAlert()
                return@launch
            }

            SoundEffects.playBoosterUse()
            _uiState.update { it.copy(extraSlotActive = true) }
        }
    }

    fun useSecondChance() {
        viewModelScope.launch {
            val level = _uiState.value.levelData ?: return@launch
            val cost = 20
            val success = repository.spendCoinsForSecondChance(cost, level.levelNumber)
            if (!success) {
                SoundEffects.playTrayFullAlert()
                return@launch
            }

            SoundEffects.playBoosterUse()
            // Clear last 3 tray tiles and put them back onto board
            val tray = _uiState.value.trayTiles.toMutableList()
            val removed = mutableListOf<TileItem>()
            repeat(3) {
                if (tray.isNotEmpty()) {
                    removed.add(tray.removeAt(tray.size - 1))
                }
            }

            val newBoard = _uiState.value.boardTiles + removed
            val computed = TileEngine.computeCoveredStates(newBoard)

            _uiState.update {
                it.copy(
                    isGameOver = false,
                    trayTiles = tray,
                    boardTiles = computed,
                    extraSlotActive = true
                )
            }
        }
    }

    fun askGeminiOracle() {
        viewModelScope.launch {
            _uiState.update { it.copy(aiMessage = "Consulting Gemini AI Oracle...") }
            val level = _uiState.value.levelData
            val advice = GeminiLevelService.analyzePuzzleDifficulty(
                levelName = level?.name ?: "Level",
                tilesCount = _uiState.value.boardTiles.size,
                layersCount = 3
            )
            _uiState.update { it.copy(aiMessage = advice) }
        }
    }

    fun dismissAiMessage() {
        _uiState.update { it.copy(aiMessage = null) }
    }

    fun pauseGame() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeGame() {
        _uiState.update { it.copy(isPaused = false) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
