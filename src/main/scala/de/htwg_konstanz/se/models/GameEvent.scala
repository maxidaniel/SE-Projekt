package de.htwg_konstanz.se.models

class GameEvent

case class JoinEvent(player: Player, game: Game) extends GameEvent
case class StartEvent(game: Game) extends GameEvent