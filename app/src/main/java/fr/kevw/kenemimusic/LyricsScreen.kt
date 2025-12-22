package fr.kevw.kenemimusic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface LyricsUiState {
    object Loading : LyricsUiState
    data class Success(val lyrics: String, val source: String) : LyricsUiState
    object NotFound : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    song: Song,
    lyricsService: LyricsService,
    imageService: ArtistImageService,
    onBack: () -> Unit
) {
    var uiState by remember { mutableStateOf<LyricsUiState>(LyricsUiState.Loading) }
    var showManualSearch by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    BackHandler { onBack() }

    fun loadLyrics() {
        scope.launch {
            uiState = LyricsUiState.Loading
            when (val result = lyricsService.getLyrics(song.title, song.artist)) {
                is LyricsResult.Success -> {
                    uiState = LyricsUiState.Success(result.lyrics, result.source)
                }
                is LyricsResult.NotFound -> {
                    uiState = LyricsUiState.NotFound
                }
                is LyricsResult.Error -> {
                    uiState = LyricsUiState.Error(result.message)
                }
            }
        }
    }

    LaunchedEffect(song.id) {
        loadLyrics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paroles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Bouton de recherche manuelle
                    IconButton(onClick = { showManualSearch = true }) {
                        Icon(Icons.Default.Search, "Rechercher manuellement")
                    }

                    // Bouton d'édition (si paroles trouvées)
                    if (uiState is LyricsUiState.Success) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, "Modifier")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "lyrics_content"
            ) { state ->
                when (state) {
                    LyricsUiState.Loading -> LoadingState()

                    is LyricsUiState.Success -> SuccessState(
                        lyrics = state.lyrics,
                        source = state.source,
                        song = song,
                        scrollState = scrollState,
                        imageService = imageService
                    )

                    LyricsUiState.NotFound -> NotFoundState(
                        onRetry = ::loadLyrics,
                        onManualSearch = { showManualSearch = true }
                    )

                    is LyricsUiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = ::loadLyrics
                    )
                }
            }
        }
    }

    // Dialog de recherche manuelle
    if (showManualSearch) {
        ManualSearchDialog(
            song = song,
            lyricsService = lyricsService,
            onDismiss = { showManualSearch = false },
            onLyricsSelected = { lyrics ->
                lyricsService.saveManualLyrics(song.title, song.artist, lyrics)
                uiState = LyricsUiState.Success(lyrics, "Manuel")
                showManualSearch = false
            }
        )
    }

    // Dialog d'édition
    if (showEditDialog && uiState is LyricsUiState.Success) {
        EditLyricsDialog(
            currentLyrics = (uiState as LyricsUiState.Success).lyrics,
            onDismiss = { showEditDialog = false },
            onSave = { newLyrics ->
                lyricsService.saveManualLyrics(song.title, song.artist, newLyrics)
                uiState = LyricsUiState.Success(newLyrics, "Manuel")
                showEditDialog = false
            }
        )
    }
}

// ===== LOADING STATE =====
@Composable
private fun LoadingState() {
    var dotsCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotsCount = (dotsCount + 1) % 4
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing)
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 4.dp
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Recherche dans 4 sources${".".repeat(dotsCount)}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Lyrics.ovh • ChartLyrics • Lyrist • 5 variantes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ===== NOT FOUND STATE =====
@Composable
private fun NotFoundState(
    onRetry: () -> Unit,
    onManualSearch: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Paroles introuvables",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Nous avons cherché dans 4 sources différentes avec 5 variantes mais nous n'avons pas trouvé les paroles de cette chanson.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Bouton de recherche manuelle (principal)
        Button(
            onClick = onManualSearch,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Rechercher manuellement", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(12.dp))

        // Bouton réessayer (secondaire)
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Réessayer")
        }
    }
}

// ===== ERROR STATE =====
@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(24.dp))

        Text("Erreur", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Button(onClick = onRetry) {
            Text("Réessayer")
        }
    }
}

// ===== SUCCESS STATE =====
@Composable
private fun SuccessState(
    lyrics: String,
    source: String,
    song: Song,
    scrollState: androidx.compose.foundation.ScrollState,
    imageService: ArtistImageService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var artistImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }

    LaunchedEffect(song.artist) {
        scope.launch {
            artistImageUrl = imageService.getArtistImageUrl(song.artist)
            isLoadingImage = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // En-tête
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artistImageUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(artistImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (isLoadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Paroles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = lyrics,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.6f
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Attribution
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "Source: $source",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.8f)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ===== DIALOG: RECHERCHE MANUELLE =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSearchDialog(
    song: Song,
    lyricsService: LyricsService,
    onDismiss: () -> Unit,
    onLyricsSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("${song.artist} ${song.title}") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<SearchResult?>(null) }
    var manualLyrics by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun doSearch() {
        scope.launch {
            isSearching = true
            searchResults = lyricsService.searchLyricsManual(searchQuery)
            isSearching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "Recherche manuelle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                // Champ de recherche
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Rechercher") },
                    placeholder = { Text("Artiste - Titre") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { doSearch() }, enabled = !isSearching) {
                            Icon(Icons.Default.Search, "Rechercher")
                        }
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (showManualInput) {
                    // Saisie manuelle
                    Text(
                        "Entrez les paroles manuellement :",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualLyrics,
                        onValueChange = { manualLyrics = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        placeholder = { Text("Collez ou tapez les paroles ici...") },
                        maxLines = 15
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showManualInput = false }) {
                            Text("Annuler")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualLyrics.isNotBlank()) {
                                    onLyricsSelected(manualLyrics)
                                }
                            },
                            enabled = manualLyrics.isNotBlank()
                        ) {
                            Text("Enregistrer")
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    // Message par défaut
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Recherchez une chanson ou entrez les paroles manuellement",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Résultats
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(searchResults) { result ->
                            ListItem(
                                headlineContent = { Text(result.title) },
                                supportingContent = { Text(result.artist) },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        val lyrics = lyricsService.getLyrics(result.title, result.artist)
                                        if (lyrics is LyricsResult.Success) {
                                            onLyricsSelected(lyrics.lyrics)
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Boutons du bas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fermer")
                    }

                    if (!showManualInput && searchResults.isEmpty()) {
                        Button(onClick = { showManualInput = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Saisie manuelle")
                        }
                    }
                }
            }
        }
    }
}

// ===== DIALOG: ÉDITION =====
@Composable
fun EditLyricsDialog(
    currentLyrics: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var editedLyrics by remember { mutableStateOf(currentLyrics) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier les paroles") },
        text = {
            OutlinedTextField(
                value = editedLyrics,
                onValueChange = { editedLyrics = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                maxLines = 20
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(editedLyrics) },
                enabled = editedLyrics.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}