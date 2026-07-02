package de.htwg_konstanz.se.ui.gui

import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.controller.strategies.IStrategy
import de.htwg_konstanz.se.models.*

/** Testable interface for GUI actions. No JavaFX dependencies. */
trait IGuiPresenter {
  def controller: IController
  def viewModel: PresidentViewModel

  def currentView: View
  def statusMessage: String
  def isErrorMessage: Boolean
  def resultTitle: String

  def navigateTo(view: View): Unit
  def addPlayer(name: String): Unit
  def addComputerPlayer(name: String, strategy: IStrategy): Unit
  def startGame(): Unit
  def undo(): Unit
  def redo(): Unit
  def abortGame(): Unit
  def resetToLobby(): Unit
  def resetToMenu(): Unit
  def removePlayer(uuid: java.util.UUID): Unit
  def viewForGame(game: Game): View

  def playSelectedCard(player: IPlayer, card: Card): Unit
  def passTrick(player: IPlayer): Unit

  def isComputerTurn: Boolean
  def triggerComputerPlay(): Unit
}
