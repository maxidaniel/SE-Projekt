package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState.*

import java.util.UUID
import scala.util.Try

trait IGame {
  def join(player: IPlayer): Try[IGame]
  def quit(player: IPlayer): Try[IGame]
  def start(): Try[IGame]
  def abort(): Try[IGame]
  def deal(): Try[IGame]
  def playCard(playerId: IPlayer, card: Card): Try[IGame]
  def passTrick(playerId: IPlayer): Try[IGame]
}

case class Game(
  playerHands: Map[IPlayer, Vector[Card]],
  playedCards: Vector[Card],
  state: GameState,
  currentPlayer: Option[IPlayer] = None,
  trickCount: Int = 0,
  trickRank: Option[CardRank] = None,
  trickLeader: Option[IPlayer] = None
) extends IGame {
  def this() = {
    this(Map.empty, Vector.empty, WaitingForPlayers, None, 0, None, None)
  }

  def join(player: IPlayer): Try[Game] = state.transition(this, Join(player))

  def quit(player: IPlayer): Try[Game] = state.transition(this, Quit(player))

  def start(): Try[Game] = state.transition(this, Start)

  def abort(): Try[Game] = state.transition(this, Abort)

  def deal(): Try[Game] = state.transition(this, Deal)

  def playCard(player: IPlayer, card: Card): Try[Game] = state.transition(this, PlayCard(player, card))

  def passTrick(player: IPlayer): Try[Game] = state.transition(this, PassTrick(player))
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
    rankPower(current.rank) >= rankPower(previous.rank)
  }
}
