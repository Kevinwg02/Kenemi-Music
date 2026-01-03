package fr.kevw.kenemimusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Composant FastScroller optimisé pour navigation alphabétique ultra-rapide
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
    val haptic = LocalHapticFeedback.current
    var lastSelectedIndex by remember { mutableStateOf(-1) }

    // Auto-masquage après inactivité
    LaunchedEffect(isDragging, selectedLetter) {
        if (!isDragging && selectedLetter != null) {
            delay(1500)
            selectedLetter = null
            lastSelectedIndex = -1
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .padding(end = 4.dp)
    ) {
        // Bulle d'indication de lettre (plus grande et plus visible)
        AnimatedVisibility(
            visible = selectedLetter != null,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            selectedLetter?.let { letter ->
                Box(
                    modifier = Modifier
                        .offset(x = (-70).dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Barre alphabétique
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .width(32.dp)
                .align(Alignment.CenterEnd)
                .onGloballyPositioned { coordinates ->
                    scrollerHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(alphabet) {
                    // Gestion du tap direct
                    detectTapGestures { offset ->
                        val index = calculateLetterIndex(offset.y, scrollerHeight, alphabet.size)
                        if (index in alphabet.indices) {
                            selectedLetter = alphabet[index]
                            onLetterSelected(alphabet[index])
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
                .pointerInput(alphabet) {
                    // Gestion du drag
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val index = calculateLetterIndex(offset.y, scrollerHeight, alphabet.size)
                            if (index in alphabet.indices && index != lastSelectedIndex) {
                                lastSelectedIndex = index
                                selectedLetter = alphabet[index]
                                onLetterSelected(alphabet[index])
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val index = calculateLetterIndex(
                                change.position.y,
                                scrollerHeight,
                                alphabet.size
                            )
                            // Ne déclencher que si on change de lettre (évite les appels multiples)
                            if (index in alphabet.indices && index != lastSelectedIndex) {
                                lastSelectedIndex = index
                                selectedLetter = alphabet[index]
                                onLetterSelected(alphabet[index])
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    fontSize = if (selectedLetter == letter) 12.sp else 11.sp,
                    fontWeight = if (selectedLetter == letter) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedLetter == letter)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Calcule l'index de la lettre en fonction de la position du toucher
 * Optimisé pour éviter les calculs répétés
 */
private fun calculateLetterIndex(
    touchY: Float,
    scrollerHeight: Float,
    alphabetSize: Int
): Int {
    if (scrollerHeight <= 0f) return 0

    val normalizedPosition = (touchY / scrollerHeight).coerceIn(0f, 1f)
    return (normalizedPosition * alphabetSize).toInt().coerceIn(0, alphabetSize - 1)
}

/**
 * Extension pour obtenir la première lettre d'une chaîne
 */
fun String.firstLetterOrHash(): String {
    if (this.isEmpty()) return "#"
    val firstChar = this.first().uppercaseChar()
    return if (firstChar.isLetter()) firstChar.toString() else "#"
}

/**
 * Fonction pour grouper les items par première lettre
 * Optimisée avec LinkedHashMap pour préserver l'ordre
 */
fun <T> List<T>.groupByFirstLetter(selector: (T) -> String): Map<String, List<T>> {
    return this.groupBy { selector(it).firstLetterOrHash() }
        .toSortedMap(compareBy { if (it == "#") "" else it })
}

/**
 * Génère l'alphabet pour le FastScroller
 */
fun generateAlphabet(): List<String> {
    return listOf("#") + ('A'..'Z').map { it.toString() }
}