package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class GameEventSpec extends AnyWordSpec {
  "Game events" should {
    "allow creating the base event type" in {
      val event = new GameEvent()
      event shouldBe a[GameEvent]
    }

    "store JoinEvent payload" in {
      val player = HumanPlayer("P")
      val game = new Game()
      val event = PlayerJoinEvent(player, game)

      event.player should be(player)
      event.game should be(game)
    }

    "store StartEvent payload" in {
      val game = new Game()
      val event = GameStartedEvent(game)

      event.game should be(game)
    }
  }
}
