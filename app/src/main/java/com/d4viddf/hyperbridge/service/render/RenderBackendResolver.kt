package com.d4viddf.hyperbridge.service.render

import android.content.Context
import com.d4viddf.hyperbridge.models.RenderBackend
import com.d4viddf.hyperbridge.models.RendererPreference
import com.d4viddf.hyperbridge.util.isOverlayPermissionGranted
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification

class RenderBackendResolver(
    private val context: Context
) {
    fun resolve(preference: RendererPreference): RenderBackend {
        val nativeSupported = isXiaomiNativeSupported()
        val overlayAvailable = isOverlayAvailable()

        return when (preference) {
            RendererPreference.XIAOMI_NATIVE -> {
                if (nativeSupported) RenderBackend.XIAOMI_NATIVE
                else if (overlayAvailable) RenderBackend.UNIVERSAL_OVERLAY
                else RenderBackend.DISABLED
            }

            RendererPreference.UNIVERSAL_OVERLAY -> {
                if (overlayAvailable) RenderBackend.UNIVERSAL_OVERLAY
                else if (nativeSupported) RenderBackend.XIAOMI_NATIVE
                else RenderBackend.DISABLED
            }

            RendererPreference.AUTO -> {
                when {
                    nativeSupported -> RenderBackend.XIAOMI_NATIVE
                    overlayAvailable -> RenderBackend.UNIVERSAL_OVERLAY
                    else -> RenderBackend.DISABLED
                }
            }
        }
    }

    fun isXiaomiNativeSupported(): Boolean {
        return runCatching { HyperIslandNotification.isSupported(context) }.getOrDefault(false)
    }

    fun isOverlayAvailable(): Boolean = isOverlayPermissionGranted(context)
}
