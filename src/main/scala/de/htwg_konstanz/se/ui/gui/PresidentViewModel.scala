package de.htwg_konstanz.se.ui.gui

import com.google.inject.Inject
import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.models.*

enum View {
  case Menu, Lobby, Game, Result
}

class PresidentViewModel(@Inject controller: IController) {
  var currentView: View = View.Menu
  private var knownPlayers: Map[IPlayer, String] = Map.empty
  var statusMessage: String = "Welcome to President. Create a lobby to begin."
  var resultTitle: String = "Game finished"
  var isErrorMessage: Boolean = false

  def handleEvent(event: GameEvent): Unit = {
    event match {
      case PlayerJoinEvent(player, game) =>
        rememberPlayer(player)
        statusMessage = s"${displayName(player)} joined the lobby."
        currentView = if game.state == GameState.Playing then View.Game else View.Lobby

      case PlayerQuitEvent(player, game) =>
        statusMessage = s"${displayName(player)} left the game."
        knownPlayers -= player
        currentView = if game.state == GameState.Playing then View.Game else View.Lobby

      case GameStartedEvent(game) =>
        statusMessage = "The game has started."
        currentView = View.Game

      case GameAbortedEvent(game) =>
        resultTitle = "Game aborted"
        statusMessage = "The current game was aborted."
        currentView = View.Result

      case GameEndedEvent(game, winner) =>
        rememberPlayer(winner)
        resultTitle = "Game finished"
        statusMessage = s"${displayName(winner)} wins the game."
        currentView = View.Result

      case CardPlayedEvent(player, card, game) =>
        statusMessage = s"${displayName(player)} played ${card.toString}."
        currentView = viewForGame(game)

      case GameChangedEvent(game) =>
        statusMessage = "Game state changed (Undo/Redo)."
        currentView = viewForGame(game)

      case GameErrorEvent(cause, error) =>
        isErrorMessage = true
        cause match {
          case PlayerJoinEvent(player, _) =>
            statusMessage = s"Could not join: ${error.exception.getMessage}"
          case PlayerQuitEvent(player, _) =>
            statusMessage = s"Could not leave: ${error.exception.getMessage}"
          case GameStartedEvent(_) =>
            statusMessage = s"Could not start game: ${error.exception.getMessage}"
          case CardPlayedEvent(player, card, _) =>
            statusMessage = s"Could not play card: ${error.exception.getMessage}"
          case _ =>
            statusMessage = s"An error occurred: ${error.exception.getMessage}"
        }
    }
  }

  def rememberPlayer(player: IPlayer): Unit = {
    knownPlayers += (player -> player.name)
  }

  def displayName(player: IPlayer): String = {
    knownPlayers.getOrElse(player, "Unknown Player")
  }

  def viewForGame(game: Game, fallbackState: Option[GameState] = None): View = {
    val state = fallbackState.getOrElse(game.state)
    state match {
      case GameState.WaitingForPlayers | WaitingForPlayersState => View.Lobby
      case GameState.Starting | StartingState => View.Game
      case GameState.Playing | PlayingState => View.Game
      case GameState.Aborted | AbortedState => View.Result
      case GameState.Ended | EndedState => View.Result
    }
  }
}
