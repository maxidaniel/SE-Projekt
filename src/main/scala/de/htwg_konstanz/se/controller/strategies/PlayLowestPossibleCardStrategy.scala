package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}
import com.google.inject.Inject

case class PlayLowestPossibleCardStrategy() extends IStrategy {

  override def name: String = "Lowest possible card strategy"

  override def play(cards: Vector[Card], lastPlayed: Card): Card = {
    val filtered = cards.filter(c => Game.canBeat(c, lastPlayed))
    val ordered = cards.sortBy(c => Game.getPower(c))
    ordered.head
  }
}
