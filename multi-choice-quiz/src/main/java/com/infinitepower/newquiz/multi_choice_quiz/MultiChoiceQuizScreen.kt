package com.infinitepower.newquiz.multi_choice_quiz

import androidx.annotation.Keep
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.infinitepower.newquiz.core.navigation.MazeNavigator
import com.infinitepower.newquiz.core.theme.NewQuizTheme
import com.infinitepower.newquiz.core.ui.components.skip_question.SkipQuestionDialog
import com.infinitepower.newquiz.core.util.toAndroidUri
import com.infinitepower.newquiz.model.RemainingTime
import com.infinitepower.newquiz.model.multi_choice_quiz.MultiChoiceBaseCategory
import com.infinitepower.newquiz.model.multi_choice_quiz.MultiChoiceQuestion
import com.infinitepower.newquiz.model.multi_choice_quiz.MultiChoiceQuestionStep
import com.infinitepower.newquiz.model.multi_choice_quiz.SelectedAnswer
import com.infinitepower.newquiz.model.multi_choice_quiz.getBasicMultiChoiceQuestion
import com.infinitepower.newquiz.multi_choice_quiz.components.CardQuestionAnswers
import com.infinitepower.newquiz.multi_choice_quiz.components.MultiChoiceQuizContainer
import com.infinitepower.newquiz.multi_choice_quiz.components.QuizStepViewRow
import com.infinitepower.newquiz.multi_choice_quiz.components.QuizTopBar
import com.infinitepower.newquiz.multi_choice_quiz.destinations.MultiChoiceQuizResultsScreenDestination
import com.infinitepower.newquiz.multi_choice_quiz.destinations.MultiChoiceQuizScreenDestination
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

internal val MULTI_CHOICE_QUIZ_COUNTDOWN_TIME = 30.seconds

@Keep
data class MultiChoiceQuizScreenNavArg(
    val initialQuestions: ArrayList<MultiChoiceQuestion> = arrayListOf(),
    val category: MultiChoiceBaseCategory = MultiChoiceBaseCategory.Normal(),
    val difficulty: String? = null,
    val mazeItemId: String? = null
)

