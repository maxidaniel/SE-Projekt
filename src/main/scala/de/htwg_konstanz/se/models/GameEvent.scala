package de.htwg_konstanz.se.models

import scala.util.Failure

class GameEvent

case class PlayerJoinEvent(player: Player, game: Game) extends GameEvent
case class PlayerQuitEvent(player: Player, game: Game) extends GameEvent

case class GameStartedEvent(game: Game) extends GameEvent
case class GameAbortedEvent(game: Game) extends GameEvent
case class GameEndedEvent(game: Game, winner: Player) extends GameEvent
case class CardPlayedEvent(player: Player, card: Card, game: Game) extends GameEvent
case class GameChangedEvent(game: Game) extends GameEvent

case object GameExitEvent extends GameEvent

case class GameErrorEvent(cause: GameEvent, error: Failure[Game]) extends GameEvent