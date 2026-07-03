package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.io.StubSaveManager
import de.htwg_konstanz.se.models.GameState.{Ended, Playing}
import de.htwg_konstanz.se.models.PlayingState
import de.htwg_konstanz.se.util.UndoManager
import org.scalatest.TryValues.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

class GameSpec extends AnyWordSpec {
  private val stubSaveManager = new StubSaveManager()

  private def makeGame(
      playerHands: Map[IPlayer, Vector[Card]],
      playedCards: Vector[Card] = Vector.empty,
      state: GameState = Playing,
      currentPlayer: Option[IPlayer] = None,
      trickCount: Int = 0,
      trickRank: Option[CardRank] = None,
      trickLeader: Option[IPlayer] = None
  ): Game = {
    Game(playerHands, playedCards, state, currentPlayer, trickCount, trickRank, trickLeader)
  }

  private def makePlayingGame(
      playerHands: Map[IPlayer, Vector[Card]],
      trickRank: CardRank,
      trickLeader: IPlayer,
      currentPlayer: IPlayer = null,
      playedCards: Vector[Card] = Vector.empty
  ): Game = {
    val cp = if (currentPlayer != null) currentPlayer else trickLeader
    Game(playerHands, playedCards, PlayingState, Some(cp), 1, Some(trickRank), Some(trickLeader))
  }

  private def makeController(game: Game): GameController = new GameController(game, new UndoManager(), stubSaveManager)

