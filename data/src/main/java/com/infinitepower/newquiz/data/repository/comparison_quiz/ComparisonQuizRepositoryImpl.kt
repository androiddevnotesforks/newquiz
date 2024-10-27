package com.infinitepower.newquiz.data.repository.comparison_quiz

import com.infinitepower.newquiz.core.NumberFormatter
import com.infinitepower.newquiz.core.NumberFormatter.Distance.DistanceUnitType
import com.infinitepower.newquiz.core.NumberFormatter.Temperature.TemperatureUnit
import com.infinitepower.newquiz.core.database.dao.GameResultDao
import com.infinitepower.newquiz.core.datastore.common.SettingsCommon
import com.infinitepower.newquiz.core.datastore.di.SettingsDataStoreManager
import com.infinitepower.newquiz.core.datastore.manager.DataStoreManager
import com.infinitepower.newquiz.core.remote_config.RemoteConfig
import com.infinitepower.newquiz.core.remote_config.RemoteConfigValue
import com.infinitepower.newquiz.core.remote_config.get
import com.infinitepower.newquiz.domain.repository.comparison_quiz.ComparisonQuizRepository
import com.infinitepower.newquiz.model.comparison_quiz.ComparisonQuizCategory
import com.infinitepower.newquiz.model.comparison_quiz.ComparisonQuizCategoryEntity
import com.infinitepower.newquiz.model.comparison_quiz.ComparisonQuizItem
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ComparisonQuizRepositoryImpl @Inject constructor(
    private val remoteConfig: RemoteConfig,
    private val gameResultDao: GameResultDao,
    @SettingsDataStoreManager private val settingsDataStoreManager: DataStoreManager,
    private val comparisonQuizApi: ComparisonQuizApi
) : ComparisonQuizRepository {
    private val categoriesCache: MutableList<ComparisonQuizCategory> = mutableListOf()

    override fun getCategories(): List<ComparisonQuizCategory> {
        if (categoriesCache.isEmpty()) {
            val categoriesStr = remoteConfig.get(RemoteConfigValue.COMPARISON_QUIZ_CATEGORIES)
            val categoriesEntity: List<ComparisonQuizCategoryEntity> = Json.decodeFromString(categoriesStr)
            val categories = categoriesEntity.map(ComparisonQuizCategoryEntity::toModel)

            categoriesCache.addAll(categories)
        }

        return categoriesCache
    }

    override suspend fun getQuestions(
        category: ComparisonQuizCategory,
        size: Int,
        random: Random
    ): List<ComparisonQuizItem> {
        val entityQuestions = comparisonQuizApi.generateQuestions(
            category = category,
            size = size,
            random = random
        )

        val userConfig = getUserConfig()

        return entityQuestions.map { entity ->
            val valueFormatter = NumberFormatter.from(category.formatType)

            val helperValue = valueFormatter.formatValueToString(
                value = entity.value,
                helperValueSuffix = category.helperValueSuffix,
                regionalPreferences = userConfig
            )

            ComparisonQuizItem(
                title = entity.title,
                value = entity.value,
                helperValue = helperValue,
                imgUri = URI.create(entity.imgUrl)
            )
        }
    }

    private suspend fun getUserConfig(): NumberFormatter.RegionalPreferences {
        val temperatureUnitStr = settingsDataStoreManager.getPreference(SettingsCommon.TemperatureUnit)
        val temperatureUnit = if (temperatureUnitStr.isBlank()) {
            null // use default
        } else {
            TemperatureUnit.valueOf(temperatureUnitStr)
        }

        val distanceUnitTypeStr = settingsDataStoreManager.getPreference(SettingsCommon.DistanceUnitType)
        val distanceUnitType = if (distanceUnitTypeStr.isBlank()) {
            null // use default
        } else {
            DistanceUnitType.valueOf(distanceUnitTypeStr)
        }

        return NumberFormatter.RegionalPreferences(
            temperatureUnit = temperatureUnit,
            distanceUnitType = distanceUnitType
        )
    }

    override suspend fun getHighestPosition(categoryId: String): Int {
        return gameResultDao.getComparisonQuizHighestPosition(categoryId)
    }

    override fun getHighestPositionFlow(categoryId: String): Flow<Int> {
        return gameResultDao.getComparisonQuizHighestPositionFlow(categoryId)
    }
}
