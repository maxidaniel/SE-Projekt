package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.controller.strategies.PlayLowestPossibleCardStrategy
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.models.GameState.{Aborted, Ended, Playing, WaitingForPlayers}
import de.htwg_konstanz.se.util.Listener
import org.scalatest.OptionValues.*
import org.scalatest.TryValues.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class GameControllerSpec extends AnyWordSpec {
  "A GameController" should {
    val alice = HumanPlayer("Alice")
    val bob = HumanPlayer("Bob")

    "start with a waiting game by default" in {
      val controller = new GameController()
      controller.getGameState should be(WaitingForPlayers)
      controller.getGame.playerHands should be(Map.empty)
    }

    "join a player and emit a JoinEvent" in {
      val controller = new GameController()
      var observed: Option[PlayerJoinEvent] = None

      controller.add {
        case e: PlayerJoinEvent => observed = Some(e)
        case _ =>
      }

      controller.join("Alice")
      val player = controller.getPlayer("Alice").value

      controller.getGame.playerHands.keySet should contain(player)
      observed.map(_.player) should be(Some(player))
      observed.map(_.game.playerHands.keySet.contains(player)) should be(Some(true))
    }

    "start a game and emit StartEvent when enough players exist" in {
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val p2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val game = Game(Map(alice -> Vector.empty, bob -> Vector.empty), Vector.empty, WaitingForPlayers)
      val controller = GameController(game)
      var observed: Option[GameStartedEvent] = None

      controller.add {
        case e: GameStartedEvent => observed = Some(e)
        case _ =>
      }

      controller.start()

      controller.getGameState should be(Playing)
      observed.map(_.game.state) should be(Some(Playing))
    }

    "set and expose game instance" in {
      val customGame = Game(Map.empty, Vector.empty, Playing)
      val controller = new GameController(customGame)

      controller.getGame should be(customGame)
      controller.getGameState should be(Playing)
    }

    "not join a player when game is running and emit GameErrorEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Playing))
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.join("Alice")
      observed.isDefined should be(true)
    }

    "not play a card and emit GameErrorEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers))
      controller.join("Alice")
      val player = controller.getPlayer("Alice").value
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.playCard(player, Card.TenOfSpades)
      observed.value.cause should be(CardPlayedEvent(player = player, card = Card.TenOfSpades, game = controller.getGame))
      observed.value.error.failure.exception should have message "Can only play cards in playing state."
    }

    "quit a player and emit a QuitEvent" in {
      val controller = new GameController(Game(Map(alice -> Vector.empty), Vector.empty, WaitingForPlayers))
      var observed: Option[PlayerQuitEvent] = None
      controller.add {
        case e: PlayerQuitEvent => observed = Some(e)
        case _ =>
      }
      controller.quit(alice.id)
      controller.getGame.playerHands.keySet should not contain alice
      observed.map(_.player) should be(Some(alice))
    }

    "not quit a player that is not part of the game and emit GameErrorEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers))
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.quit(alice.id)
      observed.value.cause should be(PlayerQuitEvent(UnknownPlayer(), controller.getGame))
    }

    "return early from quit when player is not in players list" in {
      val controller = new GameController()
      controller.join("Alice")
      val differentPlayer = HumanPlayer("Nobody")
      var observedEvents: List[GameEvent] = List.empty
      controller.add {
        (e: GameEvent) => observedEvents = observedEvents :+ e
      }
      controller.quit(differentPlayer.id)
      observedEvents should not be empty
      observedEvents.head shouldBe a[GameErrorEvent]
    }

    "abort a game and emit a GameAbortedEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Playing))
      var observed: Option[GameAbortedEvent] = None
      controller.add {
        case e: GameAbortedEvent => observed = Some(e)
        case _ =>
      }
      controller.abort()
      controller.getGameState should be(Aborted)
      observed.isDefined should be(true)
    }

    "not abort a game when not playing" in {
      val controller = new GameController()
      controller.abort()
      controller.getGameState should not be Aborted
    }

    "reset an aborted game to waiting state with existing players" in {
      val game = Game(
        Map(alice -> Vector(Card.ThreeOfHearts), bob -> Vector(Card.FourOfClubs)),
        Vector(Card.FiveOfSpades),
        Aborted,
      )
      val controller = new GameController(game)

      controller.reset()
      controller.getGameState should be(WaitingForPlayers)
      controller.getGame.playerHands.keySet should contain(alice)
      controller.getGame.playerHands.keySet should contain(bob)
      controller.getGame.playerHands(alice) should be(Vector.empty)
      controller.getGame.playerHands(bob) should be(Vector.empty)
      controller.getGame.playedCards should be(Vector.empty)
    }

    "reset an ended game to waiting state with existing players" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector(Card.KingOfHearts)),
        Vector(Card.AceOfSpades),
        Ended,
      )
      val controller = new GameController(game)

      controller.reset()
      controller.getGameState should be(WaitingForPlayers)
      controller.getGame.playerHands.keySet should contain(alice)
      controller.getGame.playerHands.keySet should contain(bob)
    }

    "notify listeners on reset" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Aborted))
      var observed: Option[GameEvent] = None
      controller.add((e: GameEvent) => observed = Some(e))
      controller.reset()
      observed.isDefined should be(true)
      observed.get match {
        case GameChangedEvent(_) => // correct
        case _ => fail("Expected GameChangedEvent")
      }
    }

    "not start a game when fewer than 2 players and emit GameErrorEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers))
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.start()
      observed.isDefined should be(true)
    }

    "emit GameExitEvent when exit is called" in {
      val controller = new GameController()
      var observed: Boolean = false
      controller.add {
        case GameExitEvent => observed = true
        case _ =>
      }
      controller.exit()
      observed should be(true)
    }

    "emit GameEndedEvent when a player plays their last card" in {
      val game = Game(
        Map(alice -> Vector(Card.AceOfSpades), bob -> Vector(Card.KingOfHearts, Card.QueenOfClubs)),
        Vector(Card.AceOfClubs),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Ace),
        Some(bob)
      )
      val controller = GameController(game)
      var observed: Option[GameEndedEvent] = None
      controller.add {
        case e: GameEndedEvent => observed = Some(e)
        case _ =>
      }
      controller.playCard(controller.getPlayer("Alice").value, Card.AceOfSpades)
      observed.isDefined should be(true)
      observed.get.winner.name should be("Alice")
    }

    "reject playing a card when it is not the player turn" in {
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(bob)
      )
      val controller = GameController(game)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.playCard(alice, Card.KingOfHearts)
      observed.isDefined should be(true)
      observed.value.error.failure.exception should have message "It is not this players' turn."
    }

    "play a card by index successfully" in {
      val game = Game(
        Map(alice -> Vector(Card.FourOfHearts, Card.FiveOfClubs), bob -> Vector(Card.SixOfSpades)),
        Vector(Card.FourOfDiamonds),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Four),
        Some(alice)
      )
      val controller = GameController(game)
      var observed: Option[CardPlayedEvent] = None
      controller.add {
        case e: CardPlayedEvent => observed = Some(e)
        case _ =>
      }
      controller.playCard(alice, 0)
      observed.isDefined should be(true)
      observed.get.card should be(Card.FourOfHearts)
      controller.getGame.playerHands(alice) should be(Vector(Card.FiveOfClubs))
    }

    "reject playing a card by invalid index" in {
      val game = Game(
        Map(alice -> Vector(Card.ThreeOfHearts)),
        Vector.empty,
        Playing,
        Some(alice)
      )
      val controller = GameController(game)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.playCard(alice, 5)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Invalid card index")
    }

    "play a card by computer using strategy" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val comp = ComputerPlayer("Com", strategy)
      val game = Game(
        Map(comp -> Vector(Card.FourOfHearts, Card.FiveOfClubs), alice -> Vector(Card.SixOfSpades)),
        Vector(Card.FourOfDiamonds),
        PlayingState,
        Some(comp),
        1,
        Some(CardRank.Four),
        Some(comp)
      )
      val controller = GameController(game)
      var observed: Option[CardPlayedEvent] = None
      controller.add {
        case e: CardPlayedEvent => observed = Some(e)
        case _ =>
      }
      controller.playCard(comp)
      observed.value.card should be(Card.FourOfHearts)
    }

    "reject computer play when not player's turn" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val comp = ComputerPlayer("Com", strategy)
      val game = Game(
        Map(comp -> Vector(Card.ThreeOfHearts), alice -> Vector(Card.FourOfClubs)),
        Vector(Card.FiveOfSpades),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      val controller = GameController(game)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.playCard(comp)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Not this player's turn")
    }
  }
}
