# TPS Monitor (Fabric, Minecraft 26.3)

Zeigt die Server-TPS (Ticks pro Sekunde) und MSPT (Millisekunden pro Tick) an.

**Funktioniert auf jedem Server**, auch wenn die Mod dort nicht installiert ist:

- `/tps` (Client): zeigt die Server-TPS im Chat.
- HUD oben links: zeigt die TPS live.
- `/tpshud`: blendet das HUD ein oder aus.

Ohne Mod auf dem Server misst der Client die TPS selbst: Vanilla-Server schicken jede Sekunde
die Spielzeit, daraus ergibt sich, wie viele Ticks pro Sekunde wirklich laufen (Mittel über ca. 10 s).
Ist die Mod **auch auf dem Server**, kommen genaue Werte inklusive MSPT direkt vom Server.
Auf dem Server selbst (Konsole) gibt `/tps` zusätzlich den 5-s- und 1-min-Durchschnitt aus.

Farben: grün ≥ 19.5 TPS, gelb ≥ 15, rot darunter. Kommt länger als 3 s kein Update, wird das HUD rot und zeigt „(keine Antwort)“.

## Bauen über GitHub (ohne lokales Java)

Bei jedem Push baut GitHub Actions die Mod automatisch mit JDK 25 (`.github/workflows/build.yml`).

1. Projekt in ein GitHub-Repository hochladen.
2. Im Repository auf **Actions** → Workflow **build** → neuesten Lauf öffnen.
3. Unten bei **Artifacts** `tps-monitor` herunterladen (ZIP) und entpacken.
4. `tps-monitor-1.0.0.jar` (nicht die `-sources.jar`) zusammen mit der
   [Fabric API](https://modrinth.com/mod/fabric-api) für 26.3 in den `mods`-Ordner legen.

Ein neuer Build lässt sich auch manuell starten: Actions → build → **Run workflow**.
