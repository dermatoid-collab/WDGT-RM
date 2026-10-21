# WDGT-RM

Suite di widget nativi (Kotlin + Jetpack Glance) per Samsung Galaxy / One UI. Colori Material You dinamici, tutto calcolato offline.

| Widget | Contenuto |
|---|---|
| Launcher (1×1, personalizzabile) | Il tocco apre un menu con le app del tuo elenco (o lancia un'app fissa); ogni istanza ha nome e icona propri |
| Luna | Fase, illuminazione, giorni alla luna piena |
| Sole | Alba, tramonto, ore di luce, prossimo evento (Parma) |
| Countdown | Giorni alla prossima Granfondo |

## Launcher
1. Apri l'app **WDGT-RM**: definisci l'elenco generale delle app e riordinalo (▲ ▼ ✕). I nuovi widget partono con una copia di questo elenco.
2. Aggiungi il widget *Launcher* alla Home (sfondo trasparente, stile minimal bianco/nero): si aprono le impostazioni, dove scegli l'**elenco di app del menu di quel widget** (indipendente dagli altri), nome, icona (set di oltre 150 simboli minimal raggruppati per tema (shopping, tecnologia, sport, cielo e scienza, cibo, casa e lavoro, musica, viaggi…), icona dell'app, emoji o immagine), colore e dimensione dell'icona, sfondo del widget (forma One UI «squircle», cerchio, quadrato arrotondato, colore e opacità a scelta), colore e opacità del menu, e se il widget apre il **menu** o una **app fissa**. Un'anteprima in tempo reale mostra il risultato.
3. Un tocco sul widget apre il menu: colonna di sole icone su sfondo opaco, la più stretta possibile, sopra il widget (sotto se non c'è spazio). Tocca un'icona per lanciare l'app, tocca fuori per chiudere.
4. One UI non comunica alle app dove si trova il widget: per aprire il menu proprio sopra di esso, dopo il salvataggio compare uno strato scuro con un riquadro grande come il widget e l'anteprima del menu. Trascina il riquadro sul widget (frecce per i ritocchi) e premi *Conferma*. Il menu può avere 1, 2, 3 o 4 colonne oppure una riga orizzontale. Con più colonne le icone partono dall'alto a sinistra e seguono l'ordine dell'elenco; in colonna singola, sopra il widget, la prima app sta in basso. Se sposti il widget, ripeti da *Impostazioni* (opzione «Posizione del menu»).
5. Per modificarlo in seguito: tieni premuto sul widget → *Impostazioni* (il tocco prolungato è gestito dalla schermata Home di Android e non può aprire il menu).

## Scorciatoie
Le prime app dell'elenco (fino al limite di Android, di solito 4-5) compaiono come scorciatoie tenendo premuta l'icona dell'app WDGT-RM.

## Backup
Nell'app WDGT-RM: *Esporta backup…* salva su file (Drive, Download…) l'elenco generale e tutte le istanze del widget. Dopo una reinstallazione: *Importa backup…*, poi aggiungi i widget e in ognuno usa *Carica da backup…* per applicare un'istanza salvata.

## Personalizzazione
`app/src/main/java/it/wdgtrm/Config.kt` (località, nome e data evento per Sole e Countdown).

## Build
- Locale: Android Studio → Run, oppure `gradle :app:assembleDebug`.
- CI: ogni push genera l'APK come artifact di GitHub Actions (`wdgt-rm-debug-apk`).

## Licenze
Le icone «piene» del set (carrello, razzo, bicicletta, ecc.) sono [Material Icons](https://github.com/google/material-design-icons) di Google, Apache License 2.0: vedi `LICENSES/MaterialIcons-Apache-2.0.txt`.
