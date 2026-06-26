package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec


import java.util.UUID

class GameStateSpec extends AnyWordSpec {
  "GameState" should {
    "expose waiting-for-players as a valid state" in {
      GameState.values should contain(GameState.WaitingForPlayers)
    }

    "expose playing as a valid state" in {
      GameState.values should contain(GameState.Playing)
    }

    "default to waiting-for-players in new games" in {
      new Game().state should be(GameState.WaitingForPlayers)
    }
  }

  "WaitingForPlayersState" should {
    "allow join" in {
      WaitingForPlayersState.canJoin should be(true)
    }

    "allow quit" in {
      WaitingForPlayersState.canQuit should be(true)
    }

    "allow start" in {
      WaitingForPlayersState.canStart should be(true)
    }

    "not allow abort" in {
      WaitingForPlayersState.canAbort should be(false)
    }

    "allow deal" in {
      WaitingForPlayersState.canDeal should be(true)
    }

    "not allow playCard" in {
      WaitingForPlayersState.canPlayCard should be(false)
    }

    "transition join successfully" in {
      val game = new Game()
      val playerId = UUID.randomUUID()
      WaitingForPlayersState.transition(game, Join(playerId)).isSuccess should be(true)
    }

    "transition quit successfully" in {
      val playerId = UUID.randomUUID()
      val game = new Game().join(playerId).get
      WaitingForPlayersState.transition(game, Quit(playerId)).isSuccess should be(true)
    }

    "transition start successfully with 2+ players" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(Map(p1 -> Vector.empty, p2 -> Vector.empty), Vector.empty, WaitingForPlayersState)
      WaitingForPlayersState.transition(game, Start).isSuccess should be(true)
    }

    "transition start fail with fewer than 2 players" in {
      val game = Game(Map(UUID.randomUUID() -> Vector.empty), Vector.empty, WaitingForPlayersState)
      WaitingForPlayersState.transition(game, Start).isFailure should be(true)
    }

