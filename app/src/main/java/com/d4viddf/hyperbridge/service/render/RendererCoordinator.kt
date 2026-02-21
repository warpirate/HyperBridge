package com.d4viddf.hyperbridge.service.render

import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.RenderBackend
import com.d4viddf.hyperbridge.models.RendererPreference

class RendererCoordinator(
    private val resolver: RenderBackendResolver,
    private val xiaomiRenderer: XiaomiNativeRenderer,
    private val overlayRenderer: UniversalOverlayRenderer
) {
    private var activeBackend: RenderBackend = RenderBackend.DISABLED

    fun getActiveBackend(): RenderBackend = activeBackend

    fun refreshBackend(preference: RendererPreference): RenderBackend {
        val resolved = resolver.resolve(preference)
        if (resolved == activeBackend) return activeBackend

        when (activeBackend) {
            RenderBackend.XIAOMI_NATIVE -> xiaomiRenderer.clearAll()
            RenderBackend.UNIVERSAL_OVERLAY -> overlayRenderer.clearAll()
            else -> {}
        }

        activeBackend = resolved
        return activeBackend
    }

    fun post(
        preference: RendererPreference,
        sourceKey: String,
        sbn: StatusBarNotification,
        type: NotificationType,
        config: IslandConfig,
        payload: Any? = null
    ): Int? {
        val backend = refreshBackend(preference)
        return when (backend) {
            RenderBackend.XIAOMI_NATIVE -> xiaomiRenderer.post(sourceKey, sbn, type, config, payload)
            RenderBackend.UNIVERSAL_OVERLAY -> overlayRenderer.post(sourceKey, sbn, type, config, payload)
            else -> null
        }
    }

    fun dismiss(sourceKey: String) {
        xiaomiRenderer.dismiss(sourceKey)
        overlayRenderer.dismiss(sourceKey)
    }

    fun dismissByRenderedId(renderedId: Int) {
        xiaomiRenderer.dismissByRenderedId(renderedId)
        overlayRenderer.dismissByRenderedId(renderedId)
    }

    fun clearAll() {
        xiaomiRenderer.clearAll()
        overlayRenderer.clearAll()
    }
}
