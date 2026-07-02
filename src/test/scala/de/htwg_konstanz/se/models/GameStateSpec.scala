package de.htwg_konstanz.se.models

import org.scalatest.TryValues.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

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
    val alice = HumanPlayer("Alice")
    val bob = HumanPlayer("Bob")
    val charlie = HumanPlayer("Charlie")
    val dave = HumanPlayer("Dave")

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

    "not allow passTrick" in {
      WaitingForPlayersState.canPassTrick should be(false)
    }

    "transition join successfully" in {
      val game = new Game()
      WaitingForPlayersState.transition(game, Join(alice)).isSuccess should be(true)
    }

    "transition quit successfully" in {
      val game = new Game().join(alice).get
      WaitingForPlayersState.transition(game, Quit(alice)).isSuccess should be(true)
    }

    "transition start successfully with 4+ players" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty, dave -> Vector.empty),
        Vector.empty,
        WaitingForPlayersState
      )
      WaitingForPlayersState.transition(game, Start).isSuccess should be(true)
    }

    "transition start fail with fewer than 4 players" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty),
        Vector.empty,
        WaitingForPlayersState
      )
      WaitingForPlayersState.transition(game, Start).isFailure should be(true)
    }

    "transition deal successfully with 4+ players" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty, dave -> Vector.empty),
        Vector.empty,
        WaitingForPlayersState
      )
      WaitingForPlayersState.transition(game, Deal).isSuccess should be(true)
    }

    "transition deal fail with fewer than 4 players" in {
      val game = Game(Map(alice -> Vector.empty, bob -> Vector.empty), Vector.empty, WaitingForPlayersState)
      WaitingForPlayersState.transition(game, Deal).isFailure should be(true)
    }

    "transition abort fail" in {
      val game = new Game()
      WaitingForPlayersState.transition(game, Abort).isFailure should be(true)
    }

    "transition playCard fail" in {
      val game = new Game()
      WaitingForPlayersState.transition(game, PlayCard(alice, Card.ThreeOfHearts)).isFailure should be(true)
    }

    "transition PassTrick fail" in {
      val game = new Game()
      WaitingForPlayersState.transition(game, PassTrick(alice)).isFailure should be(true)
    }

    "transition NextRound fail" in {
      val game = new Game()
      WaitingForPlayersState.transition(game, NextRound).isFailure should be(true)
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

    "not allow passTrick" in {
      StartingState.canPassTrick should be(false)
    }

    "transition any operation to failure" in {
      val game = new Game()
      val alice = HumanPlayer("Alice")
      StartingState.transition(game, Join(alice)).isFailure should be(true)
      StartingState.transition(game, Quit(alice)).isFailure should be(true)
      StartingState.transition(game, Start).isFailure should be(true)
      StartingState.transition(game, Abort).isFailure should be(true)
      StartingState.transition(game, Deal).isFailure should be(true)
      StartingState.transition(game, PlayCard(alice, Card.ThreeOfHearts)).isFailure should be(true)
    }
  }

  "PlayingState" should {
    val alice = HumanPlayer("Alice")
    val bob = HumanPlayer("Bob")
    val charlie = HumanPlayer("Charlie")
    val dave = HumanPlayer("Dave")
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
      PlayingState.transition(game, Join(alice)).isFailure should be(true)
    }

    "transition quit fail" in {
      val game = Game(Map.empty, Vector.empty, PlayingState)
      PlayingState.transition(game, Quit(alice)).isFailure should be(true)
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

    "transition NextRound fail" in {
      val game = Game(Map.empty, Vector.empty, PlayingState)
      PlayingState.transition(game, NextRound).isFailure should be(true)
    }

    "transition playCard with unknown card fail" in {
      val game = Game(Map(alice -> Vector(Card.ThreeOfHearts)), Vector.empty, PlayingState)
      PlayingState.transition(game, PlayCard(alice, Card.Unknown)).isFailure should be(true)
    }

    "transition playCard with non-existent player fail" in {
      val game = Game(Map(alice -> Vector(Card.ThreeOfHearts)), Vector.empty, PlayingState)
      PlayingState.transition(game, PlayCard(alice, Card.ThreeOfHearts)).isFailure should be(true)
    }

    "transition playCard with card not in hand fail" in {
      val game = Game(Map(alice -> Vector(Card.KingOfHearts)), Vector.empty, PlayingState)
      PlayingState.transition(game, PlayCard(alice, Card.ThreeOfHearts)).isFailure should be(true)
    }

    "transition playCard with card that cannot beat previous fail" in {
      val game = Game(
        Map(alice -> Vector(Card.ThreeOfHearts)),
        Vector(Card.KingOfHearts),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.King),
        Some(alice)
      )
      PlayingState.transition(game, PlayCard(alice, Card.ThreeOfHearts)).isFailure should be(true)
    }

    "transition playCard successfully" in {
      val game = Game(
        Map(alice -> Vector(Card.TwoOfSpades)),
        Vector(Card.AceOfClubs),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Ace),
        Some(alice)
      )
      PlayingState.transition(game, PlayCard(alice, Card.TwoOfSpades)).isSuccess should be(true)
    }

    "transition PassTrick successfully" in {
      val bob = HumanPlayer("Bob")
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      PlayingState.transition(game, PassTrick(bob)).isSuccess should be(true)
    }

    "transition PassTrick fail when no trick led" in {
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts)),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None
      )
      PlayingState.transition(game, PassTrick(alice)).isFailure should be(true)
    }

    "transition PassTrick fail when trick leader tries to pass" in {
      val bob = HumanPlayer("Bob")
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      PlayingState.transition(game, PassTrick(alice)).isFailure should be(true)
    }

    "passTrick end trick when all others passed" in {
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")
      val dave = HumanPlayer("Dave")
      val game = Game(
        Map(
          alice -> Vector(Card.KingOfHearts),
          bob -> Vector(Card.AceOfSpades),
          charlie -> Vector(Card.QueenOfHearts),
          dave -> Vector(Card.JackOfHearts)
        ),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice),
        Set(charlie, dave)
      )
      val result = PlayingState.transition(game, PassTrick(bob)).success.value
      result.trickCount should be(0)
      result.currentPlayer should be(Some(alice))
      result.passedPlayers should be(Set.empty)
    }

    "passTrick end trick and game over when trick winner has no cards" in {
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")
      val dave = HumanPlayer("Dave")
      val game = Game(
        Map(
          alice -> Vector.empty,
          bob -> Vector(Card.AceOfSpades),
          charlie -> Vector(Card.QueenOfHearts),
          dave -> Vector(Card.JackOfHearts)
        ),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice),
        Set(charlie, dave)
      )
      val result = PlayingState.transition(game, PassTrick(bob)).success.value
      result.state should be(GameState.Ended)
      result.finishOrder should contain(alice)
    }

    "passTrick move to next player when not all passed" in {
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")
      val dave = HumanPlayer("Dave")
      val game = Game(
        Map(
          alice -> Vector(Card.KingOfHearts),
          bob -> Vector(Card.AceOfSpades),
          charlie -> Vector(Card.QueenOfHearts),
          dave -> Vector(Card.JackOfHearts)
        ),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice),
        Set.empty
      )
      val result = PlayingState.transition(game, PassTrick(bob)).success.value
      result.passedPlayers should contain(bob)
      result.currentPlayer should not be Some(bob)
    }

    "leadTrick fail when player does not have the card" in {
      val bob = HumanPlayer("Bob")
      val game = Game(
        Map(
          alice -> Vector(Card.ThreeOfHearts),
          bob -> Vector(Card.FiveOfClubs),
          charlie -> Vector(Card.SixOfHearts),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None
      )
      PlayingState.transition(game, PlayCard(alice, Card.FiveOfClubs)).isFailure should be(true)
    }

    "leadTrick with card of different rank than requested" in {
      val bob = HumanPlayer("Bob")
      val game = Game(
        Map(
          alice -> Vector(Card.ThreeOfHearts, Card.FourOfHearts),
          bob -> Vector(Card.FiveOfClubs),
          charlie -> Vector(Card.SixOfHearts),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None
      )
      PlayingState.transition(game, PlayCard(alice, Card.ThreeOfHearts)).isSuccess should be(true)
    }

    "respondToTrick fail when card not in hand" in {
      val bob = HumanPlayer("Bob")
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      PlayingState.transition(game, PlayCard(bob, Card.ThreeOfHearts)).isFailure should be(true)
    }

    "respondToTrick fail when rank is not higher" in {
      val bob = HumanPlayer("Bob")
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.ThreeOfHearts)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      PlayingState.transition(game, PlayCard(bob, Card.ThreeOfHearts)).isFailure should be(true)
    }

    "respondToTrick fail when rank equals trick rank" in {
      val bob = HumanPlayer("Bob")
      val game = Game(
        Map(alice -> Vector(Card.FiveOfClubs), bob -> Vector(Card.FiveOfHearts)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      PlayingState.transition(game, PlayCard(bob, Card.FiveOfHearts)).isFailure should be(true)
    }

    "respondToTrick fail when card does not outrank last played" in {
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")
      val game = Game(
        Map(
          alice -> Vector(Card.FiveOfClubs),
          bob -> Vector(Card.SixOfHearts),
          charlie -> Vector(Card.SevenOfHearts),
          dave -> Vector(Card.EightOfHearts)
        ),
        Vector(Card.FiveOfClubs, Card.AceOfClubs),
        PlayingState,
        Some(bob),
        2,
        Some(CardRank.Five),
        Some(alice)
      )
      PlayingState.transition(game, PlayCard(bob, Card.SixOfHearts)).isFailure should be(true)
    }
  }

  "AbortedState" should {
    val alice = HumanPlayer("Alice")
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

    "not allow passTrick" in {
      AbortedState.canPassTrick should be(false)
    }

    "transition any operation to failure" in {
      val game = Game(Map.empty, Vector.empty, AbortedState)
      AbortedState.transition(game, Join(alice)).isFailure should be(true)
      AbortedState.transition(game, Quit(alice)).isFailure should be(true)
      AbortedState.transition(game, Start).isFailure should be(true)
      AbortedState.transition(game, Abort).isFailure should be(true)
      AbortedState.transition(game, Deal).isFailure should be(true)
      AbortedState.transition(game, PlayCard(alice, Card.ThreeOfHearts)).isFailure should be(true)
    }
  }

  "EndedState" should {
    val alice = HumanPlayer("Alice")
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

    "not allow passTrick" in {
      EndedState.canPassTrick should be(false)
    }

    "transition any operation to failure" in {
      val game = Game(Map.empty, Vector.empty, EndedState)
      EndedState.transition(game, Join(alice)).isFailure should be(true)
      EndedState.transition(game, Quit(alice)).isFailure should be(true)
      EndedState.transition(game, Start).isFailure should be(true)
      EndedState.transition(game, Abort).isFailure should be(true)
      EndedState.transition(game, Deal).isFailure should be(true)
      EndedState.transition(game, PlayCard(alice, Card.ThreeOfHearts)).isFailure should be(true)
      EndedState.transition(game, PassTrick(alice)).isFailure should be(true)
      EndedState.transition(game, NextRound).isFailure should be(true)
    }

    "fail NextRound with fewer than 4 players" in {
      val bob = HumanPlayer("Bob")
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector(Card.FourOfHearts)),
        Vector.empty,
        GameState.Ended,
        None,
        0,
        None,
        None,
        Set.empty,
        Map.empty,
        1,
        Vector(alice, bob)
      )
      game.nextRound().isFailure should be(true)
    }

    "succeed NextRound without VP/VScum (only 2 finishers)" in {
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")
      val dave = HumanPlayer("Dave")
      val game = Game(
        Map(
          alice -> Vector(Card.FiveOfClubs),
          bob -> Vector(Card.SixOfHearts),
          charlie -> Vector(Card.SevenOfHearts),
          dave -> Vector(Card.EightOfHearts)
        ),
        Vector.empty,
        GameState.Ended,
        None,
        0,
        None,
        None,
        Set.empty,
        Map.empty,
        1,
        Vector(alice, bob)
      )
      game.nextRound().isSuccess should be(true)
    }

    "succeed NextRound with full exchange (4 finishers)" in {
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")
      val dave = HumanPlayer("Dave")
      val game = Game(
        Map(
          alice -> Vector(Card.ThreeOfHearts, Card.FourOfHearts, Card.FiveOfHearts),
          bob -> Vector(Card.KingOfHearts, Card.AceOfHearts, Card.TwoOfHearts),
          charlie -> Vector(Card.SixOfHearts, Card.SevenOfHearts),
          dave -> Vector(Card.TenOfHearts, Card.JackOfHearts)
        ),
        Vector.empty,
        GameState.Ended,
        None,
        0,
        None,
        None,
        Set.empty,
        Map.empty,
        1,
        Vector(alice, bob, charlie, dave)
      )
      val result = game.nextRound().success.value
      result.state should be(PlayingState)
      result.roundNumber should be(2)
      result.scoredRanks(alice) should be(2)
      result.scoredRanks(bob) should be(1)
      result.scoredRanks(charlie) should be(0)
      result.scoredRanks(dave) should be(0)
      result.finishOrder should be(Vector.empty)
      result.playerHands.values.map(_.size).sum should be(Card.standardDeckCards.size)
    }

    "fail NextRound when game is over (someone has 11+ points)" in {
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")
      val dave = HumanPlayer("Dave")
      val game = Game(
        Map(
          alice -> Vector(Card.FiveOfClubs),
          bob -> Vector(Card.SixOfHearts),
          charlie -> Vector(Card.SevenOfHearts),
          dave -> Vector(Card.EightOfHearts)
        ),
        Vector.empty,
        GameState.Ended,
        None,
        0,
        None,
        None,
        Set.empty,
        Map(alice -> 11),
        1,
        Vector(alice)
      )
      game.nextRound().isFailure should be(true)
    }
  }

  "GameState JSON serialization" should {
    "serialize and deserialize all states" in {
      for state <- GameState.values do
        val json = Json.toJson(state)
        val restored = json.as[GameState]
        restored should be(state)
    }

    "fail on unknown state string" in {
      val json = play.api.libs.json.JsString("Unknown")
      json.validate[GameState].isError should be(true)
    }

    "fail on non-string input" in {
      val json = play.api.libs.json.JsNumber(42)
      json.validate[GameState].isError should be(true)
    }
  }

  "GameState XML serialization" should {
    "toXml and fromXml all states" in {
      for state <- GameState.values do
        val xml = GameState.toXml(state)
        GameState.fromXml(xml) should be(state)
    }

    "throw on unknown state text" in {
      an[IllegalArgumentException] should be thrownBy GameState.fromXml(scala.xml.Text("Unknown"))
    }
  }
}
