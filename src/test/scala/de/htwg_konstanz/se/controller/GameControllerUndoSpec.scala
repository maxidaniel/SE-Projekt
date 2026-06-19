package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.models.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.OptionValues.*
import org.scalatest.wordspec.AnyWordSpec
import java.util.UUID

class GameControllerUndoSpec extends AnyWordSpec {
  "A GameController with Undo/Redo" should {
    val controller = new GameController()

    "undo joining a player" in {
      controller.join("Alice")
      val p1 = controller.getPlayer("Alice").value
      controller.getGame.playerHands should contain key p1.id
      
      controller.undo()
      controller.getGame.playerHands shouldNot contain key p1.id
      
      controller.redo()
      controller.getGame.playerHands should contain key p1.id
    }

    "undo starting the game" in {
      controller.join("Bob")
      controller.getGame.state should be(GameState.WaitingForPlayers)
      
      controller.start()
      controller.getGame.state should be(GameState.Playing)
      
      controller.undo()
      controller.getGame.state should be(GameState.WaitingForPlayers)
      
      controller.redo()
      controller.getGame.state should be(GameState.Playing)
    }

    "undo playing a card" in {
      val testController = new GameController()
      testController.join("Alice")
      testController.join("Bob")
      testController.start()
      val game = testController.getGame
      val playerWithCards = game.playerHands.find(_._2.nonEmpty).get
      val playerId = playerWithCards._1
      val card = playerWithCards._2.head
      val p1 = testController.getPlayer("Alice").value
      val p2 = testController.getPlayer("Bob").value
      val player = if (playerId == p1.id) p1 else p2
      
      testController.playCard(player, card)
      testController.getGame.playedCards should contain(card)
      
      testController.undo()
      testController.getGame.playedCards should not contain(card)
      
      testController.redo()
      testController.getGame.playedCards should contain(card)
    }
  }
}
