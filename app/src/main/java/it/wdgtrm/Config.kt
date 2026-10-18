package it.wdgtrm

import java.time.LocalDate
import java.time.ZoneId

/** Impostazioni personali: modificale e ricompila. */
object Config {
    // Parma
    const val LATITUDE = 44.8015
    const val LONGITUDE = 10.3279
    val ZONE: ZoneId = ZoneId.of("Europe/Rome")

    // Prossima Granfondo (segnaposto da sostituire)
    const val EVENT_NAME = "Granfondo"
    val EVENT_DATE: LocalDate = LocalDate.of(2027, 6, 20)

    /** App del launcher shopping: prima il pacchetto esatto (se noto), poi il nome mostrato in drawer. */
    class ShopApp(val name: String, val packageHint: String?, val labelMatch: String?, val storeQuery: String)

    val SHOP_APPS = listOf(
        ShopApp("Wallet", "com.google.android.apps.walletnfcrel", null, "Google Wallet"),
        ShopApp("Satispay", null, "satispay", "Satispay"),
        ShopApp("Eurospin", null, "eurospin", "Eurospin"),
        ShopApp("Lidl", null, "lidl", "Lidl Plus"),
    )
}
