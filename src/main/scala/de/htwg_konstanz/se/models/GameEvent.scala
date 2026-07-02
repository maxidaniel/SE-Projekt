package de.htwg_konstanz.se.models

import scala.util.Failure

class GameEvent

enum TableClearReason:
  case BurnByTwo, FourOfAKindBomb, TrickWon

case class PlayerJoinEvent(player: IPlayer, game: Game) extends GameEvent
case class PlayerQuitEvent(player: IPlayer, game: Game) extends GameEvent

case class GameStartedEvent(game: Game) extends GameEvent
case class GameAbortedEvent(game: Game) extends GameEvent
case class GameEndedEvent(game: Game, winner: IPlayer) extends GameEvent
case class CardPlayedEvent(player: IPlayer, card: Card, game: Game) extends GameEvent
case class PassTrickEvent(player: IPlayer, game: Game) extends GameEvent
case class NextRoundEvent(game: Game) extends GameEvent
case class GameChangedEvent(game: Game) extends GameEvent
case class TableClearedEvent(player: IPlayer, game: Game, reason: TableClearReason) extends GameEvent

case object GameExitEvent extends GameEvent

case class GameErrorEvent(cause: GameEvent, error: Failure[Game]) extends GameEvent