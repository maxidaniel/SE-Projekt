package de.htwg_konstanz.se.ui.gui

import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.models.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import java.util.UUID

class PresidentViewModelSpec extends AnyWordSpec {
  "A PresidentViewModel" should {
    val controller = new GameController()
    val viewModel = new PresidentViewModel(controller)

    "update state on PlayerJoinEvent" in {
      val p1 = Player(UUID.randomUUID(), "Alice")
      viewModel.handleEvent(PlayerJoinEvent(p1, controller.getGame))
      viewModel.knownPlayers should contain(p1.id -> "Alice")
      viewModel.statusMessage should include("Alice joined")
    }

    "update state on GameStartedEvent" in {
      viewModel.handleEvent(GameStartedEvent(controller.getGame))
      viewModel.currentView should be(View.Game)
    }

    "update state on GameEndedEvent" in {
      val p1 = Player(UUID.randomUUID(), "Winner")
      viewModel.handleEvent(GameEndedEvent(controller.getGame, p1))
      viewModel.knownPlayers should contain(p1.id -> "Winner")
      viewModel.currentView should be(View.Result)
      viewModel.statusMessage should include("Winner wins")
    }

    "update state on CardPlayedEvent" in {
      val p1 = Player(UUID.randomUUID(), "Alice")
      val card = Card.ThreeOfSpades
      viewModel.rememberPlayer(p1)
      viewModel.handleEvent(CardPlayedEvent(p1, card, controller.getGame))
      viewModel.statusMessage should include("Alice played")
    }

    "update state on GameChangedEvent" in {
      viewModel.handleEvent(GameChangedEvent(controller.getGame))
      viewModel.statusMessage should include("Game state changed")
    }
  }
}
