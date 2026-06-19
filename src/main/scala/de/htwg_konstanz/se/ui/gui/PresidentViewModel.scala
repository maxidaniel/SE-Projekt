package de.htwg_konstanz.se.ui.gui

import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.models.*
import java.util.UUID

enum View {
  case Menu, Lobby, Game, Result
}

class PresidentViewModel(controller: GameController) {
  var currentView: View = View.Menu
  var knownPlayers: Map[UUID, String] = Map.empty
  var statusMessage: String = "Welcome to President. Create a lobby to begin."
  var resultTitle: String = "Game finished"
  var isErrorMessage: Boolean = false

  def handleEvent(event: GameEvent): Unit = {
    event match {
      case PlayerJoinEvent(player, game) =>
        rememberPlayer(player)
        statusMessage = s"${displayName(player.id)} joined the lobby."
        currentView = if game.state == GameState.Playing then View.Game else View.Lobby

      case PlayerQuitEvent(player, game) =>
        statusMessage = s"${displayName(player.id)} left the game."
        knownPlayers = knownPlayers - player.id
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
        statusMessage = s"${displayName(winner.id)} wins the game."
        currentView = View.Result

      case CardPlayedEvent(player, card, game) =>
        statusMessage = s"${displayName(player.id)} played ${card.toString}."
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

  def rememberPlayer(player: Player): Unit = {
    knownPlayers = knownPlayers + (player.id -> player.name)
  }

  def displayName(playerId: UUID): String = {
    knownPlayers.getOrElse(playerId, "Unknown Player")
  }

  def viewForGame(game: Game, fallbackState: Option[GameState] = None): View = {
    val state = fallbackState.getOrElse(game.state)
    state match {
      case GameState.WaitingForPlayers => View.Lobby
      case GameState.Starting => View.Game
      case GameState.Playing => View.Game
      case GameState.Aborted => View.Result
      case GameState.Ended => View.Result
    }
  }
}
