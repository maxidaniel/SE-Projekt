package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.models.GameState.{Aborted, Playing, WaitingForPlayers}
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
      val game = Game(Map(p1 -> Vector.empty, p2 -> Vector.empty), Vector.empty, WaitingForPlayers)
      val controller = GameController(game, Seq.empty)
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
      val controller = new GameController(customGame, Seq.empty)

      controller.getGame should be(customGame)
      controller.getGameState should be(Playing)
    }

    "not join a player when game is running and emit GameErrorEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Playing), Seq.empty)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.join("Alice")
      observed.isDefined should be(true)
    }

    "not play a card and emit GameErrorEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers), Seq.empty)
      var observed: Option[GameErrorEvent] = None
      controller.join("Alice")
      val player = controller.getPlayer("Alice").value
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
      val controller = new GameController(Game(Map(p1 -> Vector.empty), Vector.empty, WaitingForPlayers), Seq(Player(p1, "Alice")))
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
      val controller = new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers), Seq(player))
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _ =>
      }
      controller.quit(player.id)
      observed.isDefined should be(true)
      observed.value.error.failure.exception should have message "The player with id " + player.id + " is not part of the game."
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
      observedEvents should be(empty)
    }

    "abort a game and emit a GameAbortedEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Playing), Seq.empty)
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

    "not start a game when fewer than 2 players and emit GameErrorEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers), Seq.empty)
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
  }
}
