package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.Card

trait IStrategy {
  def name: String
  def play(cards: Vector[Card], lastPlayed: Card): Card
}
