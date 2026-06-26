package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import de.htwg_konstanz.se.controller.strategies.PlayLowestPossibleCardStrategy
import java.util.UUID

class PlayerSpec extends AnyWordSpec {
  "A player" should {
    "be created with a name" in {
      val player = new Player("Test Player")
      player.name should be("Test Player")
    }

    "be created with a name, and id" in {
      val id = UUID.randomUUID()
      val player = new Player(id, "Test Player")
      player.id should be(id)
      player.name should be("Test Player")
    }

    "be created with a name and playerType" in {
      val player = new Player("Computer Player", ComputerPlayer)
      player.name should be("Computer Player")
      player.playerType should be(ComputerPlayer)
      player.strategy should be(None)
    }

    "be created with a name, playerType, and strategy" in {
      val strategy = new PlayLowestPossibleCardStrategy()
      val player = new Player("AI Player", ComputerPlayer, strategy)
      player.name should be("AI Player")
      player.playerType should be(ComputerPlayer)
      player.strategy should be(Some(strategy))
    }
  }

  "HumanPlayer" should {
    "have name 'Human'" in {
      HumanPlayer.name should be("Human")
    }
  }

  "ComputerPlayer" should {
    "have name 'Computer'" in {
      ComputerPlayer.name should be("Computer")
    }
  }

}
