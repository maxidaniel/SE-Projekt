package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import de.htwg_konstanz.se.controller.strategies.PlayLowestPossibleCardStrategy
import java.util.UUID

class PlayerSpec extends AnyWordSpec {
  "A human player" should {
    val player = HumanPlayer("Test")
    
    "be created with a name, id, Human player type, and empty strategy" in {
      player.name should be("Test")
      player.id should not be null
      player.playerType should be(PlayerType.Human)
      player.playerType.strategy should be(None)
    }
  }

  "A computer player" should {
    val strat = PlayLowestPossibleCardStrategy()
    val player = ComputerPlayer("Computer", strat)
    
    "have name 'Computer'" in {
      player.name should be("Computer")
    }
    
    "have PlayLowestPossibleCard strategy" in {
      player.strategy should be(strat)  
    }
  }
}
