package it.wdgtrm

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/** Calcoli astronomici offline, precisione adeguata a un widget. */
object Astro {
    private const val SYNODIC = 29.530588853
    private const val JD_UNIX_EPOCH = 2440587.5
    private const val JD_J2000 = 2451545.0

    data class Moon(
        val ageDays: Double,
        val illumination: Double,
        val name: String,
        val emoji: String,
        val daysToFull: Double,
        val daysToNew: Double,
    )

    data class Sun(val sunrise: ZonedDateTime?, val sunset: ZonedDateTime?)

    private fun julianDay(instant: Instant) = instant.toEpochMilli() / 86_400_000.0 + JD_UNIX_EPOCH

    fun moon(now: Instant): Moon {
        // Luna nuova di riferimento: JD 2451550.1 (6 gennaio 2000)
        val age = ((julianDay(now) - 2451550.1) % SYNODIC + SYNODIC) % SYNODIC
        val illumination = (1 - cos(2 * PI * age / SYNODIC)) / 2
        val half = SYNODIC / 2
        val toFull = if (age < half) half - age else SYNODIC - age + half
        val (name, emoji) = when {
            age < 1.85 -> "Luna nuova" to "🌑"
            age < 5.54 -> "Crescente" to "🌒"
            age < 9.23 -> "Primo quarto" to "🌓"
            age < 12.91 -> "Gibbosa crescente" to "🌔"
            age < 16.61 -> "Luna piena" to "🌕"
            age < 20.30 -> "Gibbosa calante" to "🌖"
            age < 23.99 -> "Ultimo quarto" to "🌗"
            age < 27.68 -> "Calante" to "🌘"
            else -> "Luna nuova" to "🌑"
        }
        return Moon(age, illumination, name, emoji, toFull, SYNODIC - age)
    }

    /** Alba e tramonto (equazione dell'alba, NOAA semplificata). */
    fun sun(date: LocalDate, lat: Double, lon: Double, zone: ZoneId): Sun {
        val jd = date.toEpochDay() + 2440588.0 // mezzogiorno UTC del giorno
        val n = ceil(jd - JD_J2000 + 0.0008)
        val jStar = n - lon / 360.0
        val m = Math.toRadians((357.5291 + 0.98560028 * jStar) % 360)
        val c = 1.9148 * sin(m) + 0.02 * sin(2 * m) + 0.0003 * sin(3 * m)
        val lambda = Math.toRadians((Math.toDegrees(m) + c + 180 + 102.9372) % 360)
        val transit = JD_J2000 + jStar + 0.0053 * sin(m) - 0.0069 * sin(2 * lambda)
        val sinDec = sin(lambda) * sin(Math.toRadians(23.4397))
        val dec = Math.asin(sinDec)
        val phi = Math.toRadians(lat)
        val cosH = (sin(Math.toRadians(-0.833)) - sin(phi) * sinDec) / (cos(phi) * cos(dec))
        if (cosH > 1 || cosH < -1) return Sun(null, null) // sole di mezzanotte / notte polare
        val h = Math.toDegrees(acos(cosH))
        fun at(j: Double) = Instant.ofEpochMilli(((j - JD_UNIX_EPOCH) * 86_400_000.0).toLong()).atZone(zone)
        return Sun(at(transit - h / 360.0), at(transit + h / 360.0))
    }
}
