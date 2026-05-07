package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState.WaitingForPlayers

import java.util.UUID

case class Game(playerHands: Map[UUID, Vector[Card]], playedCards: Vector[Card], state: GameState) {
  def this() = {
    this(Map.empty, Vector.empty, state = WaitingForPlayers)
  }

  def join(playerId: UUID): Game = {
    // Only allow players to join outside of a running game
    if state != WaitingForPlayers then return this

    this.copy(playerHands = playerHands + (playerId -> Vector()))
  }

  def leave(playerId: UUID): Game = {
    // TODO: handle cards returning to deck
    this.copy(playerHands = playerHands - playerId)
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