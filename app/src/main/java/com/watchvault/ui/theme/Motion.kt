package com.watchvault.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * The one motion scale for the whole app. Kept short and restrained on purpose — this is a
 * quiet, editorial product, not a showcase of animation; every transition should read as
 * "the interface responded" rather than "look at this animation."
 */
object Motion {
    /** Micro-feedback: press states, capsule selection toggle. */
    const val quick = 100

    /** Local content change: card entering/leaving a list, a value updating. */
    const val standard = 220

    /** Screen-level or large-surface transitions: navigation, sheet enter/exit, gallery paging. */
    const val emphasized = 340

    val standardEasing: Easing = FastOutSlowInEasing

    /** A slightly more decisive curve for full-screen and sheet transitions. */
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
