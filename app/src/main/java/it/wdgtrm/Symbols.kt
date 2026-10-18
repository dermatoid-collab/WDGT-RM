package it.wdgtrm

/** Set di icone minimal (vettoriali, monocromatiche) tra cui scegliere per il widget. */
object Symbols {
    const val DEFAULT = "grid"

    val all: List<Pair<String, Int>> = listOf(
        "dot" to R.drawable.ic_w_dot,
        "ring" to R.drawable.ic_w_ring,
        "target" to R.drawable.ic_w_target,
        "square" to R.drawable.ic_w_square,
        "grid" to R.drawable.ic_w_grid,
        "plus" to R.drawable.ic_w_plus,
        "triangle" to R.drawable.ic_w_triangle,
        "diamond" to R.drawable.ic_w_diamond,
        "star" to R.drawable.ic_w_star,
        "bolt" to R.drawable.ic_w_bolt,
        "heart" to R.drawable.ic_w_heart,
        "home" to R.drawable.ic_w_home,
        "menu" to R.drawable.ic_w_menu,
        "bag" to R.drawable.ic_w_bag,
    )

    fun res(key: String): Int = all.firstOrNull { it.first == key }?.second
        ?: all.first { it.first == DEFAULT }.second
}
