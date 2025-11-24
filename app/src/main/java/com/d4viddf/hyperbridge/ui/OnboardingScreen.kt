package com.d4viddf.hyperbridge.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.isNotificationServiceEnabled
import com.d4viddf.hyperbridge.isPostNotificationsEnabled
import com.d4viddf.hyperbridge.openAutoStartSettings
import com.d4viddf.hyperbridge.openBatterySettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- STATE TRACKING ---
    var isListenerGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isPostGranted by remember { mutableStateOf(isPostNotificationsEnabled(context)) }

    val postPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> isPostGranted = isGranted }
    )

    // Lifecycle Observer
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isListenerGranted = isNotificationServiceEnabled(context)
                isPostGranted = isPostNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Bottom Navigation Area
            if (pagerState.currentPage > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(), // Respect system gesture bar
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page Indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(4) { iteration ->
                            // Adjust index because Page 0 is welcome screen (hidden dots)
                            val active = (pagerState.currentPage - 1) == iteration
                            val width = if (active) 24.dp else 10.dp
                            val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

                            Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }

                    // Logic to BLOCK navigation
                    val canProceed = when (pagerState.currentPage) {
                        2 -> isPostGranted
                        3 -> isListenerGranted
                        else -> true
                    }

                    val isLastPage = pagerState.currentPage == 4

                    // PillPal Style Button: Floating, Rounded, Iconic
                    Button(
                        onClick = {
                            if (isLastPage) onFinish()
                            else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        enabled = canProceed,
                        shape = RoundedCornerShape(50), // Fully rounded pills
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = if (isLastPage) "Finish" else "Next",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    onStartClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
                1 -> ExplanationPage()
                2 -> PostPermissionPage(
                    isGranted = isPostGranted,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            postPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                3 -> ListenerPermissionPage(context, isListenerGranted)
                4 -> OptimizationPage(context)
            }
        }
    }
}

// --- PAGE 0: WELCOME (Matches PillPal Style) ---
@Composable
fun WelcomePage(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // App Icon Container (Large, Centered, Tonal)
        Surface(
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "HyperBridge Logo",
                    modifier = Modifier.size(80.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "HyperBridge",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Seamlessly integrate Dynamic Islands into your HyperOS experience.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Big "Get Started" Button
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp), // PillPal boxy-round style
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- GENERIC ONBOARDING LAYOUT COMPONENT ---
// This ensures every page (1-4) has the exact same "PillPal" structure
@Composable
fun OnboardingPageLayout(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp), // Wider padding for clean look
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.8f))

        // Icon Bubble
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = iconColor
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Custom Content (Buttons/Cards)
        content()

        Spacer(modifier = Modifier.weight(1f))
    }
}

// --- PAGE 1: EXPLANATION ---
@Composable
fun ExplanationPage() {
    OnboardingPageLayout(
        title = "How it Works",
        description = "Select your favorite apps (Spotify, Maps, Timer). HyperBridge transforms their notifications into a sleek Dynamic Island.",
        icon = Icons.Default.ToggleOn
    ) {
        // Beta Warning Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Construction, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Early Access: Some notifications might look duplicated or experimental.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// --- PAGE 2: POST PERMISSION ---
@Composable
fun PostPermissionPage(isGranted: Boolean, onRequest: () -> Unit) {
    OnboardingPageLayout(
        title = "Show Island",
        description = "We need permission to post the 'Island' notification on your screen.",
        icon = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Notifications
    ) {
        if (isGranted) {
            Text(
                "✅ Permission Granted",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        } else {
            FilledTonalButton(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Allow Notifications")
            }
        }
    }
}

// --- PAGE 3: LISTENER PERMISSION ---
@Composable
fun ListenerPermissionPage(context: Context, isGranted: Boolean) {
    OnboardingPageLayout(
        title = "Read Data",
        description = "To bridge your apps, we need to read their notifications (Music, Maps, Timers).",
        icon = if (isGranted) Icons.Default.CheckCircle else Icons.Default.NotificationsActive
    ) {
        if (isGranted) {
            Text(
                "✅ Permission Granted",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        } else {
            FilledTonalButton(
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Open Settings")
            }
        }
    }
}

// --- PAGE 4: OPTIMIZATION ---
@Composable
fun OptimizationPage(context: Context) {
    OnboardingPageLayout(
        title = "Keep Alive",
        description = "HyperOS kills background apps. Enable Autostart and No Restrictions to keep the island active.",
        icon = Icons.Default.BatteryAlert,
        iconColor = Color(0xFFFF9800)
    ) {
        OutlinedButton(
            onClick = { openAutoStartSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("1. Enable Autostart")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { openBatterySettings(context) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("2. Set Battery 'No Restrictions'")
        }
    }
}