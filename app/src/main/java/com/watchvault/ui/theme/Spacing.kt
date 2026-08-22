package com.watchvault.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The one spacing scale for the whole app. Every screen touched in the design-system rebuild
 * pass uses these named steps instead of ad-hoc `.dp` literals, so gaps between related content,
 * section breaks and screen edges read the same everywhere.
 *
 * Rough usage guide:
 *  - [xs]/[sm] for spacing between tightly related elements (an icon and its label, a value and
 *    its caption).
 *  - [sm2]/[md] for spacing between rows inside one logical block (a list of labeled facts).
 *  - [screenH] for a screen's default horizontal padding.
 *  - [lg]/[xl] for spacing between unrelated sections on the same screen.
 *  - [xxl]/[xxxl] for the largest separations (e.g. above a hero image block, around empty
 *    states).
 */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val sm2 = 20.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp

    /** Default screen horizontal padding. */
    val screenH = sm2
}

/** Corner radii used across cards/images/sheets. Pill shapes ([androidx.compose.foundation.shape.CircleShape]
 *  or a very large [androidx.compose.foundation.shape.RoundedCornerShape]) are reserved for small
 *  status/priority tags only — not for cards, buttons or nav bars. */
object Radius {
    val thumbnail = 8.dp
    val card = 12.dp
    val sheet = 20.dp
}
