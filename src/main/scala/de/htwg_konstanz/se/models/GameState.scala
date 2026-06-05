package de.htwg_konstanz.se.models

enum GameState {
  case WaitingForPlayers
  case Starting
  case Playing
  case Aborted
  case Ended
}