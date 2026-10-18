# WDGT-RM

Suite di widget nativi (Kotlin + Jetpack Glance) per Samsung Galaxy / One UI. Colori Material You dinamici, tutto calcolato offline.

| Widget | Contenuto |
|---|---|
| Luna | Fase, illuminazione, giorni alla luna piena |
| Sole | Alba, tramonto, ore di luce, prossimo evento (Parma) |
| Countdown | Giorni alla prossima Granfondo |
| Shopping | Launcher a griglia per Wallet, Satispay, Eurospin, Lidl (icone reali delle app installate) |

## Personalizzazione
Modifica `app/src/main/java/it/wdgtrm/Config.kt` (località, nome e data evento).

## Build
- Locale: Android Studio → Run, oppure `gradle :app:assembleDebug`.
- CI: ogni push genera l'APK come artifact di GitHub Actions (`wdgt-rm-debug-apk`).

## Installazione
Scarica l'APK sul telefono, installalo, poi Home → tieni premuto → Widget → WDGT-RM.
