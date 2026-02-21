package com.d4viddf.hyperbridge.service.render

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NotificationType

class OverlayContentExtractor(
    private val context: Context
) {
    fun extract(
        sbn: StatusBarNotification,
        type: NotificationType,
        fallbackTitle: String,
        fallbackText: String,
        isLocked: Boolean,
        showOnLockscreen: Boolean
    ): OverlayIslandContent? {
        if (isLocked && !showOnLockscreen) return null

        val appLabel = resolveAppLabel(sbn.packageName)
        val visibility = sbn.notification.visibility
        val redacted = isLocked && visibility != Notification.VISIBILITY_PUBLIC

        val title = if (redacted) appLabel else fallbackTitle.ifBlank { appLabel }
        val text = if (redacted) {
            context.getString(R.string.overlay_private_content)
        } else {
            fallbackText.ifBlank {
                sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            }
        }

        val progress = if (redacted) {
            null
        } else {
            val max = sbn.notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            val current = sbn.notification.extras.getInt(Notification.EXTRA_PROGRESS, -1)
            if (max > 0 && current >= 0) {
                ((current / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
            } else {
                null
            }
        }

        val actions = if (redacted) {
            emptyList()
        } else {
            (sbn.notification.actions ?: emptyArray())
                .take(3)
                .map {
                    OverlayAction(
                        title = it.title?.toString().orEmpty(),
                        intent = it.actionIntent
                    )
                }
                .filter { it.title.isNotBlank() }
        }

        return OverlayIslandContent(
            appLabel = appLabel,
            title = title,
            text = text,
            type = type,
            icon = resolveIconBitmap(sbn),
            actions = actions,
            progressPercent = progress,
            isPrivateRedacted = redacted
        )
    }

    private fun resolveAppLabel(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun resolveIconBitmap(sbn: StatusBarNotification): Bitmap? {
        val extras = sbn.notification.extras

        val largeBitmap = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON_BIG)
            }
        }.getOrNull()
        if (largeBitmap != null) return largeBitmap

        val largeIcon = sbn.notification.getLargeIcon()
        val largeIconBitmap = loadIconBitmap(largeIcon)
        if (largeIconBitmap != null) return largeIconBitmap

        val smallIconBitmap = loadIconBitmap(sbn.notification.smallIcon)
        if (smallIconBitmap != null) return smallIconBitmap

        return runCatching {
            val drawable = context.packageManager.getApplicationIcon(sbn.packageName)
            drawableToBitmap(drawable)
        }.getOrNull()
    }

    private fun loadIconBitmap(icon: Icon?): Bitmap? {
        if (icon == null) return null
        return runCatching {
            val drawable = icon.loadDrawable(context) ?: return null
            drawableToBitmap(drawable)
        }.getOrNull()
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
