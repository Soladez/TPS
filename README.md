# TPS Monitor (Fabric, Minecraft 26.3)

Zeigt die Server-TPS (Ticks pro Sekunde) und MSPT (Millisekunden pro Tick) an.

- `/tps`: zeigt TPS (Durchschnitt über 5 s und 1 min) und MSPT im Chat. Funktioniert auch für Spieler ohne die Mod.
- HUD oben links: zeigt live TPS und MSPT, wenn Server **und** Client die Mod haben (in Einzelspieler immer).
- `/tpshud`: blendet das HUD clientseitig ein oder aus.

Farben: grün ≥ 19.5 TPS, gelb ≥ 15, rot darunter. Kommt länger als 3 s kein Update, wird das HUD rot und zeigt „(keine Antwort)“.

## Bauen über GitHub (ohne lokales Java)

Bei jedem Push baut GitHub Actions die Mod automatisch mit JDK 25 (`.github/workflows/build.yml`).

1. Projekt in ein GitHub-Repository hochladen.
2. Im Repository auf **Actions** → Workflow **build** → neuesten Lauf öffnen.
3. Unten bei **Artifacts** `tps-monitor` herunterladen (ZIP) und entpacken.
4. `tps-monitor-1.0.0.jar` (nicht die `-sources.jar`) zusammen mit der
   [Fabric API](https://modrinth.com/mod/fabric-api) für 26.3 in den `mods`-Ordner legen.

Ein neuer Build lässt sich auch manuell starten: Actions → build → **Run workflow**.
