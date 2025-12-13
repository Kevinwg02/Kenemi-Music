package fr.kevw.kenemimusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay



/**
 * Composant FastScroller pour navigation alphabétique rapide
 */
@Composable
fun FastScroller(
    alphabet: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    var scrollerHeight by remember { mutableStateOf(0f) }
    var scrollerTop by remember { mutableStateOf(0f) }

    LaunchedEffect(isDragging) {
        if (!isDragging) {
            delay(1000)
            selectedLetter = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 8.dp)
    ) {
        // Indicateur de lettre au centre
        AnimatedVisibility(
            visible = selectedLetter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            selectedLetter?.let { letter ->
                Box(
                    modifier = Modifier
                        .offset(x = (-80).dp)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Liste des lettres
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
                .onGloballyPositioned { coordinates ->
                    scrollerHeight = coordinates.size.height.toFloat()
                    scrollerTop = coordinates.positionInRoot().y
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            handleTouch(offset.y, scrollerHeight, alphabet) { letter ->
                                selectedLetter = letter
                                onLetterSelected(letter)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            handleTouch(change.position.y, scrollerHeight, alphabet) { letter ->
                                selectedLetter = letter
                                onLetterSelected(letter)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            alphabet.forEach { letter ->
                Text(
                    text = letter,
                    fontSize = 10.sp,
                    fontWeight = if (selectedLetter == letter) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedLetter == letter)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun handleTouch(
    touchY: Float,
    scrollerHeight: Float,
    alphabet: List<String>,
    onLetterSelected: (String) -> Unit
) {
    val index = ((touchY / scrollerHeight) * alphabet.size).toInt()
        .coerceIn(0, alphabet.size - 1)
    onLetterSelected(alphabet[index])
}

/**
 * Extension pour obtenir la première lettre d'une chaîne
 */
fun String.firstLetterOrHash(): String {
    return if (this.isEmpty()) "#"
    else this.first().uppercaseChar().toString()
        .let { if (it[0].isLetter()) it else "#" }
}

/**
 * Fonction pour grouper les items par première lettre
 */
fun <T> List<T>.groupByFirstLetter(selector: (T) -> String): Map<String, List<T>> {
    return this.groupBy { selector(it).firstLetterOrHash() }
        .toSortedMap(compareBy { if (it == "#") "ZZZ" else it })
}

/**
 * Génère l'alphabet pour le FastScroller
 */
fun generateAlphabet(): List<String> {
    return listOf("#") + ('A'..'Z').map { it.toString() }
}