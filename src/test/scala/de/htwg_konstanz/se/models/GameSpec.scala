package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState.{Ended, Playing}
import org.scalatest.TryValues.*
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

      val result = game.join(playerId).success.value
      
      result.playerHands.keySet should contain(playerId)
      result.playerHands(playerId) should be(Vector.empty)
    }

    "remove a player on leave" in {
      val playerId = UUID.randomUUID()
      val game = new Game().join(playerId).get

      val result = game.quit(playerId)
      result.isSuccess should be(true)
      result.isFailure should be(false)

      val afterLeave = result.get
      afterLeave.playerHands.keySet should not contain playerId
    }

    "not join a player that is already part of the game" in {
      val p1 = UUID.randomUUID()
      val game = new Game().join(p1).success.value
      game.join(p1).isFailure should be(true)
    }

    "not quit a player that is not part of the game" in {
      val p1 = UUID.randomUUID()
      val game = new Game()
      game.quit(p1).isFailure should be(true)
    }

    "not quit a player when the game is running" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(Map(p1 -> Vector.empty, p2 -> Vector.empty), Vector.empty, Playing)
      game.quit(p1).isFailure should be(true)
    }

    "not add a player on join when playing" in {
      val game = new Game().copy(state = Playing)
      val playerId = UUID.randomUUID()

      val result = game.join(playerId)
      result.isSuccess should be(false)
      result.isFailure should be(true)

      val throwable = result.failed.get
      throwable.getMessage should be("Cannot join a running game.")
    }

    "start when waiting and at least two players exist" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(Map(p1 -> Vector.empty, p2 -> Vector.empty), Vector.empty, GameState.WaitingForPlayers)

      val started = game.start().success.value
      started.state should be(Playing)
      started.playerHands.values.map(_.size).sum should be(Card.standardDeckCards.size)
    }

    "not start when fewer than two players exist" in {
      val p1 = UUID.randomUUID()
      val game = Game(Map(p1 -> Vector.empty), Vector.empty, GameState.WaitingForPlayers)
      game.start().failure.exception should have message "Can only start a new game with two or more players."
    }

    "not start when game is not in waiting state" in {
      val game = Game(Map.empty, Vector.empty, Playing)
      game.start().failure.exception should have message "Can only start a new game when in lobby."
    }

    "deal cards as evenly as possible" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val p3 = UUID.randomUUID()
      val game = Game(
        Map(p1 -> Vector.empty, p2 -> Vector.empty, p3 -> Vector.empty),
        Vector.empty,
        GameState.WaitingForPlayers,
      )

      val dealt = game.deal().success.value
      val handSizes = dealt.playerHands.values.map(_.size).toVector

      handSizes.sum should be(Card.standardDeckCards.size)
      handSizes.max - handSizes.min should be <= 1
    }

    "not deal cards in playing state" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(Map(p1 -> Vector.empty, p2 -> Vector.empty), Vector.empty, Playing)

      game.deal().failure.exception should have message "Can only deal cards before the game starts."
    }

    "allow a player to play a valid first card" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val playedCard = Card.ThreeOfHearts
      val game = Game(
        Map(
          p1 -> Vector(playedCard, Card.KingOfHearts),
          p2 -> Vector(Card.FiveOfClubs),
        ),
        Vector.empty,
        Playing,
      )

      val afterPlay = game.playCard(p1, playedCard).success.value

      afterPlay.playedCards.last should be(playedCard)
      afterPlay.playerHands(p1) should contain(Card.KingOfHearts)
      afterPlay.playerHands(p1) should not contain playedCard
    }

    "reject a played card that does not outrank the previous card" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(
        Map(
          p1 -> Vector(Card.FiveOfHearts),
          p2 -> Vector(Card.TenOfClubs),
        ),
        Vector(Card.TenOfHearts),
        Playing,
      )

      game.playCard(p1, Card.FiveOfHearts).failure.exception should have message "Played card must outrank the previous card."
    }

    "end the game when a player plays the last card in hand" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val winningCard = Card.AceOfSpades
      val game = Game(
        Map(
          p1 -> Vector(winningCard),
          p2 -> Vector(Card.KingOfHearts),
        ),
        Vector(Card.KingOfDiamonds),
        Playing,
      )

      val ended = game.playCard(p1, winningCard).success.value
      ended.state should be(Ended)
      ended.playerHands(p1) should be(Vector.empty)
    }

    "reject playing a card that the player does not have" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(
        Map(
          p1 -> Vector(Card.FourOfHearts),
          p2 -> Vector(Card.QueenOfClubs),
        ),
        Vector.empty,
        Playing,
      )

      game.playCard(p1, Card.AceOfClubs).failure.exception.getMessage should include("does not have card")
    }

    "reject playing an unknown card" in {
      val p1 = UUID.randomUUID()
      val game = Game(
        Map(p1 -> Vector(Card.FourOfHearts)),
        Vector.empty,
        Playing,
      )
      game.playCard(p1, Card.Unknown).isFailure should be(true)
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
