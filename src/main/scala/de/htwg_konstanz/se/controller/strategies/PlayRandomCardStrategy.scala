package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}

import scala.util.Random

case class PlayRandomCardStrategy() extends IStrategy {
  override def name = "Random card strategy"

  override def play(cards: Vector[Card], lastPlayed: Card): Card = {
    val filtered = cards.filter(c => Game.canBeat(c, lastPlayed))
    val next = Random.nextInt(filtered.length)
    filtered(next)
  }
}
