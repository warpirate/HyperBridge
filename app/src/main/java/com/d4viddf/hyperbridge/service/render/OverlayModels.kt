package com.d4viddf.hyperbridge.service.render

import android.app.PendingIntent
import android.graphics.Bitmap
import com.d4viddf.hyperbridge.models.NotificationType

data class OverlayAction(
    val title: String,
    val intent: PendingIntent?
)

data class OverlayIslandContent(
    val appLabel: String,
    val title: String,
    val text: String,
    val type: NotificationType,
    val icon: Bitmap?,
    val actions: List<OverlayAction> = emptyList(),
    val progressPercent: Int? = null,
    val isPrivateRedacted: Boolean = false
) {
    val contentHash: Int
        get() = listOf(
            appLabel,
            title,
            text,
            type.name,
            progressPercent?.toString() ?: "",
            isPrivateRedacted.toString(),
            actions.joinToString("|") { it.title }
        ).joinToString("::").hashCode()
}
