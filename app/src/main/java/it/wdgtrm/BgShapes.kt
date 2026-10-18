package it.wdgtrm

/** Forme dello sfondo del widget (tessera dell'icona). */
object BgShapes {
    const val NONE = "none"
    const val SQUIRCLE = "squircle"
    const val CIRCLE = "circle"
    const val ROUNDED = "rounded"

    /** Chiave, nome mostrato, risorsa (null = nessuno sfondo). */
    val all: List<Triple<String, String, Int?>> = listOf(
        Triple(NONE, "Nessuno (trasparente)", null),
        Triple(SQUIRCLE, "Squircle One UI", R.drawable.bg_squircle),
        Triple(CIRCLE, "Cerchio", R.drawable.bg_circle),
        Triple(ROUNDED, "Quadrato arrotondato", R.drawable.bg_rounded),
    )

    fun res(key: String): Int? = all.firstOrNull { it.first == key }?.third
}