    "transition deal successfully with 2+ players" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(Map(p1 -> Vector.empty, p2 -> Vector.empty), Vector.empty, WaitingForPlayersState)
      WaitingForPlayersState.transition(game, Deal).isSuccess should be(true)
    }

    "transition deal fail with fewer than 2 players" in {
      val game = Game(Map(UUID.randomUUID() -> Vector.empty), Vector.empty, WaitingForPlayersState)
      WaitingForPlayersState.transition(game, Deal).isFailure should be(true)
    }

    "transition abort fail" in {
      val game = new Game()
      WaitingForPlayersState.transition(game, Abort).isFailure should be(true)
    }

    "transition playCard fail" in {
      val game = new Game()
      WaitingForPlayersState.transition(game, PlayCard(UUID.randomUUID(), Card.ThreeOfHearts)).isFailure should be(true)
    }
  }

  "StartingState" should {
    "not allow join" in {
      StartingState.canJoin should be(false)
    }

    "not allow quit" in {
      StartingState.canQuit should be(false)
    }

    "not allow start" in {
      StartingState.canStart should be(false)
    }

    "not allow abort" in {
      StartingState.canAbort should be(false)
    }

    "not allow deal" in {
      StartingState.canDeal should be(false)
    }

    "not allow playCard" in {
      StartingState.canPlayCard should be(false)
    }

    "transition any operation to failure" in {
      val game = new Game()
      StartingState.transition(game, Join(UUID.randomUUID())).isFailure should be(true)
      StartingState.transition(game, Quit(UUID.randomUUID())).isFailure should be(true)
      StartingState.transition(game, Start).isFailure should be(true)
      StartingState.transition(game, Abort).isFailure should be(true)
      StartingState.transition(game, Deal).isFailure should be(true)
      StartingState.transition(game, PlayCard(UUID.randomUUID(), Card.ThreeOfHearts)).isFailure should be(true)
    }
  }

  "PlayingState" should {
    "not allow join" in {
      PlayingState.canJoin should be(false)
    }

    "not allow quit" in {
      PlayingState.canQuit should be(false)
    }

    "not allow start" in {
      PlayingState.canStart should be(false)
    }

    "allow abort" in {
      PlayingState.canAbort should be(true)
    }

    "not allow deal" in {
      PlayingState.canDeal should be(false)
    }

    "allow playCard" in {
      PlayingState.canPlayCard should be(true)
    }

    "transition join fail" in {
      val game = Game(Map.empty, Vector.empty, PlayingState)
      PlayingState.transition(game, Join(UUID.randomUUID())).isFailure should be(true)
    }

    "transition quit fail" in {
      val game = Game(Map.empty, Vector.empty, PlayingState)
      PlayingState.transition(game, Quit(UUID.randomUUID())).isFailure should be(true)
    }

    "transition start fail" in {
      val game = Game(Map.empty, Vector.empty, PlayingState)
      PlayingState.transition(game, Start).isFailure should be(true)
    }

    "transition abort successfully" in {
      val game = Game(Map.empty, Vector.empty, PlayingState)
      PlayingState.transition(game, Abort).isSuccess should be(true)
    }

    "transition deal fail" in {
      val game = Game(Map.empty, Vector.empty, PlayingState)
      PlayingState.transition(game, Deal).isFailure should be(true)
    }

    "transition playCard with unknown card fail" in {
      val playerId = UUID.randomUUID()
      val game = Game(Map(playerId -> Vector(Card.ThreeOfHearts)), Vector.empty, PlayingState)
      PlayingState.transition(game, PlayCard(playerId, Card.Unknown)).isFailure should be(true)
    }

    "transition playCard with non-existent player fail" in {
      val game = Game(Map(UUID.randomUUID() -> Vector(Card.ThreeOfHearts)), Vector.empty, PlayingState)
      PlayingState.transition(game, PlayCard(UUID.randomUUID(), Card.ThreeOfHearts)).isFailure should be(true)
    }

    "transition playCard with card not in hand fail" in {
      val playerId = UUID.randomUUID()
      val game = Game(Map(playerId -> Vector(Card.KingOfHearts)), Vector.empty, PlayingState)
      PlayingState.transition(game, PlayCard(playerId, Card.ThreeOfHearts)).isFailure should be(true)
    }

    "transition playCard with card that cannot beat previous fail" in {
      val playerId = UUID.randomUUID()
      val game = Game(
        Map(playerId -> Vector(Card.ThreeOfHearts)),
        Vector(Card.KingOfHearts),
        PlayingState,
        Map.empty,
        Some(playerId),
        1,
        Some(CardRank.King),
        Some(playerId)
      )
      PlayingState.transition(game, PlayCard(playerId, Card.ThreeOfHearts)).isFailure should be(true)
    }

    "transition playCard successfully" in {
      val playerId = UUID.randomUUID()
      val game = Game(
        Map(playerId -> Vector(Card.AceOfSpades)),
        Vector(Card.AceOfClubs),
        PlayingState,
        Map.empty,
        Some(playerId),
        1,
        Some(CardRank.Ace),
        Some(playerId)
      )
      PlayingState.transition(game, PlayCard(playerId, Card.AceOfSpades)).isSuccess should be(true)
    }
  }

  "AbortedState" should {
    "not allow join" in {
      AbortedState.canJoin should be(false)
    }

    "not allow quit" in {
      AbortedState.canQuit should be(false)
    }

    "not allow start" in {
      AbortedState.canStart should be(false)
    }

    "not allow abort" in {
      AbortedState.canAbort should be(false)
    }

    "not allow deal" in {
      AbortedState.canDeal should be(false)
    }

    "not allow playCard" in {
      AbortedState.canPlayCard should be(false)
    }

    "transition any operation to failure" in {
      val game = Game(Map.empty, Vector.empty, AbortedState)
      AbortedState.transition(game, Join(UUID.randomUUID())).isFailure should be(true)
      AbortedState.transition(game, Quit(UUID.randomUUID())).isFailure should be(true)
      AbortedState.transition(game, Start).isFailure should be(true)
      AbortedState.transition(game, Abort).isFailure should be(true)
      AbortedState.transition(game, Deal).isFailure should be(true)
      AbortedState.transition(game, PlayCard(UUID.randomUUID(), Card.ThreeOfHearts)).isFailure should be(true)
    }
  }

  "EndedState" should {
    "not allow join" in {
      EndedState.canJoin should be(false)
    }

    "not allow quit" in {
      EndedState.canQuit should be(false)
    }

    "not allow start" in {
      EndedState.canStart should be(false)
    }

    "not allow abort" in {
      EndedState.canAbort should be(false)
    }

    "not allow deal" in {
      EndedState.canDeal should be(false)
    }

    "not allow playCard" in {
      EndedState.canPlayCard should be(false)
    }

    "transition any operation to failure" in {
      val game = Game(Map.empty, Vector.empty, EndedState)
      EndedState.transition(game, Join(UUID.randomUUID())).isFailure should be(true)
      EndedState.transition(game, Quit(UUID.randomUUID())).isFailure should be(true)
      EndedState.transition(game, Start).isFailure should be(true)
      EndedState.transition(game, Abort).isFailure should be(true)
      EndedState.transition(game, Deal).isFailure should be(true)
      EndedState.transition(game, PlayCard(UUID.randomUUID(), Card.ThreeOfHearts)).isFailure should be(true)
    }
  }
}
