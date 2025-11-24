package com.d4viddf.hyperbridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.ui.AppCategory
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.OnboardingScreen
import com.d4viddf.hyperbridge.ui.SortOption
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }
    val isSetupComplete by preferences.isSetupComplete.collectAsState(initial = null)

    when (isSetupComplete) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        false -> OnboardingScreen { scope.launch { preferences.setSetupComplete(true) } }
        true -> HyperBridgeMainScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperBridgeMainScreen(viewModel: AppListViewModel = viewModel()) {
    val context = LocalContext.current
    // 0 = Active, 1 = Library
    var selectedTab by remember { mutableIntStateOf(0) }

    val appList by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) "Active Bridges" else "App Library", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if(selectedTab==0) Icons.Filled.ToggleOn else Icons.Outlined.ToggleOff, null) },
                    label = { Text("Active") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if(selectedTab==1) Icons.Filled.Apps else Icons.Outlined.Apps, null) },
                    label = { Text("Library") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                ActiveAppsScreen(appList, viewModel)
            } else {
                LibraryAppsScreen(appList, viewModel)
            }
        }
    }
}

// --- TAB 1: ACTIVE APPS ---
@Composable
fun ActiveAppsScreen(apps: List<AppInfo>, viewModel: AppListViewModel) {
    val activeApps = apps.filter { it.isBridged }

    if (activeApps.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.NotificationsOff, null, Modifier.size(64.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Active Bridges", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Text("Go to Library to add apps.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(activeApps, key = { it.packageName }) { app ->
                AppListItem(app, isSimple = true) { viewModel.toggleApp(app.packageName, false) }
            }
        }
    }
}

// --- TAB 2: LIBRARY (ALL APPS) ---
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryAppsScreen(apps: List<AppInfo>, viewModel: AppListViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    Column {
        // 1. SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search apps...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // 2. FILTERS ROW
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppCategory.entries.toTypedArray()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category.label) },
                    leadingIcon = if (selectedCategory == category) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. SORT ROW (UPDATED: Only A-Z and Z-A)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AssistChip(
                    onClick = {
                        // Toggle between A-Z and Z-A
                        val nextSort = if (sortOption == SortOption.NAME_AZ) SortOption.NAME_ZA else SortOption.NAME_AZ
                        viewModel.setSort(nextSort)
                    },
                    label = {
                        Text(if (sortOption == SortOption.NAME_AZ) "Sort: A-Z" else "Sort: Z-A")
                    },
                    leadingIcon = {
                        val icon = if (sortOption == SortOption.NAME_AZ) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                        Icon(icon, null, Modifier.size(16.dp))
                    }
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .alpha(0.5f)
        )

        // 4. LIST
        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No apps found", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(apps, key = { it.packageName }) { app ->
                    AppListItem(app, isSimple = false) { viewModel.toggleApp(app.packageName, it) }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun AppListItem(app: AppInfo, isSimple: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!app.isBridged) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (!isSimple) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
        Switch(
            checked = app.isBridged,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF34C759),
                checkedTrackColor = Color(0xFF34C759).copy(alpha = 0.3f)
            )
        )
    }
}

// --- HELPERS ---

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

fun isPostNotificationsEnabled(context: Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
}

fun openAutoStartSettings(context: Context) {
    try {
        val intent = Intent().apply {
            component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        } catch (e2: Exception) {
            Toast.makeText(context, "Settings not found", Toast.LENGTH_SHORT).show()
        }
    }
}

fun openBatterySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        } catch (e2: Exception) {
            Toast.makeText(context, "Settings not found", Toast.LENGTH_SHORT).show()
        }
    }
}