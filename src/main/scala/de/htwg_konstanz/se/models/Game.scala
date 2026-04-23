package de.htwg_konstanz.se.models

import java.util.UUID

case class Game(playerHands: Map[UUID, Vector[Card]], playedCards: Vector[Card]) {
  def this() = {
    this(Map.empty, Vector.empty)
  }
}

case class Deck(cards: Vector[Card]) {
  def this() = {
    this(Card.standardDeckCards)
  }

  def shuffle(): Deck = {
    copy(cards = scala.util.Random.shuffle(cards))
  }
}