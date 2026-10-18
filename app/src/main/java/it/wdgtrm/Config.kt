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
}
