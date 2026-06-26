package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class GameFactorySpec extends AnyWordSpec {
  "A GameFactory" should {
    "create a game with Alice and Bob" should {
      val game = GameFactory.create(Seq(HumanPlayer("Alice"), HumanPlayer("Bob")))
      game should not be null

      "and have size 2" in {
        game.playerHands.size should be(2)
      }

      "and should have state WaitingForPlayers" in {
        game.state should be(GameState.WaitingForPlayers)
      }

      "and create a game with empty hands" in {
        game.playerHands.values.foreach(_.size should be(0))
      }

      "and have no played cards" in {
        game.playedCards should be(empty)
      }

      "and have unique player ids" in {
        val alice = game.playerHands.keySet.toSeq.head
        val bob = game.playerHands.keySet.toSeq(1)
        alice.id should not be equal(bob.id)
      }
    }

    "create a game without any players given empty Seq" in {
      val game = GameFactory.create(Seq.empty)
      game.playerHands should be(empty)
    }
  }

  "DeckFactory" should {
    "create a standard deck" in {
      val deck = DeckFactory.standardDeck()
      deck.cards.size should be(Card.standardDeckCards.size)
      deck.cards.toSet should be(Card.standardDeckCards.toSet)
    }

    "create a shuffled standard deck" in {
      val deck = DeckFactory.shuffledStandardDeck()
      deck.cards.size should be(Card.standardDeckCards.size)
      deck.cards.toSet should be(Card.standardDeckCards.toSet)
    }

    "create a custom deck" in {
      val customCards = Vector(Card.ThreeOfHearts, Card.FourOfHearts, Card.FiveOfHearts)
      val deck = DeckFactory.customDeck(customCards)
      deck.cards should be(customCards)
    }

    "create a custom deck with empty cards" in {
      val deck = DeckFactory.customDeck(Vector.empty)
      deck.cards should be(Vector.empty)
    }
  }
}
