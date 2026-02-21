package com.d4viddf.hyperbridge.service.render

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.RenderBackend

class XiaomiNativeRenderer(
    private val serviceContext: android.content.Context,
    private val channelId: String,
    private val extraOriginalKey: String
) : IslandRenderer {

    override val backend: RenderBackend = RenderBackend.XIAOMI_NATIVE
    private val sourceToRendered = linkedMapOf<String, Int>()

    override fun post(
        sourceKey: String,
        sbn: StatusBarNotification,
        type: NotificationType,
        config: IslandConfig,
        payload: Any?
    ): Int? {
        val data = payload as? HyperIslandData ?: return null
        val renderedId = sourceToRendered[sourceKey] ?: sourceKey.hashCode()

        val builder = NotificationCompat.Builder(serviceContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(serviceContext.getString(R.string.app_name))
            .setContentText("Active Island")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        val extras = Bundle().apply { putString(extraOriginalKey, sourceKey) }
        builder.addExtras(extras)
        builder.addExtras(data.resources)
        sbn.notification.contentIntent?.let { builder.setContentIntent(it) }

        val notification: Notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)

        NotificationManagerCompat.from(serviceContext).notify(renderedId, notification)
        sourceToRendered[sourceKey] = renderedId
        return renderedId
    }

    override fun dismiss(sourceKey: String) {
        val renderedId = sourceToRendered.remove(sourceKey) ?: return
        NotificationManagerCompat.from(serviceContext).cancel(renderedId)
    }

    override fun dismissByRenderedId(renderedId: Int) {
        NotificationManagerCompat.from(serviceContext).cancel(renderedId)
        val key = sourceToRendered.entries.firstOrNull { it.value == renderedId }?.key ?: return
        sourceToRendered.remove(key)
    }

    override fun clearAll() {
        val manager = NotificationManagerCompat.from(serviceContext)
        sourceToRendered.values.forEach { manager.cancel(it) }
        sourceToRendered.clear()
    }
}
