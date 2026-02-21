package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.OverlayPostureKey
import com.d4viddf.hyperbridge.models.OverlayProfile
import com.d4viddf.hyperbridge.models.RendererPreference
import com.d4viddf.hyperbridge.ui.components.IslandSettingsControl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    onBack: () -> Unit,
    onNavSettingsClick: () -> Unit // New Callback
    ) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val globalConfig by preferences.globalConfigFlow.collectAsState(initial = IslandConfig(true, true, 5))
    val rendererPreference by preferences.rendererPreferenceFlow.collectAsState(initial = RendererPreference.AUTO)
    val showOnLockscreen by preferences.overlayShowOnLockscreenFlow.collectAsState(initial = true)
    val postureKey = remember(configuration.orientation, configuration.smallestScreenWidthDp) {
        resolveOverlayPosture(configuration.smallestScreenWidthDp, configuration.orientation)
    }
    val overlayProfile by preferences.getOverlayProfileFlow(postureKey).collectAsState(initial = OverlayProfile())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.global_settings)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(16.dp)) {
                    IslandSettingsControl(
                        config = globalConfig,
                        onUpdate = { newConfig ->
                            scope.launch { preferences.updateGlobalConfig(newConfig) }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.renderer_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.renderer_mode_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    RendererOptionRow(
                        label = stringResource(R.string.renderer_pref_auto),
                        selected = rendererPreference == RendererPreference.AUTO,
                        onClick = { scope.launch { preferences.setRendererPreference(RendererPreference.AUTO) } }
                    )
                    RendererOptionRow(
                        label = stringResource(R.string.renderer_pref_native),
                        selected = rendererPreference == RendererPreference.XIAOMI_NATIVE,
                        onClick = { scope.launch { preferences.setRendererPreference(RendererPreference.XIAOMI_NATIVE) } }
                    )
                    RendererOptionRow(
                        label = stringResource(R.string.renderer_pref_overlay),
                        selected = rendererPreference == RendererPreference.UNIVERSAL_OVERLAY,
                        onClick = { scope.launch { preferences.setRendererPreference(RendererPreference.UNIVERSAL_OVERLAY) } }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.lockscreen_overlay_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.lockscreen_overlay_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showOnLockscreen,
                            onCheckedChange = { checked ->
                                scope.launch { preferences.setOverlayShowOnLockscreen(checked) }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.overlay_calibration_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.overlay_calibration_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CalibrationRow(
                        label = stringResource(R.string.overlay_offset_x, overlayProfile.offsetX),
                        onDecrease = {
                            scope.launch {
                                preferences.setOverlayProfile(
                                    postureKey,
                                    overlayProfile.copy(offsetX = overlayProfile.offsetX - 4)
                                )
                            }
                        },
                        onIncrease = {
                            scope.launch {
                                preferences.setOverlayProfile(
                                    postureKey,
                                    overlayProfile.copy(offsetX = overlayProfile.offsetX + 4)
                                )
                            }
                        }
                    )
                    CalibrationRow(
                        label = stringResource(R.string.overlay_offset_y, overlayProfile.offsetY),
                        onDecrease = {
                            scope.launch {
                                preferences.setOverlayProfile(
                                    postureKey,
                                    overlayProfile.copy(offsetY = overlayProfile.offsetY - 4)
                                )
                            }
                        },
                        onIncrease = {
                            scope.launch {
                                preferences.setOverlayProfile(
                                    postureKey,
                                    overlayProfile.copy(offsetY = overlayProfile.offsetY + 4)
                                )
                            }
                        }
                    )
                    CalibrationRow(
                        label = stringResource(R.string.overlay_width, overlayProfile.widthDp),
                        onDecrease = {
                            scope.launch {
                                preferences.setOverlayProfile(
                                    postureKey,
                                    overlayProfile.copy(widthDp = (overlayProfile.widthDp - 10).coerceAtLeast(220))
                                )
                            }
                        },
                        onIncrease = {
                            scope.launch {
                                preferences.setOverlayProfile(
                                    postureKey,
                                    overlayProfile.copy(widthDp = (overlayProfile.widthDp + 10).coerceAtMost(420))
                                )
                            }
                        }
                    )

                    TextButton(
                        onClick = {
                            scope.launch { preferences.setOverlayProfile(postureKey, OverlayProfile()) }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.overlay_reset_profile))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // NEW: Navigation Layout Card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                SettingsItem(
                    icon = Icons.Default.Navigation,
                    title = stringResource(R.string.nav_layout_title),
                    subtitle = stringResource(R.string.nav_layout_desc),
                    onClick = onNavSettingsClick
                )
            }
        }
    }
}

@Composable
private fun CalibrationRow(
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDecrease) { Text("-") }
        TextButton(onClick = onIncrease) { Text("+") }
    }
}

private fun resolveOverlayPosture(
    smallestScreenWidthDp: Int,
    orientation: Int
): OverlayPostureKey {
    val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = smallestScreenWidthDp >= 600
    return if (isTablet) {
        if (isLandscape) OverlayPostureKey.TABLET_LANDSCAPE else OverlayPostureKey.TABLET_PORTRAIT
    } else {
        if (isLandscape) OverlayPostureKey.PHONE_LANDSCAPE else OverlayPostureKey.PHONE_PORTRAIT
    }
}

@Composable
private fun RendererOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Use AutoMirrored icon for RTL support
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}
