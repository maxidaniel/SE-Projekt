package de.htwg_konstanz.se.models

import scala.util.{Failure, Success, Try}

sealed trait GameState {
  def canJoin: Boolean
  def canQuit: Boolean
  def canStart: Boolean
  def canAbort: Boolean
  def canDeal: Boolean
  def canPlayCard: Boolean
  def canPassTrick: Boolean

  def transition(game: Game, operation: GameOperation): Try[Game]
}

case object WaitingForPlayersState extends GameState {
  override def canJoin: Boolean = true
  override def canQuit: Boolean = true
  override def canStart: Boolean = true
  override def canAbort: Boolean = false
  override def canDeal: Boolean = true
  override def canPlayCard: Boolean = false
  override def canPassTrick: Boolean = false

  override def transition(game: Game, operation: GameOperation): Try[Game] = operation match {
    case Join(player) =>
      if (game.playerHands.contains(player))
        Failure(Exception(s"The player with id ${player.id} is already part of the game."))
      else
        Success(game.copy(playerHands = game.playerHands + (player -> Vector())))

    case Quit(player) =>
      if (!game.playerHands.contains(player))
        Failure(Exception(s"The player with id ${player.id} is not part of the game."))
      else
        Success(game.copy(playerHands = game.playerHands - player))

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

    case PassTrick(playerId) =>
      Failure(Exception("Can only pass tricks in playing state."))
  }

  private def dealCards(game: Game): Try[Game] = {
    val shuffledCards = DeckFactory.shuffledStandardDeck().cards
    val players = game.playerHands.keys.toVector

    val dealtHands = shuffledCards.zipWithIndex.foldLeft(game.playerHands) { case (hands, (card, index)) =>
      val player = players(index % players.size)
      hands.updated(player, hands(player) :+ card)
    }

    val first = dealtHands.find((p, hand) => hand.exists(c => c.rank == Card.ThreeOfClubs.rank && c.suit == Card.ThreeOfClubs.suit)).map(p => p._1)
    Success(game.copy(playerHands = dealtHands, playedCards = Vector.empty, currentPlayer = first))
  }
}

case object StartingState extends GameState {
  override def canJoin: Boolean = false
  override def canQuit: Boolean = false
  override def canStart: Boolean = false
  override def canAbort: Boolean = false
  override def canDeal: Boolean = false
  override def canPlayCard: Boolean = false
  override def canPassTrick: Boolean = false

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
  override def canPassTrick: Boolean = true

  override def transition(game: Game, operation: GameOperation): Try[Game] = operation match {
    case Join(_) =>
      Failure(Exception("Cannot join a running game."))

    case Quit(_) =>
      Failure(Exception("Cannot quit a running game."))

    case Start =>
      Failure(Exception("Can only start a new game when in lobby."))

    case Abort =>
      Success(game.copy(state = AbortedState))

    case Deal =>
      Failure(Exception("Can only deal cards before the game starts."))

    case PlayCard(player, card) =>
      if (card == Card.Unknown)
        Failure(Exception("Cannot play an unknown card."))
      else
        playCard(game, player, card)

    case PassTrick(player) =>
      passTrick(game, player)
  }

  private def playCard(game: Game, player: IPlayer, card: Card): Try[Game] = {
    game.playerHands.get(player) match {
      case None => Failure(Exception(s"The player with id ${player.id} is not part of the game."))
      case Some(hand) if !hand.contains(card) => Failure(Exception(s"Player ${player.id} does not have card $card in hand."))
      case Some(hand) if !game.currentPlayer.contains(player) => Failure(Exception("It is not this players' turn."))
      case _ =>
        if (game.trickCount == 0 && game.playedCards.isEmpty) {
          leadTrick(game, player, card)
        } else {
          respondToTrick(game, player, card)
        }
    }
  }

  private def leadTrick(game: Game, player: IPlayer, card: Card): Try[Game] = {
    val hand = game.playerHands(player)
    val cardsOfRank = hand.count(c => c.rank == card.rank)
    val validCount = math.min(cardsOfRank, 4)
    
    if (validCount < 1) {
      Failure(Exception("Cannot lead with this card - must play cards of same rank."))
    } else {
      val updatedHands = game.playerHands.updated(player, hand.filterNot(_ == card))
      val updatedState = if (updatedHands(player).isEmpty) EndedState else PlayingState
      val nextPlayer = if (updatedState == EndedState) None
                       else game.playerHands.keys.find(_ != player)
      Success(game.copy(
        playerHands = updatedHands,
        playedCards = game.playedCards :+ card,
        state = updatedState,
        currentPlayer = nextPlayer,
        trickCount = 1,
        trickRank = Some(card.rank),
        trickLeader = Some(player)
      ))
    }
  }

  private def respondToTrick(game: Game, player: IPlayer, card: Card): Try[Game] = {
    val hand = game.playerHands(player)
    val lastPlayed = game.playedCards.lastOption
    
    if (!hand.contains(card)) {
      Failure(Exception(s"Player $player does not have card $card in hand."))
    } else if (lastPlayed.isEmpty) {
      Failure(Exception("No cards have been played yet."))
    } else if (!game.trickRank.contains(card.rank)) {
      val rankStr = game.trickRank.map(r => s"$r").getOrElse("unknown")
      Failure(Exception(s"Must play cards of rank $rankStr, not ${card.rank}."))
    } else if (game.trickCount >= 4) {
      Failure(Exception("Trick already has 4 cards - cannot play more."))
    } else if (!Game.canBeat(card, lastPlayed.get)) {
      Failure(Exception("Played card must outrank the previous card."))
    } else {
      completeCardPlay(game, player, card)
    }
  }

  private def completeCardPlay(game: Game, player: IPlayer, card: Card): Try[Game] = {
    val hand = game.playerHands(player)
    val updatedHands = game.playerHands.updated(player, hand.filterNot(_ == card))
    val newTrickCount = game.trickCount + 1
    val updatedState = if (updatedHands(player).isEmpty) EndedState else PlayingState
    
    val nextPlayer = if (updatedState == EndedState) {
      None
    } else if (newTrickCount >= 4) {
      game.trickLeader
    } else {
      game.playerHands.keys.find(_ != player)
    }
    
    Success(game.copy(
      playerHands = updatedHands,
      playedCards = game.playedCards :+ card,
      state = updatedState,
      currentPlayer = nextPlayer,
      trickCount = newTrickCount,
      trickRank = game.trickRank,
      trickLeader = game.trickLeader
    ))
  }

  private def passTrick(game: Game, player: IPlayer): Try[Game] = {
    if (game.trickCount == 0) {
      Failure(Exception("Cannot pass when no trick has been led."))
    } else if (game.trickLeader.contains(player)) {
      Failure(Exception("The trick leader cannot pass."))
    } else {
      val nextPlayer = game.playerHands.keys.find(_ != player)
      Success(game.copy(currentPlayer = nextPlayer))
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
  override def canPassTrick: Boolean = false

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
  override def canPassTrick: Boolean = false

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

case class Join(player: IPlayer) extends GameOperation
case class Quit(player: IPlayer) extends GameOperation
case object Start extends GameOperation
case object Abort extends GameOperation
case object Deal extends GameOperation
case class PlayCard(player: IPlayer, card: Card) extends GameOperation
case class PassTrick(player: IPlayer) extends GameOperation
