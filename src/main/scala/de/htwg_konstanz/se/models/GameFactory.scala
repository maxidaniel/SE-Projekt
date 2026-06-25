package de.htwg_konstanz.se.models

import java.util.UUID

object GameFactory {
  def create(playerNames: Seq[String]): Game = {
    val playerIds = playerNames.map(_ -> UUID.randomUUID()).toMap
    create(playerIds)
  }

  def create(playerNameIds: Map[String, UUID]): Game = {
    Game(
      playerNameIds.map { case (_, id) => id -> Vector.empty },
      Vector.empty,
      GameState.WaitingForPlayers,
      playerNameIds.map { case (name, id) => id -> name }
    )
  }

  def createWithDeals(playerNames: Seq[String]): Game = {
    val game = create(playerNames)
    game.start().get
  }
}

object DeckFactory {
  def standardDeck(): Deck = Deck(Card.standardDeckCards)
  def shuffledStandardDeck(): Deck = Deck(Card.standardDeckCards).shuffle()
  def customDeck(cards: Vector[Card]): Deck = Deck(cards)
}
