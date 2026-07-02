package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState.*

import java.util.UUID
import scala.util.Try

case class Game(
  playerHands: Map[IPlayer, Vector[Card]],
  playedCards: Vector[Card],
  state: GameState,
  currentPlayer: Option[IPlayer] = None,
  trickCount: Int = 0,
  trickRank: Option[CardRank] = None,
  trickLeader: Option[IPlayer] = None,
  passedPlayers: Set[IPlayer] = Set.empty,
  scoredRanks: Map[IPlayer, Int] = Map.empty,
  roundNumber: Int = 1,
  finishOrder: Vector[IPlayer] = Vector.empty
) {
  def this() = {
    this(Map.empty, Vector.empty, WaitingForPlayers, None, 0, None, None, Set.empty, Map.empty, 1, Vector.empty)
  }

  def join(player: IPlayer): Try[Game] = state.transition(this, Join(player))

  def quit(player: IPlayer): Try[Game] = state.transition(this, Quit(player))

  def start(): Try[Game] = state.transition(this, Start)

  def abort(): Try[Game] = state.transition(this, Abort)

  def deal(): Try[Game] = state.transition(this, Deal)

  def playCard(player: IPlayer, card: Card): Try[Game] = state.transition(this, PlayCard(player, card))

  def passTrick(player: IPlayer): Try[Game] = state.transition(this, PassTrick(player))

  def nextRound(): Try[Game] = state.transition(this, NextRound)
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

  def getRankPower(rank: CardRank): Int = rankPower(rank)

  def canBeat(current: Card, previous: Card): Boolean = {
    rankPower(current.rank) > rankPower(previous.rank)
  }

  def isBurnCard(card: Card): Boolean = card.rank == CardRank.Two

  def isFourOfAKind(cards: Seq[Card]): Boolean = {
    cards.length == 4 && cards.forall(_.rank == cards.head.rank)
  }

  val PresidentScore = 2
  val VicePresidentScore = 1
  val OtherScore = 0

  def scoreForPosition(position: Int, totalPlayers: Int): Int = position match {
    case 0 => PresidentScore
    case 1 => VicePresidentScore
    case _ => OtherScore
  }

  def getBestCards(hand: Vector[Card], count: Int): Vector[Card] = {
    hand.sortBy(c => -getPower(c)).take(count)
  }

  def getWorstCards(hand: Vector[Card], count: Int): Vector[Card] = {
    hand.sortBy(c => getPower(c)).take(count)
  }

  def exchangeCards(president: IPlayer, scum: IPlayer, vicePresident: Option[IPlayer], viceScum: Option[IPlayer], playerHands: Map[IPlayer, Vector[Card]]): Map[IPlayer, Vector[Card]] = {
    var hands = playerHands
    
    val scumBest = getBestCards(hands(scum), 2)
    val presWorst = getWorstCards(hands(president), 2)
    hands = hands.updated(president, hands(president).filterNot(presWorst.contains) ++ scumBest)
    hands = hands.updated(scum, hands(scum).filterNot(scumBest.contains) ++ presWorst)
    
    (vicePresident, viceScum) match {
      case (Some(vp), Some(vscum)) =>
        val vscumBest = getBestCards(hands(vscum), 1)
        val vpWorst = getWorstCards(hands(vp), 1)
        hands = hands.updated(vp, hands(vp).filterNot(vpWorst.contains) ++ vscumBest)
        hands = hands.updated(vscum, hands(vscum).filterNot(vscumBest.contains) ++ vpWorst)
      case _ =>
    }
    
    hands
  }
}
