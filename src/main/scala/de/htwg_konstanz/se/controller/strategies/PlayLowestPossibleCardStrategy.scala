package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}
import com.google.inject.Inject

case class PlayLowestPossibleCardStrategy() extends IStrategy {

  override def name: String = "Lowest possible card strategy"

  override def canPlay(cards: Vector[Card], lastPlayed: Card, playedCards: Vector[Card] = Vector.empty): Boolean =
    cards.exists(c => Game.canBeat(c, lastPlayed))

  override def play(cards: Vector[Card], lastPlayed: Card, playedCards: Vector[Card] = Vector.empty): Card = {
    val filtered = cards.filter(c => Game.canBeat(c, lastPlayed))
    val ordered = filtered.sortBy(c => Game.getPower(c))
    ordered.head
  }

  override def shouldAcceptExchange(hand: Vector[Card], offeredCards: Vector[Card], position: String): Boolean = true
}
