package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState._

import java.util.UUID
import scala.util.{Failure, Success, Try}

case class Game(playerHands: Map[UUID, Vector[Card]], playedCards: Vector[Card], state: GameState) {
  def this() = {
    this(Map.empty, Vector.empty, state = WaitingForPlayers)
  }

  def join(playerId: UUID): Try[Game] = state.transition(this, Join(playerId))

  def quit(playerId: UUID): Try[Game] = state.transition(this, Quit(playerId))

  def start(): Try[Game] = state.transition(this, Start)

  def abort(): Try[Game] = state.transition(this, Abort)

  def deal(): Try[Game] = state.transition(this, Deal)

  def playCard(playerId: UUID, card: Card): Try[Game] = state.transition(this, PlayCard(playerId, card))
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

  def canBeat(current: Card, previous: Card): Boolean = {
    rankPower(current.rank) > rankPower(previous.rank)
  }
}
