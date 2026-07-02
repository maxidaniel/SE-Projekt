# Spielregeln: President

## Überblick

President ist ein Kartenspiel für 4 bis 8 Spieler. Ziel ist es, als Erster alle Karten abzulegen.
Am Ende jeder Runde wird ein Präsident, Vizepräsident, Vize-Schwein und Schwein ermittelt.
Zwischen diesen wird ein Karten austausch durchgeführt.

## Karten

- 52 Karten (4 Farben: Kreuz, Pik, Herz, Karo; 13 Ränge: 3 bis 2)
- Rangordnung (aufsteigend): 3, 4, 5, 6, 7, 8, 9, 10, Bube, Dame, König, Ass, 2
- Die 3 Kreuz beginnt immer als erste Karte.

## Spielverlauf

### Runde

1. Alle 52 Karten werden gleichmäßig an die Spieler verteilt.
2. Der Spieler mit der 3 Kreuz beginnt und muss diese als Erstes ausspielen.
3. Im Uhrzeigerspiel müssen die folgenden Spieler eine Karte ausspielen, die
   höher ist als die letzte ausgespielte Karte.
4. Ein Spieler kann auch passen (keine Karte ausspielen).
5. Wenn alle Spieler bis auf den Stichführer gepasst haben, gewinnt der Stichführer
   den Stich und darf eine neue Runde eröffnen.

### Besondere Karten

- **Zwei (Burn):** Eine Zwei löscht den Tisch. Der Spieler, der die Zwei ausgespielt
  hat, darf eine neue Runde eröffnen.
- **Vier gleiche (Bomb):** Vier Karten desselben Rangs können als Bombe ausgespielt
  werden. Dies setzt den Tisch zurück und der Spieler darf eine neue Runde eröffnen.

### Rundenende

- Wenn ein Spieler keine Karten mehr hat, ist er fertig.
- Die Reihenfolge der Spieler, die fertig geworden sind, wird festgehalten.
- Das Ende einer Runde tritt ein, wenn:
  - Alle Spieler bis auf einen gepasst haben, oder
  - Ein Spieler alle seine Karten ausgespielt hat.

## Rangfolge und Punkte

Nach jeder Runde wird ein Präsident, Vizepräsident, Vize-Schwein und Schwein ermittelt:

| Platz | Titel            | Punkte |
|-------|------------------|--------|
| 1     | Präsident        | 2      |
| 2     | Vizepräsident    | 1      |
| 3+    | Keine            | 0      |

Das Spiel endet, wenn ein Spieler 11 oder mehr Punkte erreicht.

## Karten austausch

Zwischen den Runden wird ein Karten austausch durchgeführt:

1. **Präsident <-> Schwein:** Der Präsident gibt seine 2 schlechtesten Karten
   an das Schwein und bekommt dessen 2 besten Karten.
2. **Vizepräsident <-> Vize-Schwein:** Der Vizepräsident gibt seine 1 schlechteste
   Karte an das Vize-Schwein und bekommt dessen 1 beste Karte.

## Spielmodi

- **Warten auf Spieler:** Spieler können beitreten oder das Spiel verlassen.
- **Spiel läuft:** Karten werden ausgespielt, Stiche werden gemacht.
- **Abgebrochen:** Das Spiel wurde abgebrochen.
- **Beendet:** Die Runde ist vorbei, Punkte werden vergeben.

## Spieler

- **Menschlicher Spieler:** Wird manuell gesteuert.
- **Computer-Spieler:** Wird von einer KI-Strategie gesteuert.

### KI-Strategien

- **Niedrigste Karte:** Spielt immer die niedrigste gültige Karte.
- **Zufällige Karte:** Spielt eine zufällige gültige Karte.
- **Beste Karte:** Spielt die höchste gültige Karte.
