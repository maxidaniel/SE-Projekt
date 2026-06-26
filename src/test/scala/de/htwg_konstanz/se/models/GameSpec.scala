package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.models.GameState.{Ended, Playing}
import de.htwg_konstanz.se.models.PlayingState
import org.scalatest.TryValues.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

class GameSpec extends AnyWordSpec {
  private def makeGame(playerHands: Map[IPlayer, Vector[Card]], playedCards: Vector[Card] = Vector.empty, state: GameState = Playing, currentPlayer: Option[IPlayer] = None, trickCount: Int = 0, trickRank: Option[CardRank] = None, trickLeader: Option[IPlayer] = None): Game = {
    Game(playerHands, playedCards, state, currentPlayer, trickCount, trickRank, trickLeader)
  }

  private def makePlayingGame(playerHands: Map[IPlayer, Vector[Card]], trickRank: CardRank, trickLeader: IPlayer, currentPlayer: IPlayer = null, playedCards: Vector[Card] = Vector.empty): Game = {
    val cp = if (currentPlayer != null) currentPlayer else trickLeader
    Game(playerHands, playedCards, PlayingState, Some(cp), 1, Some(trickRank), Some(trickLeader))
  }

  private def makeController(game: Game): GameController = new GameController(game)

  "A game" should {
    val alice = HumanPlayer("Alice")
    val bob = HumanPlayer("Bob")
    val charlie = HumanPlayer("Charlie")

    "be empty by default" in {
      val game = new Game()
      game.playerHands should be(Map.empty)
      game.playedCards should be(Vector.empty)
      game.state should be(GameState.WaitingForPlayers)
    }

    "add a player on join" in {
      val game = new Game()

      val result = game.join(alice).success.value

      result.playerHands.keySet should contain(alice)
      result.playerHands(alice) should be(Vector.empty)
    }

    "remove a player on leave" in {
      val game = new Game().join(alice).get

      val result = game.quit(alice)
      result.isSuccess should be(true)
      result.isFailure should be(false)

      val afterLeave = result.get
      afterLeave.playerHands.keySet should not contain alice
    }

    "not join a player that is already part of the game" in {
      val game = new Game().join(alice).success.value
      game.join(alice).isFailure should be(true)
    }

    "not quit a player that is not part of the game" in {
      val game = new Game()
      game.quit(alice).isFailure should be(true)
    }

    "not quit a player when the game is running" in {
      val game = Game(Map(alice -> Vector.empty, bob -> Vector.empty), Vector.empty, Playing)
      game.quit(alice).isFailure should be(true)
    }

    "not add a player on join when playing" in {
      val game = new Game().copy(state = Playing)

      val result = game.join(alice)
      result.isSuccess should be(false)
      result.isFailure should be(true)

      val throwable = result.failed.get
      throwable.getMessage should be("Cannot join a running game.")
    }

    "start when waiting and at least two players exist" in {
      val game = Game(Map(alice -> Vector.empty, bob -> Vector.empty), Vector.empty, GameState.WaitingForPlayers)

      val started = game.start().success.value
      started.state should be(Playing)
      started.playerHands.values.map(_.size).sum should be(Card.standardDeckCards.size)
    }

    "not start when fewer than two players exist" in {
      val game = Game(Map(alice -> Vector.empty), Vector.empty, GameState.WaitingForPlayers)
      game.start().failure.exception should have message "Can only start a new game with two or more players."
    }

    "not start when game is not in waiting state" in {
      val game = Game(Map.empty, Vector.empty, Playing)
      game.start().failure.exception should have message "Can only start a new game when in lobby."
    }

    "deal cards as evenly as possible" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty),
        Vector.empty,
        GameState.WaitingForPlayers,
      )

      val dealt = game.deal().success.value
      val handSizes = dealt.playerHands.values.map(_.size).toVector

      handSizes.sum should be(Card.standardDeckCards.size)
      handSizes.max - handSizes.min should be <= 1
    }

    "not deal cards in playing state" in {
      val game = Game(Map(alice -> Vector.empty, bob -> Vector.empty), Vector.empty, Playing)

      game.deal().failure.exception should have message "Can only deal cards before the game starts."
    }

    "allow a player to play a valid first card" in {
      val playedCard = Card.ThreeOfHearts
      val game = Game(
        Map(
          alice -> Vector(playedCard, Card.KingOfHearts),
          bob -> Vector(Card.FiveOfClubs),
        ),
        Vector.empty,
        Playing,
        currentPlayer = Some(alice)
      )

      val afterPlay = game.playCard(alice, playedCard).success.value

      afterPlay.playedCards.last should be(playedCard)
      afterPlay.playerHands(alice) should contain(Card.KingOfHearts)
      afterPlay.playerHands(alice) should not contain playedCard
    }

    "reject a played card with wrong rank" in {
      val game = makePlayingGame(
        Map(alice -> Vector(Card.FiveOfHearts), bob -> Vector(Card.TenOfClubs)),
        CardRank.Ten,
        alice,
        playedCards = Vector(Card.TenOfHearts)
      )

      game.playCard(alice, Card.FiveOfHearts).failure.exception should have message "Must play cards of rank Ten, not Five."
    }

    "end the game when a player plays the last card in hand" in {
      val winningCard = Card.AceOfSpades
      val game = Game(
        Map(alice -> Vector(winningCard), bob -> Vector(Card.KingOfHearts)),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None
      )

      val ended = game.playCard(alice, winningCard).success.value
      ended.state should be(Ended)
      ended.playerHands(alice) should be(Vector.empty)
    }

    "reject playing a card that the player does not have" in {
      val game = Game(
        Map(
          alice -> Vector(Card.FourOfHearts),
          bob -> Vector(Card.QueenOfClubs),
        ),
        Vector.empty,
        Playing,
      )

      game.playCard(alice, Card.AceOfClubs).failure.exception.getMessage should include("does not have card")
    }

    "reject playing an unknown card" in {
      val game = Game(
        Map(alice -> Vector(Card.FourOfHearts)),
        Vector.empty,
        Playing,
      )
      game.playCard(alice, Card.Unknown).isFailure should be(true)
    }

    "reject playing a card when player is not in the game" in {
      val game = Game(
        Map(alice -> Vector(Card.FourOfHearts)),
        Vector.empty,
        Playing,
      )
      game.playCard(bob, Card.FiveOfHearts).failure.exception.getMessage should include(bob.id.toString)
    }

    "not deal cards when fewer than two players" in {
      val game = Game(Map(alice -> Vector.empty), Vector.empty, GameState.WaitingForPlayers)
      game.deal().failure.exception should have message "Can only deal cards when two or more players are in the game."
    }
  }

  "Game.getPower" should {
    "return correct power for all card ranks" in {
      Game.getPower(Card.ThreeOfHearts) should be(1)
      Game.getPower(Card.FourOfHearts) should be(2)
      Game.getPower(Card.FiveOfHearts) should be(3)
      Game.getPower(Card.SixOfHearts) should be(4)
      Game.getPower(Card.SevenOfHearts) should be(5)
      Game.getPower(Card.EightOfHearts) should be(6)
      Game.getPower(Card.NineOfHearts) should be(7)
      Game.getPower(Card.TenOfHearts) should be(8)
      Game.getPower(Card.JackOfHearts) should be(9)
      Game.getPower(Card.QueenOfHearts) should be(10)
      Game.getPower(Card.KingOfHearts) should be(11)
      Game.getPower(Card.AceOfHearts) should be(12)
      Game.getPower(Card.TwoOfHearts) should be(13)
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
