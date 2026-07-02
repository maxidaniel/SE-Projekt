package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.Card

trait IStrategy {
  def name: String
  def play(cards: Vector[Card], lastPlayed: Card, playedCards: Vector[Card]): Card
  def canPlay(cards: Vector[Card], lastPlayed: Card, playedCards: Vector[Card]): Boolean
  def shouldAcceptExchange(hand: Vector[Card], offeredCards: Vector[Card], position: String): Boolean
}
