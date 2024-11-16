package com.infinitepower.newquiz.data.daily_challenge

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.infinitepower.newquiz.core.database.dao.DailyChallengeDao
import com.infinitepower.newquiz.data.repository.daily_challenge.util.getTitle
import com.infinitepower.newquiz.data.util.mappers.comparisonquiz.toEntity
import com.infinitepower.newquiz.domain.repository.comparison_quiz.ComparisonQuizRepository
import com.infinitepower.newquiz.domain.repository.daily_challenge.DailyChallengeRepository
import com.infinitepower.newquiz.model.daily_challenge.DailyChallengeTask
import com.infinitepower.newquiz.model.global_event.GameEvent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DailyChallengeRepositoryImplTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context

    @Inject lateinit var dailyChallengeDao: DailyChallengeDao

    @Inject lateinit var dailyChallengeRepository: DailyChallengeRepository

    @Inject lateinit var comparisonQuizRepository: ComparisonQuizRepository

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context

        hiltRule.inject()
    }

    @Test
    fun test_getDailyChallengeTasks_returnsCorrectTasks() = runTest {
        val now = Clock.System.now()

        val type1 = GameEvent.MultiChoice.PlayQuestions

        val initialTasks = listOf(
            DailyChallengeTask(
                id = 1,
                diamondsReward = 1u,
                experienceReward = 1u,
                isClaimed = false,
                dateRange = (now - 1.days).rangeTo(now + 1.days), // Not expired
                currentValue = 0u, // Not completed
                maxValue = 1u,
                event = type1,
                title = type1.getTitle(1, comparisonQuizRepository.getCategories())
            )
        )

        dailyChallengeDao.insertAll(initialTasks.map(DailyChallengeTask::toEntity))

        val tasks = dailyChallengeRepository.getAvailableTasks()

        assertThat(tasks).containsExactlyElementsIn(initialTasks)
    }

    @Test
    fun test_checkAndGenerateTasks_shouldGenerateNewTasks() = runTest {
        val now = Clock.System.now()

        val type1 = GameEvent.MultiChoice.PlayQuestions
        val task1 = DailyChallengeTask(
            id = 1,
            diamondsReward = 1u,
            experienceReward = 1u,
            isClaimed = false,
            dateRange = (now - 2.days).rangeTo(now - 1.days), // Expired
            currentValue = 0u, // Not completed
            maxValue = 1u,
            event = type1,
            title = type1.getTitle(1, comparisonQuizRepository.getCategories())
        )

        val initialTasks = setOf(task1)

        dailyChallengeDao.insertAll(initialTasks.map(DailyChallengeTask::toEntity))

        dailyChallengeRepository.checkAndGenerateTasksIfNeeded(tasksToGenerate = 2)

        val tasks = dailyChallengeRepository.getAllTasks().toSet()

        assertThat(tasks).isNotEmpty()
        assertThat(tasks).hasSize(3)

        val newTasks = tasks - initialTasks

        assertThat(newTasks).isNotEmpty()
        assertThat(newTasks).hasSize(2)

        val nowVerify = Clock.System.now()

        // Check that the new tasks are not expired
        newTasks.forEach { task ->
            assertThat(nowVerify).isAtLeast(task.dateRange.start)
            assertThat(nowVerify).isAtMost(task.dateRange.endInclusive)

            println(task)
        }
    }

    @Test
    fun test_checkAndGenerateTasks_shouldGenerateNewTasks_whenInitialTasksAreEmpty() = runTest {
        val initialTasks = emptySet<DailyChallengeTask>()

        dailyChallengeRepository.checkAndGenerateTasksIfNeeded(tasksToGenerate = 2)

        val tasks = dailyChallengeRepository.getAvailableTasks()

        assertThat(tasks).isNotEmpty()
        assertThat(tasks).hasSize(2)

        val newTasks = tasks - initialTasks

        assertThat(newTasks).isNotEmpty()
        assertThat(newTasks).hasSize(2)

        val now = Clock.System.now()

        // Check that the new tasks are not expired
        newTasks.forEach { task ->
            assertThat(task.dateRange.contains(now)).isTrue()
            assertThat(task.isActive()).isTrue()
            assertThat(task.isCompleted()).isFalse()
            assertThat(task.isExpired()).isFalse()
            assertThat(task.isClaimed).isFalse()
        }
    }

    @Test
    fun test_completeTaskStep_shouldCompleteTheTask_ifTheCurrentValueIsEqualToTheMaxValue() = runTest {
        val now = Clock.System.now()

        val initialTasks = setOf(
            DailyChallengeTask(
                id = 1,
                diamondsReward = 1u,
                experienceReward = 1u,
                isClaimed = false,
                dateRange = (now - 1.days).rangeTo(now + 1.days), // Not expired
                currentValue = 0u, // Not completed
                maxValue = 1u,
                event = GameEvent.MultiChoice.PlayQuestions,
                title = GameEvent.MultiChoice.PlayQuestions.getTitle(1, comparisonQuizRepository.getCategories())
            ),
            DailyChallengeTask(
                id = 2,
                diamondsReward = 1u,
                experienceReward = 1u,
                isClaimed = false,
                dateRange = (now - 1.days).rangeTo(now + 1.days), // Not expired
                currentValue = 0u, // Not completed
                maxValue = 1u,
                event = GameEvent.MultiChoice.GetAnswersCorrect,
                title = GameEvent.MultiChoice.GetAnswersCorrect.getTitle(1, comparisonQuizRepository.getCategories())
            )
        )

        dailyChallengeDao.insertAll(initialTasks.map(DailyChallengeTask::toEntity))

        // Check that the initial tasks are not completed
        assertThat(dailyChallengeRepository.getAvailableTasks()).containsExactlyElementsIn(initialTasks)

        // Complete the task
        dailyChallengeRepository.completeTaskStep(GameEvent.MultiChoice.PlayQuestions)

        val tasks = dailyChallengeRepository.getAvailableTasks()

        assertThat(tasks).isNotEmpty()
        assertThat(tasks).hasSize(2)

        val completedTask = tasks.find { it.event == GameEvent.MultiChoice.PlayQuestions }

        // Check that the task is not null
        assertThat(completedTask).isNotNull()
        if (completedTask == null) throw AssertionError("Completed task is null")

        assertThat(initialTasks).doesNotContain(completedTask)
        assertThat(completedTask.currentValue).isEqualTo(completedTask.maxValue)
        assertThat(completedTask.isCompleted()).isTrue()

        // Check that the others task are not changed
        val otherTasks = tasks - completedTask
        assertThat(otherTasks).containsExactlyElementsIn(initialTasks.filter { it.id != completedTask.id })
    }

    @Test
    fun completeTaskStep_shouldThrowException_ifTaskIsExpired() {
        val now = Clock.System.now()

        val task1 = DailyChallengeTask(
            id = 1,
            diamondsReward = 1u,
            experienceReward = 1u,
            isClaimed = false,
            dateRange = (now - 2.days).rangeTo(now - 1.days), // Expired
            currentValue = 0u, // Not completed
            maxValue = 1u,
            event = GameEvent.MultiChoice.PlayQuestions,
            title = GameEvent.MultiChoice.PlayQuestions.getTitle(1, comparisonQuizRepository.getCategories())
        )

        runTest { dailyChallengeDao.insertAll(task1.toEntity()) }

        val e = assertThrows(IllegalStateException::class.java) {
            runTest {
                dailyChallengeRepository.completeTaskStep(GameEvent.MultiChoice.PlayQuestions)
            }
        }

        assertThat(e)
            .hasMessageThat()
            .isEqualTo("Task (${task1.title}) is expired.")
    }

    @Test
    fun completeTaskStep_shouldThrowException_ifTaskIsClaimed() {
        val now = Clock.System.now()

        val task1 = DailyChallengeTask(
            id = 1,
            diamondsReward = 1u,
            experienceReward = 1u,
            isClaimed = true,
            dateRange = (now - 1.days).rangeTo(now + 1.days), // Not expired
            currentValue = 0u, // Not completed
            maxValue = 1u,
            event = GameEvent.MultiChoice.PlayQuestions,
            title = GameEvent.MultiChoice.PlayQuestions.getTitle(1, comparisonQuizRepository.getCategories())
        )

        runTest { dailyChallengeDao.insertAll(task1.toEntity()) }

        val e = assertThrows(IllegalStateException::class.java) {
            runTest {
                dailyChallengeRepository.completeTaskStep(GameEvent.MultiChoice.PlayQuestions)
            }
        }

        assertThat(e)
            .hasMessageThat()
            .isEqualTo("Task (${task1.title}) is already claimed.")
    }

    @Test
    fun completeTaskStep_shouldThrowException_ifTaskIsNotFound() {
        val e = assertThrows(NullPointerException::class.java) {
            runTest {
                dailyChallengeRepository.completeTaskStep(GameEvent.MultiChoice.GetAnswersCorrect)
            }
        }

        assertThat(e)
            .hasMessageThat()
            .isEqualTo("Task not found.")
    }
}