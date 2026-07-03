package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.controller.strategies.IStrategy
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.models.PlayerType.Computer
import de.htwg_konstanz.se.ui.IPresenter
import de.htwg_konstanz.se.ui.gui.View
import de.htwg_konstanz.se.ui.gui.PresidentViewModel

class TuiPresenter(
    val controller: IController
) extends IPresenter:
  val viewModel: PresidentViewModel = PresidentViewModel(controller)

  def currentView: View = viewModel.currentView
  def statusMessage: String = viewModel.statusMessage
  def isErrorMessage: Boolean = viewModel.isErrorMessage
  def resultTitle: String = viewModel.resultTitle

  def navigateTo(view: View): Unit =
    viewModel.currentView = view

  def addPlayer(name: String): Unit =
    val trimmed = name.trim
    val finalName = if trimmed.isEmpty then s"Player ${controller.playerCount + 1}" else trimmed
    controller.join(finalName)

  def addComputerPlayer(name: String, strategy: IStrategy): Unit =
    val trimmed = name.trim
    val finalName = if trimmed.isEmpty then s"Bot ${controller.playerCount + 1}" else trimmed
    controller.joinComputer(finalName, strategy)

  def startGame(): Unit = controller.start()

  def undo(): Unit = controller.undo()
  def redo(): Unit = controller.redo()

  def abortGame(): Unit = controller.abort()

  def resetToLobby(): Unit =
    controller.reset()
    navigateTo(View.Lobby)

  def resetToMenu(): Unit =
    controller.reset()
    navigateTo(View.Menu)

  def removePlayer(uuid: java.util.UUID): Unit = controller.quit(uuid)

  def playSelectedCard(player: IPlayer, card: Card): Unit = controller.playCard(player, card)

  def passTrick(player: IPlayer): Unit = controller.passTrick(player)

  def nextRound(): Unit = controller.nextRound()

  def save(path: String): Unit = controller.save(path)

  def load(path: String): Unit = controller.load(path)

  def deleteSave(path: String): Unit = controller.deleteSave(path)

  def viewForGame(game: Game): View = viewModel.viewForGame(game)

  def handleEvent(event: GameEvent): Unit =
    viewModel.handleEvent(event)

  def isComputerTurn: Boolean =
    val game = controller.getGame
    game.state == GameState.Playing && game.currentPlayer.exists { player =>
      player.playerType match
        case Computer(_) => true
        case _           => false
    }

  def triggerComputerPlay(): Unit =
    val game = controller.getGame
    if game.state == GameState.Playing then
      game.currentPlayer.foreach { player =>
        player.playerType match
          case Computer(strategy) =>
            val hand = game.playerHands.getOrElse(player, Vector.empty)
            val lastPlayed = game.playedCards.lastOption
            val canPlayNow = strategy.canPlay(hand, lastPlayed, game.playedCards)

            if canPlayNow then controller.playCard(player)
            else controller.passTrick(player)
          case _ =>
      }
