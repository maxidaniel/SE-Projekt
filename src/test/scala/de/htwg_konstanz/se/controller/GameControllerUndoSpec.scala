package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.models.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import java.util.UUID

class GameControllerUndoSpec extends AnyWordSpec {
  "A GameController with Undo/Redo" should {
    val controller = new GameController()
    val p1 = Player(UUID.randomUUID(), "Alice")
    val p2 = Player(UUID.randomUUID(), "Bob")

    "undo joining a player" in {
      controller.join(p1)
      controller.getGame.playerHands should contain key p1.id
      
      controller.undo()
      controller.getGame.playerHands shouldNot contain key p1.id
      
      controller.redo()
      controller.getGame.playerHands should contain key p1.id
    }

    "undo starting the game" in {
      controller.join(p2)
      controller.getGame.state should be(GameState.WaitingForPlayers)
      
      controller.start()
      controller.getGame.state should be(GameState.Playing)
      
      controller.undo()
      controller.getGame.state should be(GameState.WaitingForPlayers)
      
      controller.redo()
      controller.getGame.state should be(GameState.Playing)
    }

    "undo playing a card" in {
      val game = controller.getGame
      val playerWithCards = game.playerHands.find(_._2.nonEmpty).get
      val playerId = playerWithCards._1
      val card = playerWithCards._2.head
      val player = if (playerId == p1.id) p1 else p2
      
      controller.playCard(player, card)
      controller.getGame.playedCards should contain(card)
      
      controller.undo()
      controller.getGame.playedCards should not contain(card)
      
      controller.redo()
      controller.getGame.playedCards should contain(card)
    }
  }
}
