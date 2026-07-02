package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}

import scala.util.Random
import com.google.inject.Inject

case class PlayRandomCardStrategy() extends IStrategy {
  override def name = "Random card strategy"

  override def canPlay(cards: Vector[Card], lastPlayed: Card, playedCards: Vector[Card] = Vector.empty): Boolean =
    cards.exists(c => Game.canBeat(c, lastPlayed))

  override def play(cards: Vector[Card], lastPlayed: Card, playedCards: Vector[Card] = Vector.empty): Card = {
    val filtered = cards.filter(c => Game.canBeat(c, lastPlayed))
    val next = Random.nextInt(filtered.length)
    filtered(next)
  }

  override def shouldAcceptExchange(hand: Vector[Card], offeredCards: Vector[Card], position: String): Boolean =
    Random.nextBoolean()
}
