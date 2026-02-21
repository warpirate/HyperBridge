package com.d4viddf.hyperbridge.service.render

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.OverlayPostureKey
import com.d4viddf.hyperbridge.models.OverlayProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class OverlayAnchorManager(
    private val context: Context,
    private val preferences: AppPreferences
) {
    fun resolveCurrentPosture(): OverlayPostureKey {
        val conf = context.resources.configuration
        val isLandscape = conf.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isTablet = conf.smallestScreenWidthDp >= 600

        return if (isTablet) {
            if (isLandscape) OverlayPostureKey.TABLET_LANDSCAPE else OverlayPostureKey.TABLET_PORTRAIT
        } else {
            if (isLandscape) OverlayPostureKey.PHONE_LANDSCAPE else OverlayPostureKey.PHONE_PORTRAIT
        }
    }

    fun getCurrentProfile(): OverlayProfile {
        val posture = resolveCurrentPosture()
        return runBlocking(Dispatchers.IO) {
            preferences.getOverlayProfile(posture)
        }
    }

    fun buildLayoutParams(): WindowManager.LayoutParams {
        val profile = getCurrentProfile()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            dpToPx(profile.widthDp),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.x = profile.offsetX
        params.y = getTopInsetPx() + dpToPx(10) + profile.offsetY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return params
    }

    private fun getTopInsetPx(): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
