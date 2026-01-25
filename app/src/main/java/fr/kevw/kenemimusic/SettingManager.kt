@file:OptIn(ExperimentalMaterial3Api::class)
package fr.kevw.kenemimusic

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.Manifest

// ===== CLASSE POUR GÉRER LES PARAMÈTRES =====
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("music_settings", Context.MODE_PRIVATE)

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var autoScanOnStart: Boolean
        get() = prefs.getBoolean("auto_scan", true)
        set(value) = prefs.edit().putBoolean("auto_scan", value).apply()

    var highQualityImages: Boolean
        get() = prefs.getBoolean("high_quality_images", true)
        set(value) = prefs.edit().putBoolean("high_quality_images", value).apply()

    var musicFolderName: String
        get() = prefs.getString("music_folder_name", "Music") ?: "Music"
        set(value) = prefs.edit().putString("music_folder_name", value).apply()

    // ✅ NOUVEAU : Sauvegarder/charger la dernière chanson jouée
    var lastPlayedSongId: Long
        get() = prefs.getLong("last_played_song_id", -1L)
        set(value) {
            prefs.edit().putLong("last_played_song_id", value).commit() // Utiliser commit() pour sauvegarde immédiate
        }

    // ✅ NOUVEAU : Sauvegarder/charger la position de lecture de la dernière chanson
    var lastPlayedPosition: Long
        get() = prefs.getLong("last_played_position", 0L)
        set(value) {
            prefs.edit().putLong("last_played_position", value).commit() // Utiliser commit() pour sauvegarde immédiate
        }

    // ✅ NOUVEAU : Sauvegarder la chanson et la position en même temps
    fun saveLastPlayedState(songId: Long, position: Long) {
        prefs.edit()
            .putLong("last_played_song_id", songId)
            .putLong("last_played_position", position)
            .commit() // Utiliser commit() pour sauvegarde immédiate et atomique
    }

    // ✅ NOUVEAU : Effacer l'état de dernière lecture
    fun clearLastPlayedState() {
        prefs.edit()
            .remove("last_played_song_id")
            .remove("last_played_position")
            .commit()
    }
}

// ===== ÉCRAN DES PARAMÈTRES =====
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onForceScan: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val imageService = remember { ArtistImageService(context) }
    var isDarkTheme by remember { mutableStateOf(settingsManager.isDarkTheme) }
    var autoScanOnStart by remember { mutableStateOf(settingsManager.autoScanOnStart) }
    var highQualityImages by remember { mutableStateOf(settingsManager.highQualityImages) }
    var musicFolderName by remember { mutableStateOf(settingsManager.musicFolderName) }
    var isScanning by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(0) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        cacheSize = imageService.getCacheSize()
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                getAudioPermissionString()
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            hasAudioPermission = ContextCompat.checkSelfPermission(
                context,
                getAudioPermissionString()
            ) == PackageManager.PERMISSION_GRANTED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasNotificationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ===== SECTION PERMISSIONS =====
            item {
                Text(
                    text = "PERMISSIONS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!hasAudioPermission) {
                                onRequestPermission()
                            } else {
                                openAppSettings(context)
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (hasAudioPermission)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accès aux fichiers audio", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (hasAudioPermission)
                                "Accordée - Requis pour lire vos musiques"
                            else
                                "Non accordée - Appuyez pour autoriser",
                            fontSize = 14.sp,
                            color = if (hasAudioPermission)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = if (hasAudioPermission)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasAudioPermission)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                HorizontalDivider()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                openAppSettings(context)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (hasNotificationPermission)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notifications", fontWeight = FontWeight.Medium)
                            Text(
                                text = if (hasNotificationPermission)
                                    "Accordée - Contrôles de lecture"
                                else
                                    "Non accordée - Optionnelle",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (hasNotificationPermission)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (hasNotificationPermission)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }

            // SECTION APPARENCE
            item {
                Text(
                    text = "APPARENCE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isDarkTheme = !isDarkTheme
                            settingsManager.isDarkTheme = isDarkTheme
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thème sombre", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Changement instantané du thème",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = {
                            isDarkTheme = it
                            settingsManager.isDarkTheme = it
                            onThemeChanged(it)
                        }
                    )
                }
                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            highQualityImages = !highQualityImages
                            settingsManager.highQualityImages = highQualityImages
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Images haute qualité", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Photos d'artistes en haute résolution",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = highQualityImages,
                        onCheckedChange = {
                            highQualityImages = it
                            settingsManager.highQualityImages = it
                        }
                    )
                }
                HorizontalDivider()
            }

            // SECTION BIBLIOTHÈQUE
            item {
                Text(
                    text = "BIBLIOTHÈQUE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            autoScanOnStart = !autoScanOnStart
                            settingsManager.autoScanOnStart = autoScanOnStart
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scan automatique", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Scanner les musiques au démarrage",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoScanOnStart,
                        onCheckedChange = {
                            autoScanOnStart = it
                            settingsManager.autoScanOnStart = it
                        }
                    )
                }
                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFolderDialog = true
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dossier de musique", fontWeight = FontWeight.Medium)
                        Text(
                            text = musicFolderName,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isScanning && hasAudioPermission) {
                            isScanning = true
                            scope.launch {
                                onForceScan()
                                delay(1000)
                                isScanning = false
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (hasAudioPermission)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Forcer le scan",
                            fontWeight = FontWeight.Medium,
                            color = if (hasAudioPermission)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isScanning) "Scan en cours..."
                            else if (hasAudioPermission) "Rechercher de nouvelles musiques"
                            else "Permission audio requise",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showClearCacheDialog = true
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vider le cache des images", fontWeight = FontWeight.Medium)
                        Text(
                            text = "$cacheSize image${if (cacheSize > 1) "s" else ""} en cache",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }

            // SECTION À PROPOS
            item {
                Text(
                    text = "À PROPOS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Version by Kevinwg02", fontWeight = FontWeight.Medium)
                        Text(
                            text = "26.01.25",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showFolderDialog) {
        var tempFolderName by remember { mutableStateOf(musicFolderName) }
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Dossier de musique") },
            text = {
                Column {
                    Text("Entrez le nom du dossier contenant votre musique:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempFolderName,
                        onValueChange = { tempFolderName = it },
                        label = { Text("Nom du dossier") },
                        placeholder = { Text("Music") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Exemples: Music, Audio, Téléchargements, etc.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempFolderName.isNotBlank()) {
                            musicFolderName = tempFolderName.trim()
                            settingsManager.musicFolderName = musicFolderName
                            onForceScan()
                        }
                        showFolderDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Vider le cache ?") },
            text = {
                Text("Cela supprimera toutes les images en cache ($cacheSize image${if (cacheSize > 1) "s" else ""}). Elles seront retéléchargées lors de leur prochaine utilisation.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            imageService.clearCache()
                            cacheSize = 0
                            showClearCacheDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Vider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

private fun getAudioPermissionString(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}