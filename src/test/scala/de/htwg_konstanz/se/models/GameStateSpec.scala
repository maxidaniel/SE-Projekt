package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class GameStateSpec extends AnyWordSpec {
  "GameState" should {
    "expose waiting-for-players as a valid state" in {
      GameState.values should contain(GameState.WaitingForPlayers)
    }

    "default to waiting-for-players in new games" in {
      new Game().state should be(GameState.WaitingForPlayers)
    }
  }
}
