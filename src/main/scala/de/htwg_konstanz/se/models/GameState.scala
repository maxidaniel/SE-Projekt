package de.htwg_konstanz.se.models

import scala.util.{Failure, Success, Try}

sealed trait GameState {
  def canJoin: Boolean
  def canQuit: Boolean
  def canStart: Boolean
  def canAbort: Boolean
  def canDeal: Boolean
  def canPlayCard: Boolean

  def transition(game: Game, operation: GameOperation): Try[Game]
}

case object WaitingForPlayersState extends GameState {
  override def canJoin: Boolean = true
  override def canQuit: Boolean = true
  override def canStart: Boolean = true
  override def canAbort: Boolean = false
  override def canDeal: Boolean = true
  override def canPlayCard: Boolean = false

  override def transition(game: Game, operation: GameOperation): Try[Game] = operation match {
    case Join(playerId) =>
      if (game.playerHands.contains(playerId))
        Failure(Exception(s"The player with id $playerId is already part of the game."))
      else
        Success(game.copy(playerHands = game.playerHands + (playerId -> Vector())))

    case Quit(playerId) =>
      if (!game.playerHands.contains(playerId))
        Failure(Exception(s"The player with id $playerId is not part of the game."))
      else
        Success(game.copy(playerHands = game.playerHands - playerId))

    case Start =>
      if (game.playerHands.size < 2)
        Failure(Exception("Can only start a new game with two or more players."))
      else
        dealCards(game).map(_.copy(state = PlayingState))

    case Abort =>
      Failure(Exception("Can only abort in playing state!"))

    case Deal =>
      if (game.playerHands.size < 2)
        Failure(Exception("Can only deal cards when two or more players are in the game."))
      else
        dealCards(game)

    case PlayCard(playerId, card) =>
      Failure(Exception("Can only play cards in playing state."))
  }

  private def dealCards(game: Game): Try[Game] = {
    val shuffledCards = DeckFactory.shuffledStandardDeck().cards
    val playerIds = game.playerHands.keys.toVector
    val emptyHands = playerIds.map(playerId => playerId -> Vector.empty[Card]).toMap

    val dealtHands = shuffledCards.zipWithIndex.foldLeft(emptyHands) { case (hands, (card, index)) =>
      val playerId = playerIds(index % playerIds.size)
      hands.updated(playerId, hands(playerId) :+ card)
    }

    Success(game.copy(playerHands = dealtHands, playedCards = Vector.empty))
  }
}

case object StartingState extends GameState {
  override def canJoin: Boolean = false
  override def canQuit: Boolean = false
  override def canStart: Boolean = false
  override def canAbort: Boolean = false
  override def canDeal: Boolean = false
  override def canPlayCard: Boolean = false

  override def transition(game: Game, operation: GameOperation): Try[Game] =
    Failure(Exception("Cannot perform this operation in starting state."))
}

case object PlayingState extends GameState {
  override def canJoin: Boolean = false
  override def canQuit: Boolean = false
  override def canStart: Boolean = false
  override def canAbort: Boolean = true
  override def canDeal: Boolean = false
  override def canPlayCard: Boolean = true

  override def transition(game: Game, operation: GameOperation): Try[Game] = operation match {
    case Join(playerId) =>
      Failure(Exception("Cannot join a running game."))

    case Quit(playerId) =>
      Failure(Exception("Cannot quit a running game."))

    case Start =>
      Failure(Exception("Can only start a new game when in lobby."))

    case Abort =>
      Success(game.copy(state = AbortedState))

    case Deal =>
      Failure(Exception("Can only deal cards before the game starts."))

    case PlayCard(playerId, card) =>
      if (card == Card.Unknown)
        Failure(Exception("Cannot play an unknown card."))
      else
        playCard(game, playerId, card)
  }

  private def playCard(game: Game, playerId: java.util.UUID, card: Card): Try[Game] = {
    game.playerHands.get(playerId) match {
      case None => Failure(Exception(s"The player with id $playerId is not part of the game."))
      case Some(hand) if !hand.contains(card) => Failure(Exception(s"Player $playerId does not have card $card in hand."))
      case Some(hand) if game.currentPlayer.exists(_ != playerId) =>
        Failure(Exception("It is not the player's turn - not the turn."))
      case Some(hand) if game.playedCards.lastOption.exists(lastPlayed => !Game.canBeat(card, lastPlayed)) =>
        Failure(Exception("Played card must outrank the previous card."))
      case Some(hand) =>
        val updatedHands = game.playerHands.updated(playerId, hand.filterNot(_ == card))
        val updatedState = if (updatedHands(playerId).isEmpty) EndedState else PlayingState
        val nextPlayer = if (updatedState == EndedState) None
                         else game.playerHands.keys.find(_ != playerId)
        Success(game.copy(
          playerHands = updatedHands,
          playedCards = game.playedCards :+ card,
          state = updatedState,
          currentPlayer = nextPlayer
        ))
    }
  }
}

case object AbortedState extends GameState {
  override def canJoin: Boolean = false
  override def canQuit: Boolean = false
  override def canStart: Boolean = false
  override def canAbort: Boolean = false
  override def canDeal: Boolean = false
  override def canPlayCard: Boolean = false

  override def transition(game: Game, operation: GameOperation): Try[Game] =
    Failure(Exception("Cannot perform this operation in aborted state."))
}

case object EndedState extends GameState {
  override def canJoin: Boolean = false
  override def canQuit: Boolean = false
  override def canStart: Boolean = false
  override def canAbort: Boolean = false
  override def canDeal: Boolean = false
  override def canPlayCard: Boolean = false

  override def transition(game: Game, operation: GameOperation): Try[Game] =
    Failure(Exception("Cannot perform this operation in ended state."))
}

object GameState {
  val WaitingForPlayers: GameState = WaitingForPlayersState
  val Starting: GameState = StartingState
  val Playing: GameState = PlayingState
  val Aborted: GameState = AbortedState
  val Ended: GameState = EndedState

  def values: Seq[GameState] = Seq(WaitingForPlayers, Starting, Playing, Aborted, Ended)
}

sealed trait GameOperation

case class Join(playerId: java.util.UUID) extends GameOperation
case class Quit(playerId: java.util.UUID) extends GameOperation
case object Start extends GameOperation
case object Abort extends GameOperation
case object Deal extends GameOperation
case class PlayCard(playerId: java.util.UUID, card: Card) extends GameOperation
