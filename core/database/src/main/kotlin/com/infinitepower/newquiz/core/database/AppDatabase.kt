package com.infinitepower.newquiz.core.database

import android.annotation.SuppressLint
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.infinitepower.newquiz.core.database.dao.DailyChallengeDao
import com.infinitepower.newquiz.core.database.dao.GameResultDao
import com.infinitepower.newquiz.core.database.dao.MazeQuizDao
import com.infinitepower.newquiz.core.database.dao.SavedMultiChoiceQuestionsDao
import com.infinitepower.newquiz.core.database.model.DailyChallengeTaskEntity
import com.infinitepower.newquiz.core.database.model.MazeQuizItemEntity
import com.infinitepower.newquiz.core.database.model.MultiChoiceQuestionEntity
import com.infinitepower.newquiz.core.database.model.user.ComparisonQuizGameResultEntity
import com.infinitepower.newquiz.core.database.model.user.MultiChoiceGameResultEntity
import com.infinitepower.newquiz.core.database.model.user.WordleGameResultEntity
import com.infinitepower.newquiz.core.database.util.converters.ListConverter
import com.infinitepower.newquiz.core.database.util.converters.LocalDateConverter
import com.infinitepower.newquiz.core.database.util.converters.MathFormulaConverter
import com.infinitepower.newquiz.core.database.util.converters.QuestionDifficultyConverter

@SuppressLint("all")
@TypeConverters(
    ListConverter::class,
    LocalDateConverter::class,
    QuestionDifficultyConverter::class,
    MathFormulaConverter::class
)
@Database(
    entities = [
        MultiChoiceQuestionEntity::class,
        MazeQuizItemEntity::class,
        DailyChallengeTaskEntity::class,
        MultiChoiceGameResultEntity::class,
        WordleGameResultEntity::class,
        ComparisonQuizGameResultEntity::class
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(
            from = 2,
            to = 3,
            spec = AppDatabase.RemoveDailyWordleTableMigration::class
        ),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(
            from = 5,
            to = 6,
            spec = AppDatabase.RemoveComparisonQuizHighestPositionTableMigration::class
        ),
        AutoMigration(from = 6, to = 7)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedQuestionsDao(): SavedMultiChoiceQuestionsDao

    abstract fun mazeQuizDao(): MazeQuizDao

    abstract fun dailyChallengeDao(): DailyChallengeDao

    abstract fun gameResultDao(): GameResultDao

    companion object {
        internal const val DATABASE_NAME = "app-database"
    }

    @DeleteTable(tableName = "wordle_daily_calendar")
    class RemoveDailyWordleTableMigration : AutoMigrationSpec

    @DeleteColumn(
        tableName = "comparison_quiz_game_results",
        columnName = "highest_position"
    )
    @DeleteTable(tableName = "comparison_quiz_highest_position")
    class RemoveComparisonQuizHighestPositionTableMigration : AutoMigrationSpec
}

