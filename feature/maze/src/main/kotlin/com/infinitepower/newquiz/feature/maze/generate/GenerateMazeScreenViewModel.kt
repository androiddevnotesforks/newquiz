package com.infinitepower.newquiz.feature.maze.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.infinitepower.newquiz.core.ui.SnackbarController
import com.infinitepower.newquiz.data.worker.maze.GenerateMazeQuizWorker
import com.infinitepower.newquiz.domain.repository.comparison_quiz.ComparisonQuizRepository
import com.infinitepower.newquiz.feature.maze.common.MazeCategories
import com.infinitepower.newquiz.model.BaseCategory
import com.infinitepower.newquiz.model.GameMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenerateMazeScreenViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val comparisonQuizRepository: ComparisonQuizRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GenerateMazeScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    multiChoiceCategories = MazeCategories.getMultiChoiceCategories()
                        .toImmutableList(),
                    wordleCategories = MazeCategories.availableWordleCategories.toImmutableList(),
                    comparisonQuizCategories = getComparisonQuizCategories().toImmutableList(),
                    loading = false
                )
            }
        }
    }

    fun onEvent(event: GenerateMazeScreenUiEvent) {
        when (event) {
            is GenerateMazeScreenUiEvent.SelectCategory -> selectCategory(event.category)
            is GenerateMazeScreenUiEvent.GenerateMaze -> generateMaze(seed = event.seed)
            is GenerateMazeScreenUiEvent.SelectAllCategories -> selectAllCategories()
            is GenerateMazeScreenUiEvent.SelectOnlyOfflineCategories -> selectOnlyOffline()
            is GenerateMazeScreenUiEvent.SelectCategories -> selectCategories(
                gameMode = event.gameMode,
                selectAll = event.selectAll
            )
        }
    }

    /**
     * Select or deselect a category, depending on the current state.
     */
    private fun selectCategory(category: BaseCategory) {
        _uiState.update { currentState ->
            // Get the list of selected categories, depending on the game mode.
            val selectedCategories = when (category.gameMode) {
                GameMode.MULTI_CHOICE -> currentState.selectedMultiChoiceCategories
                GameMode.WORDLE -> currentState.selectedWordleCategories
                GameMode.COMPARISON_QUIZ -> currentState.selectedComparisonQuizCategories
            }.toMutableList()

            // If the category is already selected, then deselect it.
            // If the category is not selected, then select it.
            if (selectedCategories.contains(category)) {
                selectedCategories.remove(category)
            } else {
                selectedCategories.add(category)
            }

            // Update the state, depending on the game mode.
            when (category.gameMode) {
                GameMode.MULTI_CHOICE -> currentState.copy(
                    selectedMultiChoiceCategories = selectedCategories.toImmutableList()
                )

                GameMode.WORDLE -> currentState.copy(
                    selectedWordleCategories = selectedCategories.toImmutableList()
                )

                GameMode.COMPARISON_QUIZ -> currentState.copy(
                    selectedComparisonQuizCategories = selectedCategories.toImmutableList()
                )
            }
        }
    }

    private fun selectAllCategories() {
        selectMultiChoiceCategories(selectAll = true)
        selectWordleCategories(selectAll = true)
        selectComparisonQuizCategories(selectAll = true)
    }

    private fun selectOnlyOffline() {
        _uiState.update { currentState ->
            val multiChoiceOnlyOffline = currentState.multiChoiceCategories.filter { category ->
                !category.requireInternetConnection
            }

            val wordleOnlyOffline = currentState.wordleCategories.filter { category ->
                !category.requireInternetConnection
            }

            val comparisonQuizOnlyOffline =
                currentState.comparisonQuizCategories.filter { category ->
                    !category.requireInternetConnection
                }

            currentState.copy(
                selectedMultiChoiceCategories = multiChoiceOnlyOffline.toImmutableList(),
                selectedWordleCategories = wordleOnlyOffline.toImmutableList(),
                selectedComparisonQuizCategories = comparisonQuizOnlyOffline.toImmutableList()
            )
        }
    }

    private fun selectCategories(
        gameMode: GameMode,
        selectAll: Boolean,
    ) {
        when (gameMode) {
            GameMode.MULTI_CHOICE -> selectMultiChoiceCategories(selectAll)
            GameMode.WORDLE -> selectWordleCategories(selectAll)
            GameMode.COMPARISON_QUIZ -> selectComparisonQuizCategories(selectAll)
        }
    }

    private fun selectMultiChoiceCategories(selectAll: Boolean) {
        _uiState.update { currentState ->
            if (selectAll) {
                currentState.copy(selectedMultiChoiceCategories = currentState.multiChoiceCategories)
            } else {
                currentState.copy(selectedMultiChoiceCategories = persistentListOf())
            }
        }
    }

    private fun selectWordleCategories(selectAll: Boolean) {
        _uiState.update { currentState ->
            if (selectAll) {
                currentState.copy(selectedWordleCategories = currentState.wordleCategories)
            } else {
                currentState.copy(selectedWordleCategories = persistentListOf())
            }
        }
    }

    private fun selectComparisonQuizCategories(selectAll: Boolean) {
        _uiState.update { currentState ->
            if (selectAll) {
                currentState.copy(selectedComparisonQuizCategories = currentState.comparisonQuizCategories)
            } else {
                currentState.copy(selectedComparisonQuizCategories = persistentListOf())
            }
        }
    }

    private fun generateMaze(seed: Int?) {
        viewModelScope.launch {
            val currentState = uiState.first()

            val workId = GenerateMazeQuizWorker.enqueue(
                workManager = workManager,
                seed = seed,
                multiChoiceCategories = currentState.selectedMultiChoiceCategories,
                wordleCategories = currentState.selectedWordleCategories,
                comparisonQuizCategories = currentState.selectedComparisonQuizCategories
            )

            workManager
                .getWorkInfoByIdLiveData(workId)
                .asFlow()
                .onEach { workInfo ->
                    if (workInfo?.state == WorkInfo.State.FAILED) {
                        SnackbarController.sendShortMessage("Failed to generate maze")
                    }

                    _uiState.update { currentState ->
                        val isFinished = workInfo?.state?.isFinished ?: false
                        currentState.copy(generatingMaze = !isFinished)
                    }
                }.launchIn(viewModelScope)
        }
    }

    private fun getComparisonQuizCategories(): List<BaseCategory> {
        return comparisonQuizRepository.getCategories().filterNot { it.isMazeDisabled }
    }
}
