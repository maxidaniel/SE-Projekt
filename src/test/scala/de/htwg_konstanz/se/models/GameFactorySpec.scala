package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class GameFactorySpec extends AnyWordSpec {
  "GameFactory.create" should {
    "create a game with the correct number of players" in {
      val game = GameFactory.create(Seq("Alice", "Bob", "Charlie"))
      game.playerHands.size should be(3)
    }

    "create a game in WaitingForPlayers state" in {
      val game = GameFactory.create(Seq("Alice", "Bob"))
      game.state should be(GameState.WaitingForPlayers)
    }

    "create a game with empty hands" in {
      val game = GameFactory.create(Seq("Alice", "Bob"))
      game.playerHands.values.foreach(_.size should be(0))
    }

    "create a game with empty played cards" in {
      val game = GameFactory.create(Seq("Alice", "Bob"))
      game.playedCards should be(Vector.empty)
    }

    "create a game with unique player ids" in {
      val game = GameFactory.create(Seq("Alice", "Bob"))
      game.playerHands.keySet.size should be(2)
    }

    "create a game with no players given empty seq" in {
      val game = GameFactory.create(Seq.empty)
      game.playerHands should be(Map.empty)
    }

    "preserve given player ids when using map overload" in {
      val aliceId = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val bobId = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val game = GameFactory.create(Map("Alice" -> aliceId, "Bob" -> bobId))
      game.playerHands.keySet should contain(aliceId)
      game.playerHands.keySet should contain(bobId)
      game.playerHands.size should be(2)
    }
  }

  "GameFactory.createWithDeals" should {
    "create a game with dealt cards" in {
      val game = GameFactory.createWithDeals(Seq("Alice", "Bob"))
      game.playerHands.values.map(_.size).sum should be(Card.standardDeckCards.size)
    }

    "create a game in Playing state after dealing" in {
      val game = GameFactory.createWithDeals(Seq("Alice", "Bob"))
      game.state should be(GameState.Playing)
    }

    "deal cards evenly among players" in {
      val game = GameFactory.createWithDeals(Seq("Alice", "Bob", "Charlie"))
      val handSizes = game.playerHands.values.map(_.size).toVector
      handSizes.max - handSizes.min should be <= 1
    }

    "create a game with empty played cards" in {
      val game = GameFactory.createWithDeals(Seq("Alice", "Bob"))
      game.playedCards should be(Vector.empty)
    }

    "create a game with all standard cards" in {
      val game = GameFactory.createWithDeals(Seq("Alice", "Bob"))
      game.playerHands.values.flatten.toSet should be(Card.standardDeckCards.toSet)
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
