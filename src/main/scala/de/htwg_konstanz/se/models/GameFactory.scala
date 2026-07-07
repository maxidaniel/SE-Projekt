package de.htwg_konstanz.se.models

object GameFactory {
  def create(players: Seq[IPlayer]): Game = {
    Game(
      players.map(p => p -> Vector.empty).toMap,
      Vector.empty,
      GameState.WaitingForPlayers
    )
  }
}

object DeckFactory {
  def standardDeck(): Deck = Deck(Card.standardDeckCards)

  def shuffledStandardDeck(): Deck = Deck(Card.standardDeckCards).shuffle()

  def customDeck(cards: Vector[Card]): Deck = Deck(cards)
}
