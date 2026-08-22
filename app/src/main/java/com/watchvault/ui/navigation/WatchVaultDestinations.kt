package com.watchvault.ui.navigation

object Routes {
    const val HOME = "home"
    const val COLLECTION = "collection"
    const val WISHLIST = "wishlist"
    const val SETTINGS = "settings"
    const val IMPORT_EXPORT = "import_export"
    const val WATCH_DETAIL = "watch_detail/{watchUuid}"
    const val ADD_EDIT_WATCH = "add_edit_watch?watchUuid={watchUuid}"
    const val ADD_EDIT_WISH = "add_edit_wish?wishUuid={wishUuid}"

    fun watchDetail(uuid: String) = "watch_detail/$uuid"
    fun addEditWatch(uuid: String? = null) = if (uuid == null) "add_edit_watch" else "add_edit_watch?watchUuid=$uuid"
    fun addEditWish(uuid: String? = null) = if (uuid == null) "add_edit_wish" else "add_edit_wish?wishUuid=$uuid"
}
