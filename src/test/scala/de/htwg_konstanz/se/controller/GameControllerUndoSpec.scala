package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.io.StubSaveManager
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.util.UndoManager
import org.scalatest.OptionValues.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

class GameControllerUndoSpec extends AnyWordSpec {
  "A GameController with Undo/Redo" should {
    val stubSaveManager = new StubSaveManager()
    val controller = new GameController(new Game(), new UndoManager(), stubSaveManager)

    "undo joining a player" in {
      controller.join("Alice")
      val p1 = controller.getPlayer("Alice").value
      controller.getGame.playerHands should contain key p1

      controller.undo()
      controller.getGame.playerHands shouldNot contain key p1

      controller.redo()
      controller.getGame.playerHands should contain key p1
    }

    "undo starting the game" in {
      controller.join("Bob")
      controller.join("Charlie")
      controller.join("Dave")
      controller.getGame.state should be(GameState.WaitingForPlayers)

      controller.start()
      controller.getGame.state should be(GameState.Playing)

      controller.undo()
      controller.getGame.state should be(GameState.WaitingForPlayers)

      controller.redo()
      controller.getGame.state should be(GameState.Playing)
    }

    "undo playing a card" in {
      val testController = new GameController(new Game(), new UndoManager(), stubSaveManager)
      testController.join("Alice")
      testController.join("Bob")
      testController.join("Charlie")
      testController.join("Dave")
      testController.start()
      val game = testController.getGame
      val player = game.currentPlayer.value
      val cards = game.playerHands.get(player).value
      val card = cards.find(_ == Card.ThreeOfClubs).value

      testController.playCard(player, card)
      testController.getGame.playedCards should contain(card)

      testController.undo()
      testController.getGame.playedCards should not contain (card)

      testController.redo()
      testController.getGame.playedCards should contain(card)
    }
  }
}
