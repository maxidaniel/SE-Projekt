package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class GameSpec extends AnyWordSpec {
  "A game" should {
    "be empty by default" in {
      val game = new Game()
      game.playerHands should be(Map.empty)
      game.playedCards should be(Vector.empty)
    }
  }

  "A deck" should {
    "contain the full standard deck by default" in {
      val deck = new Deck()
      deck.cards should have size Card.standardDeckCards.size
      deck.cards.toSet should be(Card.standardDeckCards.toSet)
    }

    "shuffle while preserving cards" in {
      val deck = new Deck()
      val shuffled = deck.shuffle()
      shuffled.cards should have size deck.cards.size
      shuffled.cards.toSet should be(deck.cards.toSet)
    }
  }
}
