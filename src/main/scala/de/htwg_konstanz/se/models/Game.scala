package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState.{Aborted, Playing, WaitingForPlayers}

import java.util.UUID
import scala.util.{Failure, Success, Try}

case class Game(playerHands: Map[UUID, Vector[Card]], playedCards: Vector[Card], state: GameState) {
  def this() = {
    this(Map.empty, Vector.empty, state = WaitingForPlayers)
  }

  def join(playerId: UUID): Try[Game] = {
    // Only allow players to join outside of a running game
    if state != WaitingForPlayers then Failure(Exception("Cannot join a running game."))
    else if playerHands.contains(playerId) then Failure(Exception(s"The player with id $playerId is already part of the game."))
    else Success(this.copy(playerHands = playerHands + (playerId -> Vector())))
  }

  def quit(playerId: UUID): Try[Game] = {
    // TODO: handle cards returning to deck if quit during play is allowed
    if state != WaitingForPlayers then Failure(Exception("Cannot quit a running game."))
    else if !playerHands.contains(playerId) then Failure(Exception(s"The player with id $playerId is not part of the game."))
    else Success(this.copy(playerHands = playerHands - playerId))
  }

  def start(): Try[Game] = {
    if state != WaitingForPlayers then Failure(Exception("Can only start a new game when in lobby."))
    else if playerHands.size < 2 then Failure(Exception("Can only start a new game with two or more players."))
    else this.copy(state = GameState.Starting).deal().map(_.copy(state = Playing))
  }

  def abort(): Try[Game] = {
    if state != Playing then return Failure[Game](Exception("Can only abort in playing state!"))

    Success[Game](this.copy(state = Aborted))
  }

  def deal(): Try[Game] = {
    if state != WaitingForPlayers && state != GameState.Starting then Failure(Exception("Can only deal cards before the game starts."))
    else if playerHands.size < 2 then Failure(Exception("Can only deal cards when two or more players are in the game."))
    else {
      val shuffledCards = new Deck().shuffle().cards
      val playerIds = playerHands.keys.toVector
      val emptyHands = playerIds.map(playerId => playerId -> Vector.empty[Card]).toMap

      val dealtHands = shuffledCards.zipWithIndex.foldLeft(emptyHands) { case (hands, (card, index)) =>
        val playerId = playerIds(index % playerIds.size)
        hands.updated(playerId, hands(playerId) :+ card)
      }

      Success(this.copy(playerHands = dealtHands, playedCards = Vector.empty))
    }
  }

  def playCard(playerId: UUID, card: Card): Try[Game] = {
    if state != Playing then Failure(Exception("Can only play cards in playing state."))
    else if card == Card.Unknown then Failure(Exception("Cannot play an unknown card."))
    else {
      playerHands.get(playerId) match {
        case None => Failure(Exception(s"The player with id $playerId is not part of the game."))
        case Some(hand) if !hand.contains(card) => Failure(Exception(s"Player $playerId does not have card $card in hand."))
        case Some(hand) if playedCards.lastOption.exists(lastPlayed => !Game.canBeat(card, lastPlayed)) =>
          Failure(Exception("Played card must outrank the previous card."))
        case Some(hand) =>
          val updatedHands = playerHands.updated(playerId, hand.filterNot(_ == card))
          val updatedState = if updatedHands(playerId).isEmpty then GameState.Ended else state
          Success(this.copy(playerHands = updatedHands, playedCards = playedCards :+ card, state = updatedState))
      }
    }
  }
}

case class Deck(cards: Vector[Card]) {
  def this() = {
    this(Card.standardDeckCards)
  }

  def shuffle(): Deck = {
    copy(cards = scala.util.Random.shuffle(cards))
  }
}

object Game {
  private val rankPower: Map[CardRank, Int] = Map(
    CardRank.Three -> 1,
    CardRank.Four -> 2,
    CardRank.Five -> 3,
    CardRank.Six -> 4,
    CardRank.Seven -> 5,
    CardRank.Eight -> 6,
    CardRank.Nine -> 7,
    CardRank.Ten -> 8,
    CardRank.Jack -> 9,
    CardRank.Queen -> 10,
    CardRank.King -> 11,
    CardRank.Ace -> 12,
    CardRank.Two -> 13,
  )

  private def canBeat(current: Card, previous: Card): Boolean = {
    rankPower(current.rank) > rankPower(previous.rank)
  }
}