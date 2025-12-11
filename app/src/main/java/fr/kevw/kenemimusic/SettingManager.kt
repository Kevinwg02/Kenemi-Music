@file:OptIn(ExperimentalMaterial3Api::class)
package fr.kevw.kenemimusic
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
}

// ===== ÉCRAN DES PARAMÈTRES =====
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onForceScan: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    var isDarkTheme by remember { mutableStateOf(settingsManager.isDarkTheme) }
    var autoScanOnStart by remember { mutableStateOf(settingsManager.autoScanOnStart) }
    var highQualityImages by remember { mutableStateOf(settingsManager.highQualityImages) }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            // SECTION APPARENCE
            item {
                Text(
                    text = "APPARENCE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
            }

            // Thème sombre/clair
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
                        Text("Redémmarge de l'application nécéssaire")
                        Text(
                            text = if (isDarkTheme) "Activé" else "Désactivé",

                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = {
                            isDarkTheme = it
                            settingsManager.isDarkTheme = it
                        }
                    )
                }
                HorizontalDivider()
            }

            // Qualité des images
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

            // SECTION LECTURE
            item {
                Text(
                    text = "LECTURE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
                )
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

            // Auto-scan au démarrage
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

            // Forcer le scan
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isScanning) {
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Forcer le scan", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isScanning) "Scan en cours..." else "Rechercher de nouvelles musiques",
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

            // Version de l'app
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Version", fontWeight = FontWeight.Medium)
                        Text(
                            text = "11.12.25",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}