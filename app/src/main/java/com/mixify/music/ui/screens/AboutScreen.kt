package com.mixify.music.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mixify.music.BuildConfig
import com.mixify.music.R
import com.mixify.music.data.AppUpdateChecker
import com.mixify.music.data.settings.AppSettings
import com.mixify.music.ui.components.UpdateAvailableDialog
import com.mixify.music.ui.components.optimizedHazeEffect
import com.mixify.music.ui.icons.SfIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AboutScreen(
    hazeState: HazeState,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val updateInfo by AppUpdateChecker.available.collectAsStateWithLifecycle()
    val checkState by AppUpdateChecker.manualCheckState.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf(false) }

    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminance() < 0.5f
    val material = HazeMaterials.thin(surface)
    val style = remember(material, dark) {
        HazeStyle(
            backgroundColor = material.backgroundColor,
            tints = material.tints + dev.chrisbanes.haze.HazeTint(
                if (dark) Color.Black.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.40f),
            ),
            blurRadius = material.blurRadius,
            noiseFactor = material.noiseFactor,
            fallbackTint = material.fallbackTint,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 48.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                AboutAppCard(style = style, dark = dark, reduceDynamicBlur = reduceDynamicBlur, hazeState = hazeState)
            }
            item {
                AboutDeveloperCard(style = style, dark = dark, reduceDynamicBlur = reduceDynamicBlur, hazeState = hazeState, onOpenUrl = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                })
            }
            item {
                AboutCommunitySection(
                    style = style,
                    dark = dark,
                    reduceDynamicBlur = reduceDynamicBlur,
                    hazeState = hazeState,
                    updateInfo = updateInfo,
                    checkState = checkState,
                    onOpenUrl = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onCheckUpdates = {
                        if (updateInfo != null) {
                            showUpdateDialog = true
                        } else {
                            scope.launch { AppUpdateChecker.check(manual = true) }
                        }
                    }
                )
            }
        }

        if (showUpdateDialog && updateInfo != null) {
            updateInfo?.let { update ->
                UpdateAvailableDialog(
                    version = update.version,
                    notes = update.notes,
                    hazeState = hazeState,
                    onDismiss = { showUpdateDialog = false },
                    onDownload = {
                        if (update.apkUrl != null) {
                            scope.launch { AppUpdateChecker.downloadApk(context) }
                        } else {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                            showUpdateDialog = false
                        }
                    },
                    onCancelDownload = { AppUpdateChecker.cancelDownload() },
                    onInstall = {
                        val ready = AppUpdateChecker.download.value as? AppUpdateChecker.DownloadState.Ready
                        ready?.let { AppUpdateChecker.installApk(context, it.file) }
                    },
                    onOpenReleasePage = {
                        AppUpdateChecker.resetDownload()
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                        showUpdateDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AboutAppCard(style: HazeStyle, dark: Boolean, reduceDynamicBlur: Boolean, hazeState: HazeState) {
    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .then(if (reduceDynamicBlur) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier.optimizedHazeEffect(hazeState, style))
            .border(
                0.5.dp,
                if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f),
                MaterialTheme.shapes.extraLarge,
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Text(
                text = "Mixify",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            // Show build type only if available/meaningful (not just "Universal" for every APK)
            if (BuildConfig.BUILD_TYPE.isNotEmpty() && BuildConfig.BUILD_TYPE != "release") {
                Text(
                    text = BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutDeveloperCard(style: HazeStyle, dark: Boolean, reduceDynamicBlur: Boolean, hazeState: HazeState, onOpenUrl: (String) -> Unit) {
    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .then(if (reduceDynamicBlur) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier.optimizedHazeEffect(hazeState, style))
            .border(
                0.5.dp,
                if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f),
                MaterialTheme.shapes.extraLarge,
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            AsyncImage(
                model = "https://github.com/scisims12.png", 
                contentDescription = "Priyanshu Sharma",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Text(
                text = "Priyanshu Sharma",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Developer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(modifier = Modifier.padding(top = 16.dp)) {
                IconButton(onClick = { onOpenUrl("https://portfolio.mixify.co.in/") }) {
                    Icon(SfIcons.Language, contentDescription = "Developer portfolio")
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { onOpenUrl("https://github.com/scisims12") }) {
                    Icon(painterResource(R.drawable.ic_github), contentDescription = "Developer GitHub")
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { onOpenUrl("https://www.instagram.com/sharmaajikabadabeta/") }) {
                    Icon(painterResource(R.drawable.ic_instagram), contentDescription = "Developer Instagram")
                }
            }
        }
    }
}

@Composable
private fun AboutCommunitySection(
    style: HazeStyle,
    dark: Boolean,
    reduceDynamicBlur: Boolean,
    hazeState: HazeState,
    updateInfo: AppUpdateChecker.UpdateInfo?,
    checkState: AppUpdateChecker.CheckState,
    onOpenUrl: (String) -> Unit,
    onCheckUpdates: () -> Unit,
) {
    AboutSettingsGroup(
        header = "Community & Info",
        footer = "Based on BitChord by kushagrasinghx · GPL-3.0",
        style = style,
        dark = dark,
        reduceDynamicBlur = reduceDynamicBlur,
        hazeState = hazeState
    ) {
        val updateSubtitle = when {
            checkState is AppUpdateChecker.CheckState.Checking -> "Checking for updates…"
            updateInfo != null -> "Version ${updateInfo.version} available (Tap to update)"
            checkState is AppUpdateChecker.CheckState.UpToDate -> "Mixify is up to date (v${BuildConfig.VERSION_NAME})"
            checkState is AppUpdateChecker.CheckState.Error -> "${checkState.message} (Tap to retry)"
            else -> "Tap to check for updates"
        }

        AboutSettingsRow(
            icon = SfIcons.Cached,
            title = "Check for updates",
            subtitle = updateSubtitle,
            onClick = onCheckUpdates
        )
        AboutRowDivider()
        AboutSettingsRow(
            icon = SfIcons.Language,
            title = "Mixify Website",
            onClick = { onOpenUrl("https://mixify.co.in/") }
        )
        AboutRowDivider()
        AboutSettingsRow(
            icon = SfIcons.Share,
            title = "Telegram channel",
            onClick = { onOpenUrl("https://t.me/mixify20") }
        )
        AboutRowDivider()
        AboutSettingsRow(
            iconPainter = painterResource(R.drawable.ic_github),
            title = "View repository",
            onClick = { onOpenUrl("https://github.com/MixifyTeam/Mixify") }
        )
        AboutRowDivider()
        AboutSettingsRow(
            icon = SfIcons.Notes,
            title = "Terms & Conditions",
            onClick = { onOpenUrl("https://mixify.co.in/terms") }
        )
        AboutRowDivider()
        AboutSettingsRow(
            icon = SfIcons.Lock,
            title = "Privacy Policy",
            onClick = { onOpenUrl("https://mixify.co.in/privacy") }
        )
        AboutRowDivider()
        AboutSettingsRow(
            icon = SfIcons.Info,
            title = "Open-source licences",
            onClick = { /* TODO if any */ }
        )
    }
}

@Composable
private fun AboutSettingsGroup(
    header: String,
    footer: String,
    style: HazeStyle,
    dark: Boolean,
    reduceDynamicBlur: Boolean,
    hazeState: HazeState,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .then(if (reduceDynamicBlur) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier.optimizedHazeEffect(hazeState, style))
                .border(
                    0.5.dp,
                    if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f),
                    MaterialTheme.shapes.extraLarge,
                )
        ) {
            Column {
                content()
            }
        }
        Text(
            text = footer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )
    }
}

@Composable
private fun AboutSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .size(0.5.dp)
    )
}
