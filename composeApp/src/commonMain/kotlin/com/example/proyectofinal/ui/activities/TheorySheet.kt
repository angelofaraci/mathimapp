package com.example.proyectofinal.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.TheorySection
import com.example.proyectofinal.models.TheorySectionType
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.theory_sheet_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheorySheet(
    lesson: Lesson,
    sections: List<TheorySection> = emptyList(),
    isLoading: Boolean = false,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        TheorySheetContent(lesson, sections, isLoading)
    }
}

@Composable
internal fun TheorySheetContent(
    lesson: Lesson,
    sections: List<TheorySection> = emptyList(),
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.theory_sheet_label),
                modifier = Modifier.testTag("theorySheetChapter"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = lesson.title,
                modifier = Modifier.testTag("theorySheetTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isLoading) {
                Text("Cargando teoría…", modifier = Modifier.testTag("theorySheetLoading"))
            } else {
                val content = sections.ifEmpty {
                    listOf(
                        TheorySection(
                            id = "legacy-${lesson.id}",
                            type = TheorySectionType.CONCEPT,
                            title = "Concepto clave",
                            content = lesson.theoryContent,
                            position = 0
                        )
                    )
                }
                content.sortedBy { it.position }.forEach { section ->
                    TheorySection(
                        title = section.title ?: section.type.displayTitle(),
                        tag = "theorySheetSection-${section.id}",
                        content = section.content
                    )
                }
            }
    }
}

@Composable
private fun TheorySection(title: String, tag: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        MCard(modifier = Modifier.fillMaxWidth().testTag(tag)) {
            Text(
                text = content,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun TheorySectionType.displayTitle(): String = when (this) {
    TheorySectionType.CONCEPT -> "Concepto clave"
    TheorySectionType.EXPLANATION -> "Explicación"
    TheorySectionType.STEPS -> "Cómo resolverlo"
    TheorySectionType.EXAMPLE -> "Ejemplo práctico"
    TheorySectionType.WARNING -> "Tené en cuenta"
}
