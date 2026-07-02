package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}

import scala.util.Random
import com.google.inject.Inject

case class PlayRandomCardStrategy() extends IStrategy {
  override def name = "Random card strategy"

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
        case None       => Some(cards(Random.nextInt(cards.length)))
        case Some(last) =>
          val filtered = cards.filter(c => Game.canBeat(c, last))
          if filtered.isEmpty then None
          else Some(filtered(Random.nextInt(filtered.length)))

  override def shouldAcceptExchange(hand: Vector[Card], offeredCards: Vector[Card], position: String): Boolean =
    Random.nextBoolean()
}