@Composable
@Destination(
    navArgsDelegate = MultiChoiceQuizScreenNavArg::class,
    deepLinks = [
        DeepLink(uriPattern = "newquiz://quickquiz")
    ]
)
@OptIn(ExperimentalMaterial3Api::class)
fun MultiChoiceQuizScreen(
    navigator: DestinationsNavigator,
    mazeNavigator: MazeNavigator,
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: QuizScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isGameEnded) {
        if (uiState.isGameEnded) {
            val backStackEntry = navController.getBackStackEntry(MultiChoiceQuizScreenDestination.route)
            val args = MultiChoiceQuizScreenDestination.argsFrom(backStackEntry)

            navigateToResultsScreen(
                navigator = navigator,
                mazeNavigator = mazeNavigator,
                args = args,
                questionSteps = uiState.questionSteps.filterIsInstance<MultiChoiceQuestionStep.Completed>()
            )
        }
    }

    MultiChoiceQuizScreenImpl(
        onBackClick = navigator::popBackStack,
        windowSizeClass = windowSizeClass,
        uiState = uiState,
        onEvent = viewModel::onEvent
    )

    SkipQuestionDialog(
        userDiamonds = uiState.userDiamonds,
        skipCost = uiState.skipCost,
        loading = uiState.userDiamondsLoading,
        onSkipClick = { viewModel.onEvent(MultiChoiceQuizScreenUiEvent.SkipQuestion) },
        onDismissClick = { viewModel.onEvent(MultiChoiceQuizScreenUiEvent.CleanUserSkipQuestionDiamonds) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiChoiceQuizScreenImpl(
    uiState: MultiChoiceQuizScreenUiState,
    windowSizeClass: WindowSizeClass,
    onBackClick: () -> Unit,
    onEvent: (event: MultiChoiceQuizScreenUiEvent) -> Unit
) {
    val context = LocalContext.current

    val imageLoader = ImageLoader
        .Builder(context)
        .components {
            add(SvgDecoder.Factory())
        }.build()

    val widthCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val currentQuestion = uiState.currentQuestionStep?.question

    MultiChoiceQuizContainer(
        modifier = Modifier.fillMaxSize(),
        loading = uiState.loading,
        windowSizeClass = windowSizeClass,
        answerSelected = uiState.selectedAnswer.isSelected,
        onVerifyQuestionClick = { onEvent(MultiChoiceQuizScreenUiEvent.VerifyAnswer) },
        topBarContent = {
            QuizTopBar(
                remainingTime = uiState.remainingTime,
                windowHeightSizeClass = windowSizeClass.heightSizeClass,
                skipsAvailable = uiState.skipsAvailable,
                onBackClick = onBackClick,
                onSkipClick = { onEvent(MultiChoiceQuizScreenUiEvent.GetUserSkipQuestionDiamonds) },
                onSaveClick = { onEvent(MultiChoiceQuizScreenUiEvent.SaveQuestion) },
                modifier = Modifier
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .fillMaxWidth(),
                currentQuestionNull = uiState.currentQuestionStep == null,
                questionSaved = uiState.questionSaved
            )
        },
        stepsContent = {
            QuizStepViewRow(
                questionSteps = uiState.questionSteps,
                isResultsScreen = false
            )
        },
        questionPositionContent = {
            Text(
                text = uiState.getQuestionPositionFormatted(),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        questionDescriptionContent = {
            if (currentQuestion != null) {
                Text(
                    text = currentQuestion.description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        questionImageContent = {
            currentQuestion?.image?.let { image ->
                val imageScale = if (currentQuestion.category == MultiChoiceBaseCategory.Logo) {
                    ContentScale.FillHeight
                } else ContentScale.Crop

                val maxWidth = if (widthCompact) 1f else 0.4f

                Box(
                    modifier = Modifier.fillMaxWidth(maxWidth)
                ) {
                    AsyncImage(
                        model = image.toAndroidUri(),
                        contentDescription = "Image",
                        modifier = Modifier
                            .aspectRatio(16 / 9f)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = imageScale,
                        imageLoader = imageLoader
                    )
                }
            }
        },
        answersContent = {
            CardQuestionAnswers(
                answers = currentQuestion?.answers.orEmpty(),
                selectedAnswer = uiState.selectedAnswer,
                onOptionClick = { answer ->
                    onEvent(MultiChoiceQuizScreenUiEvent.SelectAnswer(answer))
                }
            )
        }
    )
}

private fun navigateToResultsScreen(
    navigator: DestinationsNavigator,
    mazeNavigator: MazeNavigator,
    args: MultiChoiceQuizScreenNavArg,
    questionSteps: List<MultiChoiceQuestionStep.Completed>
) {
    val questionFromMaze = args.mazeItemId != null

    if (questionFromMaze) {
        mazeNavigator.navigateToMazeResults(args.mazeItemId?.toIntOrNull() ?: return)
    } else {
        val questionStepsStr = Json.encodeToString(questionSteps)

        navigator.navigate(
            MultiChoiceQuizResultsScreenDestination(
                questionStepsStr = questionStepsStr,
                byInitialQuestions = args.initialQuestions.isNotEmpty(),
                category = args.category,
                difficulty = args.difficulty
            )
        ) {
            launchSingleTop = true

            popUpTo(MultiChoiceQuizScreenDestination) {
                inclusive = true
            }
        }
    }
}

@Composable
@PreviewScreenSizes
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private fun QuizScreenPreview() {
    val questionSteps = listOf(
        MultiChoiceQuestionStep.Completed(
            question = getBasicMultiChoiceQuestion(),
            correct = true
        ),
        MultiChoiceQuestionStep.Completed(
            question = getBasicMultiChoiceQuestion(),
            correct = false
        ),
        MultiChoiceQuestionStep.Current(question = getBasicMultiChoiceQuestion()),
        MultiChoiceQuestionStep.NotCurrent(question = getBasicMultiChoiceQuestion()),
    )

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(screenWidth, screenHeight))

    NewQuizTheme {
        MultiChoiceQuizScreenImpl(
            uiState = MultiChoiceQuizScreenUiState(
                questionSteps = questionSteps,
                selectedAnswer = SelectedAnswer.fromIndex((0..3).random()),
                currentQuestionIndex = 2,
                loading = false,
                remainingTime = RemainingTime(20.seconds)
            ),
            windowSizeClass = windowSizeClass,
            onBackClick = {},
            onEvent = {},
        )
    }
}