package it.wdgtrm

/**
 * Set di icone minimal (vettoriali, monocromatiche) tra cui scegliere per il widget.
 * Le icone «pieno» sono Material Icons di Google (Apache License 2.0); le forme
 * geometriche in fondo sono disegnate a mano.
 */
object Symbols {
    const val DEFAULT = "grid"

    val all: List<Pair<String, Int>> = listOf(
        "cart" to R.drawable.ic_w_cart,
        "bag" to R.drawable.ic_w_bag,
        "wallet" to R.drawable.ic_w_wallet,
        "card" to R.drawable.ic_w_card,
        "tag" to R.drawable.ic_w_tag,
        "gift" to R.drawable.ic_w_gift,
        "euro" to R.drawable.ic_w_euro,
        "grid" to R.drawable.ic_w_grid,
        "rocket" to R.drawable.ic_w_rocket,
        "gear" to R.drawable.ic_w_gear,
        "bolt" to R.drawable.ic_w_bolt,
        "phone" to R.drawable.ic_w_phone,
        "laptop" to R.drawable.ic_w_laptop,
        "wifi" to R.drawable.ic_w_wifi,
        "bulb" to R.drawable.ic_w_bulb,
        "bike" to R.drawable.ic_w_bike,
        "mountain" to R.drawable.ic_w_mountain,
        "sun" to R.drawable.ic_w_sun,
        "moon" to R.drawable.ic_w_moon,
        "cloud" to R.drawable.ic_w_cloud,
        "drop" to R.drawable.ic_w_drop,
        "flame" to R.drawable.ic_w_flame,
        "fitness" to R.drawable.ic_w_fitness,
        "food" to R.drawable.ic_w_food,
        "cafe" to R.drawable.ic_w_cafe,
        "home" to R.drawable.ic_w_home,
        "work" to R.drawable.ic_w_work,
        "key" to R.drawable.ic_w_key,
        "lock" to R.drawable.ic_w_lock,
        "calendar" to R.drawable.ic_w_calendar,
        "clock" to R.drawable.ic_w_clock,
        "bell" to R.drawable.ic_w_bell,
        "mail" to R.drawable.ic_w_mail,
        "music" to R.drawable.ic_w_music,
        "book" to R.drawable.ic_w_book,
        "camera" to R.drawable.ic_w_camera,
        "game" to R.drawable.ic_w_game,
        "heart" to R.drawable.ic_w_heart,
        "star" to R.drawable.ic_w_star,
        "compass" to R.drawable.ic_w_compass,
        "pin" to R.drawable.ic_w_pin,
        "globe" to R.drawable.ic_w_globe,
        "chat" to R.drawable.ic_w_chat,
        "menu" to R.drawable.ic_w_menu,
        "plus" to R.drawable.ic_w_plus,
        "dot" to R.drawable.ic_w_dot,
        "ring" to R.drawable.ic_w_ring,
        "target" to R.drawable.ic_w_target,
        "square" to R.drawable.ic_w_square,
        "triangle" to R.drawable.ic_w_triangle,
        "diamond" to R.drawable.ic_w_diamond,
    )

    fun res(key: String): Int = all.firstOrNull { it.first == key }?.second
        ?: all.first { it.first == DEFAULT }.second
}
