# WDGT-RM

Suite di widget nativi (Kotlin + Jetpack Glance) per Samsung Galaxy / One UI. Colori Material You dinamici, tutto calcolato offline.

| Widget | Contenuto |
|---|---|
| Launcher (1×1) | Il tocco apre un menu con le app del tuo elenco (o lancia un'app fissa); ogni istanza ha nome e icona propri |
| Luna | Fase, illuminazione, giorni alla luna piena |
| Sole | Alba, tramonto, ore di luce, prossimo evento (Parma) |
| Countdown | Giorni alla prossima Granfondo |

## Launcher
1. Apri l'app **WDGT-RM**: definisci l'elenco delle app e riordinalo (▲ ▼ ✕).
2. Aggiungi il widget *Launcher* alla Home (sfondo trasparente, stile minimal bianco/nero): si aprono le impostazioni, dove scegli nome, icona (set di 14 simboli minimal, icona dell'app, emoji o immagine), colore bianco/nero e se il widget apre il **menu** o una **app fissa**.
3. Un tocco sul widget apre il menu: colonna di sole icone su sfondo opaco, la più stretta possibile, sopra il widget (sotto se non c'è spazio). Tocca un'icona per lanciare l'app, tocca fuori per chiudere.
4. Per modificarlo in seguito: tieni premuto sul widget → *Impostazioni* (il tocco prolungato è gestito dalla schermata Home di Android e non può aprire il menu).

## Scorciatoie
Le prime app dell'elenco (fino al limite di Android, di solito 4-5) compaiono come scorciatoie tenendo premuta l'icona dell'app WDGT-RM.

## Personalizzazione
`app/src/main/java/it/wdgtrm/Config.kt` (località, nome e data evento per Sole e Countdown).

## Build
- Locale: Android Studio → Run, oppure `gradle :app:assembleDebug`.
- CI: ogni push genera l'APK come artifact di GitHub Actions (`wdgt-rm-debug-apk`).
