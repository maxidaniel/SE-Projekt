package de.htwg_konstanz.se.util

trait Command {
  def doStep(): Unit
  def undoStep(): Unit
  def redoStep(): Unit
}

case class UndoManager(undoStack: List[Command] = Nil, redoStack: List[Command] = Nil) {
  def doStep(command: Command): UndoManager = {
    command.doStep()
    copy(undoStack = command :: undoStack, redoStack = Nil)
  }

  def undoStep(): UndoManager = {
    undoStack match {
      case Nil => this
      case head :: tail =>
        head.undoStep()
        copy(undoStack = tail, redoStack = head :: redoStack)
    }
  }

  def redoStep(): UndoManager = {
    redoStack match {
      case Nil => this
      case head :: tail =>
        head.redoStep()
        copy(undoStack = head :: undoStack, redoStack = tail)
    }
  }
}