  "A game" should {
    val alice = HumanPlayer("Alice")
    val bob = HumanPlayer("Bob")
    val charlie = HumanPlayer("Charlie")
    val dave = HumanPlayer("Dave")

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

    "start when waiting and at least four players exist" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty, dave -> Vector.empty),
        Vector.empty,
        GameState.WaitingForPlayers
      )

      val started = game.start().success.value
      started.state should be(Playing)
      started.playerHands.values.map(_.size).sum should be(Card.standardDeckCards.size)
    }

    "not start when fewer than four players exist" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty),
        Vector.empty,
        GameState.WaitingForPlayers
      )
      game.start().failure.exception should have message "Can only start a new game with four or more players."
    }

    "not start when game is not in waiting state" in {
      val game = Game(Map.empty, Vector.empty, Playing)
      game.start().failure.exception should have message "Can only start a new game when in lobby."
    }

    "deal cards as evenly as possible" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty, dave -> Vector.empty),
        Vector.empty,
        GameState.WaitingForPlayers
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
      val playedCard = Card.ThreeOfClubs
      val game = Game(
        Map(
          alice -> Vector(playedCard, Card.KingOfHearts),
          bob -> Vector(Card.FiveOfClubs)
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

      game.playCard(alice, Card.FiveOfHearts).failure.exception should have message "Must play a card higher than Ten."
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
        None,
        isFirstTrick = false
      )

      val ended = game.playCard(alice, winningCard).success.value
      ended.state should be(Ended)
      ended.playerHands(alice) should be(Vector.empty)
    }

    "reject playing a card that the player does not have" in {
      val game = Game(
        Map(
          alice -> Vector(Card.FourOfHearts),
          bob -> Vector(Card.QueenOfClubs)
        ),
        Vector.empty,
        Playing
      )

      game.playCard(alice, Card.AceOfClubs).failure.exception.getMessage should include("does not have card")
    }

    "reject responding with a card that has equal rank to trick rank" in {
      val game = Game(
        Map(
          alice -> Vector(Card.FiveOfHearts),
          bob -> Vector(Card.FiveOfClubs),
          charlie -> Vector(Card.SixOfHearts),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector(Card.FiveOfHearts),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      game.playCard(bob, Card.FiveOfClubs).failure.exception.getMessage should include("higher")
    }

    "reject responding with a card that outranks last play but not trick rank" in {
      val game = Game(
        Map(
          alice -> Vector(Card.SixOfHearts),
          bob -> Vector(Card.FiveOfClubs),
          charlie -> Vector(Card.SixOfClubs),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector(Card.FiveOfHearts),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      game.playCard(bob, Card.FiveOfClubs).failure.exception.getMessage should include("higher")
    }

    "reject responding with lower card than last played card" in {
      val game = Game(
        Map(
          alice -> Vector(Card.ThreeOfHearts),
          bob -> Vector(Card.SixOfHearts),
          charlie -> Vector(Card.FiveOfClubs),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector(Card.FourOfHearts, Card.SevenOfClubs),
        PlayingState,
        Some(charlie),
        2,
        Some(CardRank.Four),
        Some(alice)
      )
      game.playCard(charlie, Card.FiveOfClubs).failure.exception.getMessage should include("outrank")
    }

    "succeed NextRound with empty finishOrder" in {
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
        Vector.empty
      )
      game.nextRound().isSuccess should be(true)
    }

    "reject playing an unknown card" in {
      val game = Game(
        Map(alice -> Vector(Card.FourOfHearts)),
        Vector.empty,
        Playing
      )
      game.playCard(alice, Card.Unknown).isFailure should be(true)
    }

    "reject playing a card when player is not in the game" in {
      val game = Game(
        Map(alice -> Vector(Card.FourOfHearts)),
        Vector.empty,
        Playing
      )
      game.playCard(bob, Card.FiveOfHearts).failure.exception.getMessage should include(bob.id.toString)
    }

    "burn the trick when leading four of a kind" in {
      val game = Game(
        Map(
          alice -> Vector(
            Card.SevenOfHearts,
            Card.SevenOfClubs,
            Card.SevenOfSpades,
            Card.SevenOfDiamonds,
            Card.ThreeOfHearts
          ),
          bob -> Vector(Card.TenOfHearts),
          charlie -> Vector(Card.JackOfHearts),
          dave -> Vector(Card.QueenOfHearts)
        ),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None,
        isFirstTrick = false
      )
      val afterPlay = game.playCard(alice, Card.SevenOfHearts).success.value
      afterPlay.playedCards should be(Vector.empty)
      afterPlay.trickCount should be(0)
      afterPlay.trickRank should be(None)
      afterPlay.trickLeader should be(None)
      afterPlay.currentPlayer should be(Some(alice))
      afterPlay.playerHands(alice) should be(Vector(Card.ThreeOfHearts))
    }

    "burn the trick when responding with a two" in {
      val game = Game(
        Map(
          alice -> Vector(Card.ThreeOfHearts),
          bob -> Vector(Card.TwoOfHearts, Card.FourOfHearts),
          charlie -> Vector(Card.AceOfHearts),
          dave -> Vector(Card.KingOfHearts)
        ),
        Vector(Card.AceOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Ace),
        Some(alice)
      )
      val afterPlay = game.playCard(bob, Card.TwoOfHearts).success.value
      afterPlay.playedCards should be(Vector.empty)
      afterPlay.trickCount should be(0)
      afterPlay.trickRank should be(None)
      afterPlay.trickLeader should be(None)
      afterPlay.currentPlayer should be(Some(bob))
    }

    "end game when burn leaves player with no cards" in {
      val game = Game(
        Map(
          alice -> Vector(Card.ThreeOfHearts),
          bob -> Vector(Card.TwoOfHearts),
          charlie -> Vector(Card.AceOfHearts),
          dave -> Vector(Card.KingOfHearts)
        ),
        Vector(Card.AceOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Ace),
        Some(alice)
      )
      val afterPlay = game.playCard(bob, Card.TwoOfHearts).success.value
      afterPlay.state should be(GameState.Ended)
      afterPlay.playerHands(bob) should be(Vector.empty)
    }

    "end game when leading four of a kind empties hand" in {
      val game = Game(
        Map(
          alice -> Vector(Card.SevenOfHearts, Card.SevenOfClubs, Card.SevenOfSpades, Card.SevenOfDiamonds),
          bob -> Vector(Card.TenOfHearts),
          charlie -> Vector(Card.JackOfHearts),
          dave -> Vector(Card.QueenOfHearts)
        ),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None,
        isFirstTrick = false
      )
      val afterPlay = game.playCard(alice, Card.SevenOfHearts).success.value
      afterPlay.state should be(GameState.Ended)
      afterPlay.playerHands(alice) should be(Vector.empty)
      afterPlay.finishOrder should contain(alice)
    }

    "complete trick when responder is last non-leader to play" in {
      val game = Game(
        Map(
          alice -> Vector(Card.KingOfHearts),
          bob -> Vector(Card.AceOfSpades, Card.ThreeOfHearts),
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
      val afterPlay = game.playCard(bob, Card.AceOfSpades).success.value
      afterPlay.trickCount should be(0)
      afterPlay.trickRank should be(None)
      afterPlay.currentPlayer should be(Some(alice))
    }

    "not deal cards when fewer than four players" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty),
        Vector.empty,
        GameState.WaitingForPlayers
      )
      game.deal().failure.exception should have message "Can only deal cards when four or more players are in the game."
    }

    "track finish order when players empty their hands" in {
      val game = Game(
        Map(
          alice -> Vector(Card.ThreeOfHearts),
          bob -> Vector(Card.FourOfHearts, Card.FiveOfHearts),
          charlie -> Vector(Card.SixOfHearts),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None,
        isFirstTrick = false
      )
      val afterAlice = game.playCard(alice, Card.ThreeOfHearts).success.value
      afterAlice.finishOrder should contain(alice)
    }

    "calculate scores correctly for round positions" in {
      Game.scoreForPosition(0, 4) should be(2)
      Game.scoreForPosition(1, 4) should be(1)
      Game.scoreForPosition(2, 4) should be(0)
      Game.scoreForPosition(3, 4) should be(0)
    }

    "start next round from ended state" in {
      val game = Game(
        Map(
          alice -> Vector.empty,
          bob -> Vector(Card.FourOfHearts),
          charlie -> Vector(Card.SixOfHearts),
          dave -> Vector(Card.SevenOfHearts)
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
      val nextRound = game.nextRound().success.value
      nextRound.state should be(PlayingState)
      nextRound.roundNumber should be(2)
      nextRound.scoredRanks(alice) should be(2)
      nextRound.scoredRanks(bob) should be(1)
      nextRound.scoredRanks(charlie) should be(0)
      nextRound.scoredRanks(dave) should be(0)
      nextRound.finishOrder should be(Vector.empty)
      nextRound.playerHands.values.map(_.size).sum should be(Card.standardDeckCards.size)
    }

    "fail next round when game is over (someone has 11+ points)" in {
      val game = Game(
        Map(
          alice -> Vector.empty,
          bob -> Vector(Card.FourOfHearts),
          charlie -> Vector(Card.SixOfHearts),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector.empty,
        GameState.Ended,
        None,
        0,
        None,
        None,
        Set.empty,
        Map(alice -> 11),
        6,
        Vector(alice)
      )
      game.nextRound().isFailure should be(true)
    }

    "exchange cards between president and scum" in {
      val presHand = Vector(Card.ThreeOfHearts, Card.FourOfHearts, Card.FiveOfHearts)
      val scumHand = Vector(Card.KingOfHearts, Card.AceOfHearts, Card.TwoOfHearts)
      val vpHand = Vector(Card.SixOfHearts, Card.SevenOfHearts)
      val vscumHand = Vector(Card.TenOfHearts, Card.JackOfHearts)

      val playerHands: Map[IPlayer, Vector[Card]] = Map(
        alice -> presHand,
        bob -> scumHand,
        charlie -> vpHand,
        dave -> vscumHand
      )

      val result = Game.exchangeCards(alice, bob, Some(charlie), Some(dave), playerHands)

      result(alice) should contain(Card.AceOfHearts)
      result(alice) should contain(Card.TwoOfHearts)
      result(alice) should not contain (Card.ThreeOfHearts)

      result(bob) should contain(Card.ThreeOfHearts)
      result(bob) should contain(Card.FourOfHearts)
      result(bob) should not contain (Card.TwoOfHearts)

      result(charlie) should contain(Card.JackOfHearts)
      result(charlie) should not contain (Card.SixOfHearts)

      result(dave) should contain(Card.SixOfHearts)
      result(dave) should not contain (Card.JackOfHearts)
    }

    "get best cards sorted by rank descending" in {
      val hand = Vector(Card.ThreeOfHearts, Card.AceOfHearts, Card.KingOfHearts, Card.FiveOfHearts)
      val best = Game.getBestCards(hand, 2)
      best should be(Vector(Card.AceOfHearts, Card.KingOfHearts))
    }

    "get worst cards sorted by rank ascending" in {
      val hand = Vector(Card.ThreeOfHearts, Card.AceOfHearts, Card.KingOfHearts, Card.FiveOfHearts)
      val worst = Game.getWorstCards(hand, 2)
      worst should be(Vector(Card.ThreeOfHearts, Card.FiveOfHearts))
    }

    "identify four of a kind" in {
      val fourCards = Vector(Card.SevenOfHearts, Card.SevenOfClubs, Card.SevenOfSpades, Card.SevenOfDiamonds)
      Game.isFourOfAKind(fourCards) should be(true)
    }

    "reject non-four of a kind (3 cards)" in {
      val threeCards = Vector(Card.SevenOfHearts, Card.SevenOfClubs, Card.SevenOfSpades)
      Game.isFourOfAKind(threeCards) should be(false)
    }

    "reject non-four of a kind (mixed ranks)" in {
      val mixedCards = Vector(Card.SevenOfHearts, Card.EightOfClubs, Card.NineOfSpades, Card.TenOfDiamonds)
      Game.isFourOfAKind(mixedCards) should be(false)
    }

    "reject non-four of a kind (empty)" in {
      Game.isFourOfAKind(Vector.empty) should be(false)
    }

    "passTrick delegates to state.transition" in {
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
      game.passTrick(bob).isSuccess should be(true)
    }

    "passTrick fail when no trick led" in {
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts)),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None
      )
      game.passTrick(alice).isFailure should be(true)
    }

    "passTrick fail when leader tries to pass" in {
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
      game.passTrick(alice).isFailure should be(true)
    }

    "round-trip a full game through JSON" in {
      val game = Game(
        playerHands = Map(
          alice -> Vector(Card.ThreeOfClubs, Card.FourOfHearts),
          bob -> Vector(Card.FiveOfClubs, Card.SixOfHearts),
          charlie -> Vector(Card.SevenOfClubs, Card.EightOfHearts),
          dave -> Vector(Card.NineOfClubs, Card.TenOfHearts)
        ),
        playedCards = Vector(Card.AceOfClubs),
        state = PlayingState,
        currentPlayer = Some(alice),
        trickCount = 1,
        trickRank = Some(CardRank.Ace),
        trickLeader = Some(bob),
        passedPlayers = Set(charlie),
        scoredRanks = Map(alice -> 3, bob -> 1),
        roundNumber = 2,
        finishOrder = Vector(alice, bob)
      )
      val json = play.api.libs.json.Json.toJson(game)
      val restored = json.as[Game]
      restored.playerHands.size should be(4)
      restored.state should be(PlayingState)
      restored.currentPlayer should be(Some(alice))
      restored.trickCount should be(1)
      restored.trickRank should be(Some(CardRank.Ace))
      restored.trickLeader should be(Some(bob))
      restored.passedPlayers should be(Set(charlie))
      restored.scoredRanks should be(Map(alice -> 3, bob -> 1))
      restored.roundNumber should be(2)
      restored.finishOrder should be(Vector(alice, bob))
      restored.isFirstTrick should be(true)
    }

    "round-trip a game with no current player through JSON" in {
      val game = Game(
        playerHands = Map(alice -> Vector(Card.AceOfClubs)),
        playedCards = Vector.empty,
        state = EndedState,
        currentPlayer = None,
        trickCount = 0,
        trickRank = None,
        trickLeader = None
      )
      val json = play.api.libs.json.Json.toJson(game)
      val restored = json.as[Game]
      restored.currentPlayer should be(None)
      restored.trickRank should be(None)
      restored.trickLeader should be(None)
    }

    "round-trip a full game through XML" in {
      val game = Game(
        playerHands = Map(
          alice -> Vector(Card.ThreeOfClubs, Card.FourOfHearts),
          bob -> Vector(Card.FiveOfClubs),
          charlie -> Vector(Card.SixOfClubs),
          dave -> Vector(Card.SevenOfClubs)
        ),
        playedCards = Vector(Card.AceOfClubs),
        state = PlayingState,
        currentPlayer = Some(alice),
        trickCount = 1,
        trickRank = Some(CardRank.Ace),
        trickLeader = Some(bob),
        passedPlayers = Set(charlie),
        scoredRanks = Map(alice -> 3),
        roundNumber = 2,
        finishOrder = Vector(alice, bob)
      )
      val xml = Game.toXml(game)
      val restored = Game.fromXml(xml)
      restored.playerHands.size should be(4)
      restored.state should be(PlayingState)
      restored.currentPlayer should be(Some(alice))
      restored.trickCount should be(1)
      restored.trickRank should be(Some(CardRank.Ace))
      restored.trickLeader should be(Some(bob))
      restored.passedPlayers should be(Set(charlie))
      restored.scoredRanks should be(Map(alice -> 3))
      restored.roundNumber should be(2)
      restored.finishOrder should be(Vector(alice, bob))
      restored.isFirstTrick should be(true)
    }

    "round-trip a game with no current player through XML" in {
      val game = Game(
        playerHands = Map(alice -> Vector(Card.AceOfClubs)),
        playedCards = Vector.empty,
        state = EndedState,
        currentPlayer = None,
        trickCount = 0,
        trickRank = None,
        trickLeader = None
      )
      val xml = Game.toXml(game)
      val restored = Game.fromXml(xml)
      restored.currentPlayer should be(None)
      restored.trickRank should be(None)
      restored.trickLeader should be(None)
    }

    "round-trip a game with empty finishOrder through XML" in {
      val game = Game(
        playerHands = Map(alice -> Vector(Card.AceOfClubs)),
        playedCards = Vector.empty,
        state = PlayingState,
        finishOrder = Vector.empty
      )
      val xml = Game.toXml(game)
      val restored = Game.fromXml(xml)
      restored.finishOrder should be(Vector.empty)
    }

    "round-trip isFirstTrick=false through JSON" in {
      val game = Game(
        playerHands = Map(alice -> Vector(Card.ThreeOfClubs)),
        playedCards = Vector.empty,
        state = PlayingState,
        isFirstTrick = false
      )
      val json = play.api.libs.json.Json.toJson(game)
      val restored = json.as[Game]
      restored.isFirstTrick should be(false)
    }

    "round-trip isFirstTrick=false through XML" in {
      val game = Game(
        playerHands = Map(alice -> Vector(Card.ThreeOfClubs)),
        playedCards = Vector.empty,
        state = PlayingState,
        isFirstTrick = false
      )
      val xml = Game.toXml(game)
      val restored = Game.fromXml(xml)
      restored.isFirstTrick should be(false)
    }

    "deserialize game JSON with missing playerHands and scoredRanks" in {
      val json = play.api.libs.json.Json.parse("""{"playedCards":[],"state":"Playing","trickCount":0,"passedPlayers":[],"finishOrder":[]}""")
      val result = json.as[Game]
      result.playerHands should be(Map.empty)
      result.scoredRanks should be(Map.empty)
    }

    "fail to deserialize game JSON with invalid player entry" in {
      val valid = HumanPlayer("Alice")
      val json = play.api.libs.json.Json.parse(s"""{
        "playerHands": [
          {"player": {"type": "Human", "name": "Alice", "id": "${valid.id}"}, "cards": []},
          {"player": {"type": "Computer", "name": "Bot", "strategy": "NonExistent"}, "cards": []},
          {"player": {"type": "Human", "name": "Bob", "id": "${java.util.UUID.randomUUID()}"}, "cards": []}
        ],
        "playedCards": [], "state": "Playing", "trickCount": 0, "passedPlayers": [], "finishOrder": []
      }""")
      json.validate[Game].isError should be(true)
    }

    "fail to deserialize game JSON with invalid scoredRank entry" in {
      val alice = HumanPlayer("Alice")
      val bob = HumanPlayer("Bob")
      val json = play.api.libs.json.Json.parse(s"""{
        "playerHands": [{"player": {"type": "Human", "name": "Alice", "id": "${alice.id}"}, "cards": []}],
        "playedCards": [], "state": "Playing", "trickCount": 0, "passedPlayers": [], "finishOrder": [],
        "scoredRanks": [
          {"player": {"type": "Human", "name": "Alice", "id": "${alice.id}"}, "score": 1},
          {"player": {"type": "Computer", "name": "Bot", "strategy": "NonExistent"}, "score": 5},
          {"player": {"type": "Human", "name": "Bob", "id": "${bob.id}"}, "score": 2}
        ]
      }""")
      json.validate[Game].isError should be(true)
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
