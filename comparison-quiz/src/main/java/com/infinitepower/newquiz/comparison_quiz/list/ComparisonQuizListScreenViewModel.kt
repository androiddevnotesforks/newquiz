package com.infinitepower.newquiz.comparison_quiz.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitepower.newquiz.core.network.NetworkStatusTracker
import com.infinitepower.newquiz.domain.repository.home.RecentCategoriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonQuizListScreenViewModel @Inject constructor(
    private val recentCategoriesRepository: RecentCategoriesRepository,
    private val networkStatusTracker: NetworkStatusTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(ComparisonQuizListScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        networkStatusTracker
            .isOnline
            .onEach { isOnline ->
                _uiState.update { currentState ->
                    currentState.copy(internetConnectionAvailable = isOnline)
                }
            }.flatMapLatest { isOnline ->
                recentCategoriesRepository.getComparisonCategories(isInternetAvailable = isOnline)
            }.onEach { homeCategories ->
                _uiState.update { currentState ->
                    currentState.copy(homeCategories = homeCategories)
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: ComparisonQuizListScreenUiEvent) {
        when (event) {
            is ComparisonQuizListScreenUiEvent.SelectMode -> {
                _uiState.update { currentState ->
                    currentState.copy(selectedMode = event.mode)
                }
            }
        }
    }
}