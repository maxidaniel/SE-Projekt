package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.models.GameState.{Playing, WaitingForPlayers}
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.util.Listener
import org.scalatest.matchers.should.Matchers.*
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
      val player = Player(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "Alice")
      var observed: Option[PlayerJoinEvent] = None

      controller.add {
        case e: PlayerJoinEvent => observed = Some(e)
        case _ =>
      }

      controller.join(player)

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
  }
}
