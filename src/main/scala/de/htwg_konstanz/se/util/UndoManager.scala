package de.htwg_konstanz.se.util

trait Command {
  def doStep(): Unit
  def undoStep(): Unit
  def redoStep(): Unit
}

trait IUndoManager:
  def doStep(command: Command): UndoManager
  def undoStep(): UndoManager
  def redoStep(): UndoManager

case class UndoManager(undoStack: List[Command] = Nil, redoStack: List[Command] = Nil) extends IUndoManager {
  def this() = this(List.empty, List.empty)

  override def doStep(command: Command): UndoManager = {
    command.doStep()
    copy(undoStack = command :: undoStack, redoStack = Nil)
  }

  override def undoStep(): UndoManager = {
    undoStack match {
      case Nil => this
      case head :: tail =>
        head.undoStep()
        copy(undoStack = tail, redoStack = head :: redoStack)
    }
  }

  override def redoStep(): UndoManager = {
    redoStack match {
      case Nil => this
      case head :: tail =>
        head.redoStep()
        copy(undoStack = head :: undoStack, redoStack = tail)
    }
  }
}
