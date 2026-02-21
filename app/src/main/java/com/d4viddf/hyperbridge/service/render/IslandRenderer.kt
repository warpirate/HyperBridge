package com.d4viddf.hyperbridge.service.render

import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.RenderBackend

interface IslandRenderer {
    val backend: RenderBackend

    fun post(
        sourceKey: String,
        sbn: StatusBarNotification,
        type: NotificationType,
        config: IslandConfig,
        payload: Any? = null
    ): Int?

    fun dismiss(sourceKey: String)

    fun dismissByRenderedId(renderedId: Int)

    fun clearAll()
}
