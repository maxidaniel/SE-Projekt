# President - Kartenspiel

[![Scala CI](https://github.com/maxidaniel/SE-Projekt/actions/workflows/build.yml/badge.svg)](https://github.com/maxidaniel/SE-Projekt/actions/workflows/build.yml) [![Coverage Status](https://coveralls.io/repos/github/maxidaniel/SE-Projekt/badge.svg?branch=main)](https://coveralls.io/github/maxidaniel/SE-Projekt?branch=main)

Ein President-Kartenspiel implementiert in Scala, mit menschlichen Spielern und Computergegner verschiedener Schwierigkeitsstufen.

## Funktionen

- Vollständige Implementierung der President-Spielregeln
- Unterstützung für 4-8 Spieler
- Spielmodi für menschliche Spieler und Computergegner
- Drei Strategien für Computergegner: niedrigste Karte, zufällige Karte, höchste Karte
- Kartentausch zwischen Präsident, Vizepräsident, Vize-Schwein und Schwein
- Spielverwaltung mit verschiedenen Phasen
- Umfassende Testabdeckung

## Spielregeln

### Überblick

President ist ein Kartenspiel für 4 bis 8 Spieler. Ziel ist es, als Erster alle Karten abzulegen. Am Ende jeder Runde wird ein Präsident, Vizepräsident, Vize-Schwein und Schwein ermittelt. Zwischen diesen wird ein Kartentausch durchgeführt.

### Karten

- **52 Karten** (4 Farben: Kreuz, Pik, Herz, Karo; 13 Ränge: 3 bis 2)
- **Rangordnung** (aufsteigend): 3, 4, 5, 6, 7, 8, 9, 10, Bube, Dame, König, Ass, 2
- Die 3 Kreuz beginnt immer als erste Karte

### Spielverlauf

1. Alle 52 Karten werden gleichmäßig an die Spieler verteilt
2. Der Spieler mit der 3 Kreuz beginnt und muss diese als Erstes ausspielen
3. Im Uhrzeigerspiel müssen die folgenden Spieler eine Karte ausspielen, die höher ist als die letzte ausgespielte Karte
4. Ein Spieler kann auch passen (keine Karte ausspielen)
5. Wenn alle Spieler bis auf den Stichführer gepasst haben, gewinnt der Stichführer den Stich und darf eine neue Runde eröffnen

### Besondere Karten

- **Zwei (Burn):** Eine Zwei löscht den Tisch. Der Spieler, der die Zwei ausgespielt hat, darf eine neue Runde eröffnen.
- **Vier gleiche (Bomb):** Vier Karten desselben Rangs können als Bombe ausgespielt werden. Dies setzt den Tisch zurück und der Spieler darf eine neue Runde eröffnen.

### Rundenende

- Wenn ein Spieler keine Karten mehr hat, ist er fertig
- Die Reihenfolge der Spieler, die fertig geworden sind, wird festgehalten
- Das Ende einer Runde tritt ein, wenn:
  - Alle Spieler bis auf einen gepasst haben, oder
  - Ein Spieler alle seine Karten ausgespielt hat

### Rangfolge und Punkte

Nach jeder Runde wird ein Präsident, Vizepräsident, Vize-Schwein und Schwein ermittelt:

| Platz | Titel | Punkte |
|-------|-------|--------|
| 1 | Präsident | 2 |
| 2 | Vizepräsident | 1 |
| 3+ | Keine | 0 |

Das Spiel endet, wenn ein Spieler 11 oder mehr Punkte erreicht.

### Kartentausch

Zwischen den Runden wird ein Kartentausch durchgeführt:

1. **Präsident <-> Schwein:** Der Präsident gibt seine 2 schlechtesten Karten an das Schwein und bekommt dessen 2 besten Karten.
2. **Vizepräsident <-> Vize-Schwein:** Der Vizepräsident gibt seine 1 schlechteste Karte an das Vize-Schwein und bekommt dessen 1 beste Karte.

### Spielmodi

- **Warten auf Spieler:** Spieler können beitreten oder das Spiel verlassen
- **Spiel läuft:** Karten werden ausgespielt, Stiche werden gemacht
- **Abgebrochen:** Das Spiel wurde abgebrochen
- **Beendet:** Die Runde ist vorbei, Punkte werden vergeben

### Computergegner-Strategien

- **Niedrigste Karte:** Spielt immer die niedrigste gültige Karte
- **Zufällige Karte:** Spielt eine zufällige gültige Karte
- **Beste Karte:** Spielt die höchste gültige Karte

## Voraussetzungen

- JDK 8 oder höher
- sbt (Scala Build Tool)
- Docker (optional)

## Installation

```bash
git clone https://github.com/maxidaniel/SE-Projekt.git
cd SE-Projekt
sbt compile
```

## Verwendung

### Befehle

| Befehl | Beschreibung |
|--------|--------------|
| `sbt compile` | Projekt kompilieren |
| `sbt run` | Spiel starten |
| `sbt test` | Tests ausführen |
| `sbt coverage` | Testabdeckung aktivieren |
| `sbt coverageReport` | Testabdeckungsbericht erstellen |

### Kommandozeilenargumente

| Argument | Beschreibung |
|----------|--------------|
| `--gui` | Grafische Benutzeroberfläche starten (Standard) |
| `--tui` | Terminalbenutzeroberfläche starten |
| `--json` | Spielstand im JSON-Format speichern (Standard) |
| `--xml` | Spielstand im XML-Format speichern |

**Beispiele:**

```bash
# GUI mit JSON-Speicherung starten (Standard)
sbt run

# TUI starten
sbt run --tui

# GUI mit XML-Speicherung starten
sbt run --gui --xml

# TUI mit XML-Speicherung starten
sbt run --tui --xml
```

## Docker

### Image erstellen

```bash
docker build -t president .
```

### Container ausführen

```bash
# Standard: sbt "run --tui --json"
docker run -it --rm president

# GUI starten
docker run -it --rm president run --gui --json

# TUI mit XML-Speicherung starten
docker run -it --rm president run --tui --xml

# Beliebige sbt-Kommandos
docker run -it --rm president test
docker run -it --rm president "clean;compile"
```

CLI-Argumente hinter dem Imagenamen werden direkt als sbt-Kommando verwendet.

### GUI mit Host-Fenster (X11 / XWayland)

```bash
# Beispiel: GUI mit korrekt gesetztem DISPLAY
# Falls DISPLAY leer ist: export DISPLAY=:0
docker run -it --rm \
  -e DISPLAY="${DISPLAY:-:0}" \
  -e XAUTHORITY=/tmp/.Xauthority \
  -v /tmp/.X11-unix:/tmp/.X11-unix:rw \
  -v "${XAUTHORITY:-$HOME/.Xauthority}:/tmp/.Xauthority:ro" \
  president run --gui --json
```

## Projektstruktur

```
src/
├── main/scala/de/htwg_konstanz/se/
│   ├── President.scala              # Hauptprogramm
│   ├── PresidentModule.scala        # Guice-Modul
│   ├── models/                      # Spielmodelle
│   │   ├── Card.scala
│   │   ├── Game.scala
│   │   ├── GameState.scala
│   │   ├── GameEvent.scala
│   │   ├── GameFactory.scala
│   │   └── Player.scala
│   ├── controller/                  # Spiellogik
│   │   ├── GameController.scala
│   │   └── strategies/
│   │       ├── IStrategy.scala
│   │       ├── PlayLowestPossibleCardStrategy.scala
│   │       ├── PlayRandomCardStrategy.scala
│   │       └── PlayBestCardStrategy.scala
│   ├── ui/
│   │   ├── tui/                     # Terminal-Oberfläche
│   │   │   ├── TuiReisen.scala
│   │   │   ├── TuiPresenter.scala
│   │   │   ├── TuiColors.scala
│   │   │   ├── TerminalRenderer.scala
│   │   │   ├── CardRenderer.scala
│   │   │   └── ConsoleCanvas.scala
│   │   └── gui/                     # Grafische Oberfläche
│   │       ├── GuiPresident.scala
│   │       ├── GuiPresenter.scala
│   │       ├── GuiViews.scala
│   │       ├── PresidentViewModel.scala
│   │       ├── IGuiPresenter.scala
│   │       ├── components/
│   │       │   └── CardComponent.scala
│   │       └── views/
│   │           ├── MenuView.scala
│   │           ├── LobbyView.scala
│   │           ├── GameView.scala
│   │           └── ResultView.scala
│   └── io/                          # Speicherfunktionen
│       ├── ISaveManager.scala
│       ├── JsonSaveManager.scala
│       └── XmlSaveManager.scala
└── test/scala/                      # Testdateien
```

## Kontakt

Max Daniel - [@maxidaniel](https://github.com/maxidaniel)

Projekt: [https://github.com/maxidaniel/SE-Projekt](https://github.com/maxidaniel/SE-Projekt)
