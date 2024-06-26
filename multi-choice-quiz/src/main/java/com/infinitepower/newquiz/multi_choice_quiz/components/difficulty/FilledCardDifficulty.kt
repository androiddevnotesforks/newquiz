package com.infinitepower.newquiz.multi_choice_quiz.components.difficulty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.infinitepower.newquiz.core.theme.CustomColor
import com.infinitepower.newquiz.core.theme.NewQuizTheme
import com.infinitepower.newquiz.core.theme.extendedColors
import com.infinitepower.newquiz.core.util.asString
import com.infinitepower.newquiz.core.util.model.getText
import com.infinitepower.newquiz.model.question.QuestionDifficulty

@Composable
internal fun FilledCardDifficulty(
    modifier: Modifier = Modifier,
    multiChoiceQuizDifficulty: QuestionDifficulty,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colorRoles = MaterialTheme.extendedColors.getColorsByKey(
        key = when (multiChoiceQuizDifficulty) {
            is QuestionDifficulty.Easy -> CustomColor.Key.Green
            is QuestionDifficulty.Medium -> CustomColor.Key.Yellow
            is QuestionDifficulty.Hard -> CustomColor.Key.Red
        }
    )

    FilledCardDifficulty(
        modifier = modifier,
        text = multiChoiceQuizDifficulty.getText().asString(),
        containerColor = colorRoles.color,
        contentColor = colorRoles.onColor,
        onClick = onClick,
        enabled = enabled
    )
}

@Composable
internal fun FilledCardDifficulty(
    modifier: Modifier = Modifier,
    text: String,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilledCardDifficultyContainer(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        onClick = onClick,
        enabled = enabled
    ) {
        BaseCardDifficultyContent(text = text)
    }
}

@Composable
private fun FilledCardDifficultyContainer(
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        enabled = enabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.large
    ) {
        content()
    }
}

@Composable
@PreviewLightDark
private fun CardDifficultyPreview() {
    NewQuizTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledCardDifficulty(
                    multiChoiceQuizDifficulty = QuestionDifficulty.Easy,
                    onClick = {}
                )
                FilledCardDifficulty(
                    multiChoiceQuizDifficulty = QuestionDifficulty.Medium,
                    onClick = {}
                )
                FilledCardDifficulty(
                    multiChoiceQuizDifficulty = QuestionDifficulty.Hard,
                    onClick = {}
                )
            }
        }
    }
}