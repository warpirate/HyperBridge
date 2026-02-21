package com.d4viddf.hyperbridge.models

enum class OverlayPostureKey(val key: String) {
    PHONE_PORTRAIT("phone_portrait"),
    PHONE_LANDSCAPE("phone_landscape"),
    TABLET_PORTRAIT("tablet_portrait"),
    TABLET_LANDSCAPE("tablet_landscape"),
    FOLD_OUTER_PORTRAIT("fold_outer_portrait"),
    FOLD_OUTER_LANDSCAPE("fold_outer_landscape"),
    FOLD_INNER_PORTRAIT("fold_inner_portrait"),
    FOLD_INNER_LANDSCAPE("fold_inner_landscape")
}

data class OverlayProfile(
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val widthDp: Int = 300
)
