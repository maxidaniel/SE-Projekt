package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.controller.strategies.PlayLowestPossibleCardStrategy
import de.htwg_konstanz.se.models.GameState.{Aborted, Ended, Playing, WaitingForPlayers}
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.util.Listener
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.TryValues.*
import org.scalatest.OptionValues.*
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class GameControllerSpec extends AnyWordSpec {
  "A GameController" should {
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

      controller.getGame.playerHands.keySet should contain(player.id)
      observed.map(_.player) should be(Some(player))
      observed.map(_.game.playerHands.keySet.contains(player.id)) should be(Some(true))
    }

    "start a game and emit StartEvent when enough players exist" in {
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val p2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val game = Game(Map(p1 -> Vector.empty, p2 -> Vector.empty), Vector.empty, WaitingForPlayers, Map(p1 -> "Alice", p2 -> "Bob"))
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
      val p1 = UUID.randomUUID()
      val controller = new GameController(Game(Map(p1 -> Vector.empty), Vector.empty, WaitingForPlayers, Map(p1 -> "Alice")))
      val player = controller.getPlayer(p1).value
      var observed: Option[PlayerQuitEvent] = None
      controller.add {
        case e: PlayerQuitEvent => observed = Some(e)
        case _ =>
      }
      controller.quit(player.id)
      controller.getGame.playerHands.keySet should not contain p1
      observed.map(_.player) should be(Some(player))
    }

    "not quit a player that is not part of the game and emit GameErrorEvent" in {
      val player = Player(UUID.randomUUID(), "Alice")
      val controller = new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers))
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.quit(player.id)
      observed.isDefined should be(true)
    }

    "return early from quit when player is not in players list" in {
      val controller = new GameController()
      controller.join("Alice")
      val differentPlayer = Player(UUID.randomUUID(), "Nobody")
      var observedEvents: List[GameEvent] = List.empty
      controller.add {
        case e: GameEvent => observedEvents = observedEvents :+ e
        case _ =>
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
      controller.getGameState should not be (Aborted)
    }

    "reset an aborted game to waiting state with existing players" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(
        Map(p1 -> Vector(Card.ThreeOfHearts), p2 -> Vector(Card.FourOfClubs)),
        Vector(Card.FiveOfSpades),
        Aborted,
        Map(p1 -> "Alice", p2 -> "Bob")
      )
      val controller = new GameController(game)

      controller.reset()
      controller.getGameState should be(WaitingForPlayers)
      controller.getGame.playerHands.keySet should contain(p1)
      controller.getGame.playerHands.keySet should contain(p2)
      controller.getGame.playerHands(p1) should be(Vector.empty)
      controller.getGame.playerHands(p2) should be(Vector.empty)
      controller.getGame.playedCards should be(Vector.empty)
    }

    "reset an ended game to waiting state with existing players" in {
      val p1 = UUID.randomUUID()
      val p2 = UUID.randomUUID()
      val game = Game(
        Map(p1 -> Vector.empty, p2 -> Vector(Card.KingOfHearts)),
        Vector(Card.AceOfSpades),
        Ended,
        Map(p1 -> "Alice", p2 -> "Bob")
      )
      val controller = new GameController(game)

      controller.reset()
      controller.getGameState should be(WaitingForPlayers)
      controller.getGame.playerHands.keySet should contain(p1)
      controller.getGame.playerHands.keySet should contain(p2)
    }

    "notify listeners on reset" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Aborted))
      var observed: Option[GameEvent] = None
      controller.add {
        case e: GameEvent => observed = Some(e)
        case _ =>
      }
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
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val p2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val game = Game(
        Map(p1 -> Vector(Card.AceOfSpades), p2 -> Vector(Card.KingOfHearts, Card.QueenOfClubs)),
        Vector(Card.AceOfClubs),
        PlayingState,
        Map(p1 -> "Alice", p2 -> "Bob"),
        Some(p1),
        1,
        Some(CardRank.Ace),
        Some(p2)
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
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val p2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val game = Game(
        Map(p1 -> Vector(Card.KingOfHearts), p2 -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Map(p1 -> "Alice", p2 -> "Bob"),
        Some(p2),
        1,
        Some(CardRank.Five),
        Some(p2)
      )
      val controller = GameController(game)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.playCard(controller.getPlayer("Alice").value, Card.KingOfHearts)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("not the turn")
    }

    "play a card by index successfully" in {
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val p2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val game = Game(
        Map(p1 -> Vector(Card.FourOfHearts, Card.FiveOfClubs), p2 -> Vector(Card.SixOfSpades)),
        Vector(Card.FourOfDiamonds),
        PlayingState,
        Map(p1 -> "Alice", p2 -> "Bob"),
        Some(p1),
        1,
        Some(CardRank.Four),
        Some(p1)
      )
      val controller = GameController(game)
      var observed: Option[CardPlayedEvent] = None
      controller.add {
        case e: CardPlayedEvent => observed = Some(e)
        case _ =>
      }
      val player = controller.getPlayer(p1).value
      controller.playCardByIndex(player, 0)
      observed.isDefined should be(true)
      observed.get.card should be(Card.FourOfHearts)
      controller.getGame.playerHands(p1) should be(Vector(Card.FiveOfClubs))
    }

    "reject playing a card by invalid index" in {
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val game = Game(
        Map(p1 -> Vector(Card.ThreeOfHearts)),
        Vector.empty,
        Playing,
        Map(p1 -> "Alice"),
        Some(p1)
      )
      val controller = GameController(game)
      val player = controller.getPlayer("Alice").value
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.playCardByIndex(player, 5)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Invalid card index")
    }

    "play a card by computer using strategy" in {
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val p2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val strategy = PlayLowestPossibleCardStrategy()
      val game = Game(
        Map(p1 -> Vector(Card.FourOfHearts, Card.FiveOfClubs), p2 -> Vector(Card.SixOfSpades)),
        Vector(Card.FourOfDiamonds),
        PlayingState,
        Map(p1 -> "Alice", p2 -> "Bob"),
        Some(p1),
        1,
        Some(CardRank.Four),
        Some(p1)
      )
      val controller = GameController(game)
      var observed: Option[CardPlayedEvent] = None
      controller.add {
        case e: CardPlayedEvent => observed = Some(e)
        case _ =>
      }
      val player = Player(p1, "Alice", ComputerPlayer, Some(strategy))
      controller.playCardByComputer(player)
      observed.isDefined should be(true)
      observed.get.card should be(Card.FourOfHearts)
    }

    "reject computer play when not player's turn" in {
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val p2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val strategy = PlayLowestPossibleCardStrategy()
      val game = Game(
        Map(p1 -> Vector(Card.ThreeOfHearts), p2 -> Vector(Card.FourOfClubs)),
        Vector(Card.FiveOfSpades),
        PlayingState,
        Map(p1 -> "Alice", p2 -> "Bob"),
        Some(p2),
        1,
        Some(CardRank.Five),
        Some(p2)
      )
      val controller = GameController(game)
      val player = Player(p1, "Alice", ComputerPlayer, Some(strategy))
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.playCardByComputer(player)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Not this player's turn")
    }

    "reject computer play when no strategy configured" in {
      val p1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val game = Game(
        Map(p1 -> Vector(Card.ThreeOfHearts)),
        Vector.empty,
        PlayingState,
        Map(p1 -> "Alice"),
        Some(p1),
        1,
        Some(CardRank.Three),
        Some(p1)
      )
      val controller = GameController(game)
      val player = Player(p1, "Alice", ComputerPlayer, None)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.playCardByComputer(player)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("no strategy")
    }
  }
}
