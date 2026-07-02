package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}

case class PlayBestCardStrategy() extends IStrategy {

  override def name: String = "Best play strategy"

  override def canPlay(cards: Vector[Card], lastPlayed: Card, playedCards: Vector[Card] = Vector.empty): Boolean =
    cards.exists(c => Game.canBeat(c, lastPlayed))

  override def play(cards: Vector[Card], lastPlayed: Card, playedCards: Vector[Card] = Vector.empty): Card = {
    val filtered = cards.filter(c => Game.canBeat(c, lastPlayed))
    val ordered = filtered.sortBy(c => -Game.getPower(c))
    ordered.head
  }

  override def shouldAcceptExchange(hand: Vector[Card], offeredCards: Vector[Card], position: String): Boolean = {
    if offeredCards.isEmpty || hand.isEmpty then return false
    val avgOfferedPower = offeredCards.map(Game.getPower).sum.toDouble / offeredCards.size
    val avgHandPower = hand.map(Game.getPower).sum.toDouble / hand.size
    position match {
      case "president" | "vice_president" =>
        avgOfferedPower > avgHandPower
      case "scum" | "vice_scum" =>
        avgOfferedPower > avgHandPower
      case _ =>
        avgOfferedPower > avgHandPower
    }
  }
}
