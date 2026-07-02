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

  "Player equality" should {
    "be equal for same id" in {
      val id = UUID.randomUUID()
      val p1 = HumanPlayer("Test1")
      val p2 = HumanPlayer("Test2")
      // Create players with same id by using the sealed trait constructor
      val p3 = new HumanPlayer("Test1") {
        override val id: UUID = p1.id
      }
      p1 should be(p1)
      p1.hashCode() should be(p1.id.hashCode())
    }

    "not be equal to non-player" in {
      val player = HumanPlayer("Test")
      player.equals("not a player") should be(false)
    }

    "not be equal to null" in {
      val player = HumanPlayer("Test")
      player.equals(null) should be(false)
    }
  }

  "PlayerType" should {
    "have Computer with strategy" in {
      val strat = PlayLowestPossibleCardStrategy()
      PlayerType.Computer(strat).strategy should be(Some(strat))
    }

    "have Human without strategy" in {
      PlayerType.Human.strategy should be(None)
    }

    "have Unknown without strategy" in {
      PlayerType.Unknown.strategy should be(None)
    }
  }

  "UnknownPlayer" should {
    "have name Unknown" in {
      UnknownPlayer.name should be("Unknown")
    }

    "be of type Unknown" in {
      UnknownPlayer.playerType should be(PlayerType.Unknown)
    }
  }
}
