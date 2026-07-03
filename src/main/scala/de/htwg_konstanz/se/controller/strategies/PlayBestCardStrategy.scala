package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}

case class PlayBestCardStrategy() extends IStrategy {

  override def name: String = "Best play strategy"

  override def canPlay(
      cards: Vector[Card],
      lastPlayed: Option[Card],
      playedCards: Vector[Card] = Vector.empty
  ): Boolean =
    lastPlayed match
      case Some(last) => cards.exists(c => Game.canBeat(c, last))
      case None       => cards.nonEmpty

  override def play(
      cards: Vector[Card],
      lastPlayed: Option[Card],
      playedCards: Vector[Card] = Vector.empty
  ): Option[Card] =
    if cards.isEmpty then None
    else
      lastPlayed match
        case None       => Some(cards.minBy(c => -Game.getPower(c)))
        case Some(last) =>
          val filtered = cards.filter(c => Game.canBeat(c, last))
          if filtered.isEmpty then None
          else Some(filtered.minBy(c => -Game.getPower(c)))

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
