package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState._

import java.util.UUID
import scala.util.{Failure, Success, Try}

trait IGame {
  def join(id: UUID): Try[IGame]

  def quit(id: UUID): Try[IGame]

  def start(): Try[IGame]

  def abort(): Try[IGame]

  def deal(): Try[IGame]

  def playCard(player: IPlayer, card: Card): Try[IGame]
}

case class Game(playerHands: Map[UUID, Vector[Card]], playedCards: Vector[Card], state: GameState, playerNames: Map[UUID, String] = Map.empty, currentPlayer: Option[UUID] = None) {
  def this() = {
    this(Map.empty, Vector.empty, WaitingForPlayers, Map.empty, None)
  }

  def join(playerId: UUID): Try[Game] = state.transition(this, Join(playerId))

  def quit(playerId: UUID): Try[Game] = state.transition(this, Quit(playerId))

  def start(): Try[Game] = state.transition(this, Start)

  def abort(): Try[Game] = state.transition(this, Abort)

  def deal(): Try[Game] = state.transition(this, Deal)

  def playCard(playerId: UUID, card: Card): Try[Game] = state.transition(this, PlayCard(playerId, card))

  def withPlayerName(playerId: UUID, name: String): Game =
    copy(playerNames = playerNames + (playerId -> name))

  def withoutPlayerName(playerId: UUID): Game =
    copy(playerNames = playerNames - playerId)
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
  
  def getPower(card: Card): Int = rankPower(card.rank)

  def canBeat(current: Card, previous: Card): Boolean = {
    rankPower(current.rank) > rankPower(previous.rank)
  }
}
