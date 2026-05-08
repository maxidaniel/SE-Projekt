package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState.Playing
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class GameSpec extends AnyWordSpec {
  "A game" should {
    "be empty by default" in {
      val game = new Game()
      game.playerHands should be(Map.empty)
      game.playedCards should be(Vector.empty)
      game.state should be(GameState.WaitingForPlayers)
    }

    "add a player on join" in {
      val game = new Game()
      val playerId = UUID.randomUUID()

      val joined = game.join(playerId)
      joined.playerHands.keySet should contain(playerId)
      joined.playerHands(playerId) should be(Vector.empty)
    }

    "remove a player on leave" in {
      val playerId = UUID.randomUUID()
      val game = new Game().join(playerId)

      val afterLeave = game.leave(playerId)
      afterLeave.playerHands.keySet should not contain playerId
    }

    "not add a player on join when playing" in {
      val game = new Game().copy(state = Playing)
      val playerId = UUID.randomUUID()

      val afterJoin = game.join(playerId)
      afterJoin.playerHands.keySet should not contain playerId
    }

    "start when waiting and at least two players exist" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(Map(p1 -> Vector.empty, p2 -> Vector.empty), Vector.empty, GameState.WaitingForPlayers)

      game.start().state should be(Playing)
    }

    "not start when fewer than two players exist" in {
      val p1 = UUID.randomUUID()
      val game = Game(Map(p1 -> Vector.empty), Vector.empty, GameState.WaitingForPlayers)

      game.start().state should be(GameState.WaitingForPlayers)
    }

    "not start when game is not in waiting state" in {
      val game = Game(Map.empty, Vector.empty, Playing)
      game.start() should be(game)
    }

    "return itself for deal and playCard placeholders" in {
      val game = new Game()
      game.deal() should be(game)
      game.playCard() should be(game)
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
