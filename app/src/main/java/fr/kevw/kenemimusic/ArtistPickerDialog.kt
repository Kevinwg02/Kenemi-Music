package fr.kevw.kenemimusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.NumberFormat

/**
 * Propose les meilleurs candidats Deezer pour un artiste.
 * Un champ de recherche permet de retaper un autre nom si aucun candidat ne convient.
 * onPicked reçoit l'URL "file://..." de la nouvelle image.
 */
@Composable
fun ArtistPickerDialog(
    artistName: String,
    service: ArtistImageService,
    onDismiss: () -> Unit,
    onPicked: (String?) -> Unit
) {
    var query by remember { mutableStateOf(artistName) }
    var result by remember { mutableStateOf<ArtistSearchResult?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search(q: String) {
        result = null
        message = null
        scope.launch {
            result = try {
                service.searchCandidates(q, limit = 10)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ArtistSearchResult(emptyList(), listOf("Erreur : ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    LaunchedEffect(artistName) { search(artistName) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Image de « $artistName »") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Rechercher un artiste") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search(query) }),
                    trailingIcon = {
                        IconButton(onClick = { search(query) }) {
                            Icon(Icons.Default.Search, contentDescription = "Rechercher")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                val r = result
                when {
                    r == null -> Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    r.candidates.isEmpty() -> Column {
                        Text("Aucun résultat. Essaie une autre orthographe ci-dessus.")
                        r.errors.forEach {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    else -> LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        itemsIndexed(r.candidates) { _, c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !saving) {
                                        saving = true
                                        message = null
                                        scope.launch {
                                            val url = service.selectArtistImage(artistName, c)
                                            saving = false
                                            if (url != null) {
                                                onPicked(url)
                                                onDismiss()
                                            } else {
                                                message = "Téléchargement impossible, réessaie."
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = c.thumbUrl,
                                    contentDescription = c.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        if (c.fans > 0)
                                            "${c.source} · ${NumberFormat.getInstance().format(c.fans)} fans"
                                        else c.source,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        message = null
                        scope.launch {
                            val url = service.redetectArtistImage(artistName)
                            saving = false
                            if (url != null) {
                                onPicked(url)
                                onDismiss()
                            } else {
                                message = "Détection automatique : aucun résultat fiable."
                            }
                        }
                    }
                ) { Text("Auto") }
                TextButton(enabled = !saving, onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}
