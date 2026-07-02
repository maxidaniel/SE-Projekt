package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}
import com.google.inject.Inject

case class PlayLowestPossibleCardStrategy() extends IStrategy {

  override def name: String = "Lowest possible card strategy"

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
        case None       => Some(cards.sortBy(c => Game.getPower(c)).head)
        case Some(last) =>
          val filtered = cards.filter(c => Game.canBeat(c, last))
          if filtered.isEmpty then None
          else Some(filtered.sortBy(c => Game.getPower(c)).head)

  override def shouldAcceptExchange(hand: Vector[Card], offeredCards: Vector[Card], position: String): Boolean = true
}
