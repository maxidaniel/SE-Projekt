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
      if (game.playerHands.size < 4)
        Failure(Exception("Can only start a new game with four or more players."))
      else
        dealCards(game).map(_.copy(state = PlayingState))

    case Abort =>
      Failure(Exception("Can only abort in playing state!"))

    case Deal =>
      if (game.playerHands.size < 4)
        Failure(Exception("Can only deal cards when four or more players are in the game."))
      else
        dealCards(game)

    case PlayCard(playerId, card) =>
      Failure(Exception("Can only play cards in playing state."))

    case PassTrick(playerId) =>
      Failure(Exception("Can only pass tricks in playing state."))

    case NextRound =>
      Failure(Exception("Can only start next round in ended state."))
  }

  private def dealCards(game: Game): Try[Game] = {
    val shuffledCards = DeckFactory.shuffledStandardDeck().cards
    val players = game.playerHands.keys.toVector

    val dealtHands = shuffledCards.zipWithIndex.foldLeft(game.playerHands) { case (hands, (card, index)) =>
      val player = players(index % players.size)
      hands.updated(player, hands(player) :+ card)
    }

    val first = dealtHands.find((p, hand) => hand.exists(c => c.rank == Card.ThreeOfClubs.rank && c.suit == Card.ThreeOfClubs.suit)).map(p => p._1)
    Success(game.copy(playerHands = dealtHands, playedCards = Vector.empty, currentPlayer = first, passedPlayers = Set.empty))
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

    case NextRound =>
      Failure(Exception("Can only start next round in ended state."))
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
    val cardsOfRank = hand.filter(c => c.rank == card.rank)
    
    if (cardsOfRank.isEmpty) {
      Failure(Exception("Cannot lead with this card - must play cards of same rank."))
    } else if (cardsOfRank.size >= 4) {
      val fourCards = cardsOfRank.take(4)
      val updatedHands = game.playerHands.updated(player, fourCards.foldLeft(hand)((h, c) => h.filterNot(_ == c)))
      val isGameOver = updatedHands(player).isEmpty
      val updatedState = if (isGameOver) EndedState else PlayingState
      val updatedFinishOrder = if (isGameOver && !game.finishOrder.contains(player)) game.finishOrder :+ player else game.finishOrder
      Success(game.copy(
        playerHands = updatedHands,
        playedCards = Vector.empty,
        state = updatedState,
        currentPlayer = if (isGameOver) None else Some(player),
        trickCount = 0,
        trickRank = None,
        trickLeader = None,
        passedPlayers = Set.empty,
        finishOrder = updatedFinishOrder
      ))
    } else {
      val updatedHands = game.playerHands.updated(player, hand.filterNot(_ == card))
      val updatedState = if (updatedHands(player).isEmpty) EndedState else PlayingState
      val updatedFinishOrder = if (updatedHands(player).isEmpty && !game.finishOrder.contains(player)) game.finishOrder :+ player else game.finishOrder
      val nextPlayer = if (updatedState == EndedState) None
                       else game.playerHands.keys.find(_ != player)
      Success(game.copy(
        playerHands = updatedHands,
        playedCards = game.playedCards :+ card,
        state = updatedState,
        currentPlayer = nextPlayer,
        trickCount = 1,
        trickRank = Some(card.rank),
        trickLeader = Some(player),
        finishOrder = updatedFinishOrder
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
    } else if (game.trickRank.exists(tr => Game.getRankPower(card.rank) <= Game.getRankPower(tr))) {
      val rankStr = game.trickRank.map(r => s"$r").getOrElse("unknown")
      Failure(Exception(s"Must play a card higher than $rankStr."))
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
    val isBurn = Game.isBurnCard(card)
    val updatedFinishOrder = if (updatedHands(player).isEmpty && !game.finishOrder.contains(player)) game.finishOrder :+ player else game.finishOrder
    
    val trickOver = isBurn || updatedState == EndedState || {
      val trickWinner = game.trickLeader.getOrElse(player)
      val nonLeaderPlayers = game.playerHands.keys.filterNot(_ == trickWinner).toSet
      val stillActive = nonLeaderPlayers -- game.passedPlayers - player
      stillActive.isEmpty
    }
    
    val nextPlayer = if (updatedState == EndedState) {
      None
    } else if (trickOver && isBurn) {
      Some(player)
    } else if (trickOver) {
      Some(game.trickLeader.getOrElse(player))
    } else {
      val trickWinner = game.trickLeader.getOrElse(player)
      val nonLeaderPlayers = game.playerHands.keys.filterNot(_ == trickWinner).toSet
      val stillActive = nonLeaderPlayers -- game.passedPlayers - player
      stillActive.headOption
    }
    
    Success(game.copy(
      playerHands = updatedHands,
      playedCards = if (trickOver) Vector.empty else game.playedCards :+ card,
      state = updatedState,
      currentPlayer = nextPlayer,
      trickCount = if (trickOver) 0 else newTrickCount,
      trickRank = if (trickOver) None else game.trickRank,
      trickLeader = if (trickOver) None else game.trickLeader,
      passedPlayers = if (trickOver) Set.empty else game.passedPlayers,
      finishOrder = updatedFinishOrder
    ))
  }

  private def passTrick(game: Game, player: IPlayer): Try[Game] = {
    if (game.trickCount == 0) {
      Failure(Exception("Cannot pass when no trick has been led."))
    } else if (game.trickLeader.contains(player)) {
      Failure(Exception("The trick leader cannot pass."))
    } else {
      val updatedPassed = game.passedPlayers + player
      val otherPlayers = game.playerHands.keys.filterNot(_ == game.trickLeader.getOrElse(player)).toSet
      val allOthersPassed = otherPlayers.subsetOf(updatedPassed)
      
      if (allOthersPassed) {
        val trickWinner = game.trickLeader.get
        val winnerHasCards = game.playerHands.get(trickWinner).exists(_.nonEmpty)
        val updatedState = if (!winnerHasCards) EndedState else PlayingState
        val updatedFinishOrder = if (!winnerHasCards && !game.finishOrder.contains(trickWinner)) game.finishOrder :+ trickWinner else game.finishOrder
        
        Success(game.copy(
          playedCards = Vector.empty,
          trickCount = 0,
          trickRank = None,
          trickLeader = None,
          passedPlayers = Set.empty,
          currentPlayer = if (updatedState == EndedState) None else Some(trickWinner),
          state = updatedState,
          finishOrder = updatedFinishOrder
        ))
      } else {
        val nextPlayer = game.playerHands.keys.find(p => 
          p != player && 
          !updatedPassed.contains(p) && 
          !game.trickLeader.contains(p)
        ).orElse(game.trickLeader)
        Success(game.copy(
          currentPlayer = nextPlayer,
          passedPlayers = updatedPassed
        ))
      }
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

  override def transition(game: Game, operation: GameOperation): Try[Game] = operation match {
    case NextRound =>
      val totalPlayers = game.playerHands.size
      if (totalPlayers < 4) {
        Failure(Exception("Need at least 4 players for next round."))
      } else {
        val newScores = game.finishOrder.zipWithIndex.foldLeft(game.scoredRanks) { case (scores, (player, position)) =>
          val points = Game.scoreForPosition(position, totalPlayers)
          scores.updated(player, scores.getOrElse(player, 0) + points)
        }
        val gameOver = newScores.exists(_._2 >= 11)
        if (gameOver) {
          Failure(Exception("Game is over - someone reached 11 points."))
        } else {
          val shuffledCards = DeckFactory.shuffledStandardDeck().cards
          val players = game.playerHands.keys.toVector
          val dealtHands = shuffledCards.zipWithIndex.foldLeft(Map.empty[IPlayer, Vector[Card]]) { case (hands, (card, index)) =>
            val player = players(index % players.size)
            hands.updated(player, hands.getOrElse(player, Vector.empty) :+ card)
          }
          val sortedByRank = game.finishOrder
          val president = sortedByRank.headOption
          val vicePresident = sortedByRank.drop(1).headOption
          val viceScum = sortedByRank.dropRight(1).headOption
          val scum = sortedByRank.lastOption
          
          val exchangedHands = (president, scum) match {
            case (Some(pres), Some(sc)) =>
              val vp = vicePresident.filter(p => p != pres && p != sc)
              val vsc = viceScum.filter(p => p != pres && p != sc && !vp.contains(p))
              Game.exchangeCards(pres, sc, vp, vsc, dealtHands)
            case _ => dealtHands
          }
          
          val first = exchangedHands.find((p, hand) => hand.exists(c => c.rank == Card.ThreeOfClubs.rank && c.suit == Card.ThreeOfClubs.suit)).map(p => p._1)
          Success(Game(
            playerHands = exchangedHands,
            playedCards = Vector.empty,
            state = PlayingState,
            currentPlayer = first,
            trickCount = 0,
            trickRank = None,
            trickLeader = None,
            passedPlayers = Set.empty,
            scoredRanks = newScores,
            roundNumber = game.roundNumber + 1,
            finishOrder = Vector.empty
          ))
        }
      }
    case _ =>
      Failure(Exception("Cannot perform this operation in ended state."))
  }
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
case object NextRound extends GameOperation
