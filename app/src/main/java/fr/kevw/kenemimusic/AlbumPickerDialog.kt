package fr.kevw.kenemimusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * Propose les meilleures pochettes (Deezer + iTunes) pour un album.
 * Un champ de recherche permet de retaper le titre si aucune ne convient.
 * onPicked reçoit l'URL "file://..." de la nouvelle pochette.
 */
@Composable
fun AlbumPickerDialog(
    albumName: String,
    artistName: String,
    service: ArtistImageService,
    onDismiss: () -> Unit,
    onPicked: (String?) -> Unit
) {
    val initialQuery = "$albumName $artistName".trim()
    var query by remember { mutableStateOf(initialQuery) }
    var result by remember { mutableStateOf<AlbumSearchResult?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search(q: String, useHints: Boolean) {
        result = null
        message = null
        scope.launch {
            result = try {
                if (useHints)
                    service.searchAlbumCandidates(q, limit = 10, album = albumName, artist = artistName)
                else
                    service.searchAlbumCandidates(q, limit = 10)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AlbumSearchResult(emptyList(), listOf("Erreur : ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    LaunchedEffect(albumName, artistName) { search(initialQuery, useHints = true) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Pochette de « $albumName »", maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Rechercher un album") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search(query, false) }),
                    trailingIcon = {
                        IconButton(onClick = { search(query, false) }) {
                            Icon(Icons.Default.Search, contentDescription = "Rechercher")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        saving = true
                        message = null
                        scope.launch {
                            val url = service.useArtistImageForAlbum(albumName, artistName)
                            saving = false
                            if (url != null) {
                                onPicked(url)
                                onDismiss()
                            } else {
                                message = "Aucune image d'artiste trouvée pour « $artistName »."
                            }
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Utiliser l'image de l'artiste") }
                Spacer(Modifier.height(8.dp))

                val r = result
                when {
                    r == null -> Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    r.candidates.isEmpty() -> Column {
                        Text("Aucun résultat. Essaie un autre titre ci-dessus.")
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
                                            val url = service.selectAlbumCover(albumName, artistName, c)
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
                                    contentDescription = c.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        c.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        listOf(c.artist, c.source).filter { it.isNotBlank() }
                                            .joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                            val url = service.redetectAlbumCover(albumName, artistName)
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
