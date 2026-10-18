# WDGT-RM

Suite di widget nativi (Kotlin + Jetpack Glance) per Samsung Galaxy / One UI. Colori Material You dinamici, tutto calcolato offline.

| Widget | Contenuto |
|---|---|
| Launcher (1×1) | Lancia un'app a scelta; ogni istanza ha app, nome e icona propri |
| Luna | Fase, illuminazione, giorni alla luna piena |
| Sole | Alba, tramonto, ore di luce, prossimo evento (Parma) |
| Countdown | Giorni alla prossima Granfondo |

## Launcher
1. Apri l'app **WDGT-RM**: definisci l'elenco delle app e riordinalo (▲ ▼ ✕).
2. Aggiungi il widget *Launcher* alla Home: si aprono le impostazioni, dove scegli app, nome e icona (icona dell'app, emoji o immagine).
3. Per modificarlo in seguito: tieni premuto sul widget → *Impostazioni*.

## Personalizzazione
`app/src/main/java/it/wdgtrm/Config.kt` (località, nome e data evento per Sole e Countdown).

## Build
- Locale: Android Studio → Run, oppure `gradle :app:assembleDebug`.
- CI: ogni push genera l'APK come artifact di GitHub Actions (`wdgt-rm-debug-apk`).
