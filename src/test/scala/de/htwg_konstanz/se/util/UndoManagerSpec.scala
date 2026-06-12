package de.htwg_konstanz.se.util

import de.htwg_konstanz.se.models.GameState.{Aborted, Playing, WaitingForPlayers}
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.util.Listener
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class UndoManagerSpec extends AnyWordSpec {
  "An undo manager" should {
    val undoManager = UndoManager()
    "do no undo when undoStack is empty" in {
      val postUndo = undoManager.undoStep()
      postUndo should be(undoManager)
    }

    "do no redo when redoStack is empty" in {
      val postRedo = undoManager.redoStep()
      postRedo should be(undoManager)
    }
  }
}
