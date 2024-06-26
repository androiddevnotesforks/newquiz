package com.infinitepower.newquiz.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infinitepower.newquiz.core.theme.NewQuizTheme
import com.infinitepower.newquiz.core.theme.spacing
import com.infinitepower.newquiz.core.ui.components.icon.button.BackIconButton
import com.infinitepower.newquiz.core.user_services.DateTimeRangeFormatter
import com.infinitepower.newquiz.core.user_services.model.User
import com.infinitepower.newquiz.feature.profile.components.MainUserCard
import com.infinitepower.newquiz.feature.profile.components.UserXpAndLevelCard
import com.infinitepower.newquiz.feature.profile.components.XpEarnedByDayCard
import com.infinitepower.newquiz.model.TimestampWithXP
import com.infinitepower.newquiz.core.R as CoreR
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

@Composable
@Destination
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileScreen(
    navigator: DestinationsNavigator,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackClick = navigator::popBackStack
    )
}

@Composable
@ExperimentalMaterial3Api
private fun ProfileScreen(
    uiState: ProfileScreenUiState,
    onEvent: (event: ProfileScreenUiEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val options = mapOf(
        DateTimeRangeFormatter.Day to stringResource(id = CoreR.string.today),
        DateTimeRangeFormatter.Week to stringResource(id = CoreR.string.this_week),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = CoreR.string.profile)) },
                navigationIcon = { BackIconButton(onClick = onBackClick) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
        ) {
            if (uiState.loading) {
                item { CircularProgressIndicator() }
            }

            uiState.user?.let { user ->
                item {
                    MainUserCard(
                        modifier = Modifier.fillParentMaxWidth(),
                        level = user.level,
                        levelProgress = user.getLevelProgress(),
                        userName = user.fullName,
                        userPhoto = user.imageUrl
                    )
                }

                item {
                    UserXpAndLevelCard(
                        modifier = Modifier.fillParentMaxWidth(),
                        totalXp = user.totalXp,
                        level = user.level
                    )
                }

                item {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillParentMaxWidth(),
                    ) {
                        options.onEachIndexed { index, (timeRange, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                onClick = {
                                    onEvent(ProfileScreenUiEvent.OnFilterByTimeRangeClick(timeRange))
                                },
                                selected = timeRange == uiState.selectedTimeRange
                            ) {
                                Text(text = label)
                            }
                        }
                    }
                }

                item {
                    OutlinedCard(
                        modifier = Modifier.fillParentMaxWidth()
                    ) {
                        XpEarnedByDayCard(
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                            formatter = uiState.selectedTimeRange,
                            xpEarnedList = uiState.xpEarnedList
                        )
                    }
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileScreenPreview() {
    val now = Clock.System.now()

    NewQuizTheme {
        ProfileScreen(
            uiState = ProfileScreenUiState(
                loading = false,
                user = User(
                    uid = "uid",
                    totalXp = 1235u
                ),
                selectedTimeRange = DateTimeRangeFormatter.Week,
                xpEarnedList = persistentListOf(
                    TimestampWithXP((now - 4.days).toEpochMilliseconds(), 20),
                    TimestampWithXP((now - 3.days).toEpochMilliseconds(), 10),
                    TimestampWithXP((now - 1.days).toEpochMilliseconds(), 30),
                    TimestampWithXP(now.toEpochMilliseconds(), 15)
                )
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}
