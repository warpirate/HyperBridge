package com.d4viddf.hyperbridge.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.hyperbridge.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- DATA MODELS ---

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Bitmap,
    val isBridged: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)

enum class AppCategory(val label: String) {
    ALL("All"),
    MUSIC("Music"),
    MAPS("Navigation"),
    TIMER("Productivity"),
    OTHER("Other")
}

// Removed Enabled/Disabled options
enum class SortOption {
    NAME_AZ, NAME_ZA
}

// --- VIEW MODEL ---

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val packageManager = application.packageManager
    private val preferences = AppPreferences(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    // Filters
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow(AppCategory.ALL)
    val sortOption = MutableStateFlow(SortOption.NAME_AZ) // Default to A-Z

    // Helpers for Categories
    private val MUSIC_KEYS = listOf("music", "spotify", "youtube", "deezer", "tidal", "sound", "audio", "podcast")
    private val MAPS_KEYS = listOf("map", "nav", "waze", "gps", "transit", "uber", "cabify")
    private val TIMER_KEYS = listOf("clock", "timer", "alarm", "stopwatch", "calendar", "todo")

    val uiState: StateFlow<List<AppInfo>> = combine(
        _installedApps,
        preferences.allowedPackagesFlow,
        searchQuery,
        selectedCategory,
        sortOption
    ) { apps, allowedSet, query, category, sort ->

        // 1. Map bridged status
        var result = apps.map { app ->
            app.copy(isBridged = allowedSet.contains(app.packageName))
        }

        // 2. Filter by Search
        if (query.isNotEmpty()) {
            result = result.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }

        // 3. Filter by Category
        if (category != AppCategory.ALL) {
            result = result.filter { it.category == category }
        }

        // 4. Apply Sorting (Simplified)
        result = when (sort) {
            SortOption.NAME_AZ -> result.sortedBy { it.name }
            SortOption.NAME_ZA -> result.sortedByDescending { it.name }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = getLaunchableApps()
        }
    }

    fun toggleApp(packageName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            preferences.toggleApp(packageName, isEnabled)
        }
    }

    // ACTIONS
    fun setCategory(cat: AppCategory) { selectedCategory.value = cat }
    fun setSort(option: SortOption) { sortOption.value = option }
    fun clearSearch() { searchQuery.value = "" }

    private suspend fun getLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        resolveInfos.mapNotNull { resolveInfo ->
            try {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == getApplication<Application>().packageName) return@mapNotNull null

                val name = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager).toBitmap()

                val cat = when {
                    MUSIC_KEYS.any { pkg.contains(it, true) } -> AppCategory.MUSIC
                    MAPS_KEYS.any { pkg.contains(it, true) } -> AppCategory.MAPS
                    TIMER_KEYS.any { pkg.contains(it, true) } -> AppCategory.TIMER
                    else -> AppCategory.OTHER
                }

                AppInfo(name, pkg, icon, category = cat)
            } catch (e: Exception) { null }
        }.distinctBy { it.packageName }.sortedBy { it.name }
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) return this.bitmap
        val width = if (intrinsicWidth > 0) intrinsicWidth else 1
        val height = if (intrinsicHeight > 0) intrinsicHeight else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}