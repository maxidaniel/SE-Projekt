package de.htwg_konstanz.se.ui.gui

import com.google.inject.Inject
import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.models.*

enum View:
  case Menu, Lobby, Game, Result

class PresidentViewModel(@Inject controller: IController):
  var currentView: View = View.Menu
  var statusMessage: String = "Welcome to President. Create a lobby to begin."
  var resultTitle: String = "Game finished"
  var isErrorMessage: Boolean = false

  def handleEvent(event: GameEvent): Unit = event match
    case PlayerJoinEvent(player, game) =>
      statusMessage = s"${player.name} joined the lobby."
      currentView = viewForGame(game)

    case PlayerQuitEvent(player, game) =>
      statusMessage = s"${player.name} left the game."
      currentView = viewForGame(game)

    case GameStartedEvent(game) =>
      statusMessage = "The game has started."
      currentView = View.Game

    case GameAbortedEvent(game) =>
      resultTitle = "Game aborted"
      statusMessage = "The current game was aborted."
      currentView = View.Result

    case GameEndedEvent(game, winner) =>
      resultTitle = "Game finished"
      statusMessage = s"${winner.name} wins the game."
      currentView = View.Result

    case CardPlayedEvent(player, card, game) =>
      statusMessage = s"${player.name} played ${card.toString}."
      currentView = viewForGame(game)

    case PassTrickEvent(player, game) =>
      statusMessage = s"${player.name} passed."
      currentView = viewForGame(game)

    case NextRoundEvent(game) =>
      statusMessage = s"Round ${game.roundNumber} started."
      currentView = viewForGame(game)

    case GameChangedEvent(game) =>
      statusMessage = "Game state changed (Undo/Redo)."
      currentView = viewForGame(game)

    case TableClearedEvent(player, game, reason) =>
      val reasonText = reason match
        case TableClearReason.BurnByTwo       => "Two played (burn)"
        case TableClearReason.FourOfAKindBomb => "Four of a kind (bomb)"
        case TableClearReason.TrickWon        => "All players passed"
      statusMessage = s"Table cleared: $reasonText"
      currentView = viewForGame(game)

    case GameErrorEvent(cause, error) =>
      isErrorMessage = true
      statusMessage = cause match
        case PlayerJoinEvent(player, _)       => s"Could not join: ${error.exception.getMessage}"
        case PlayerQuitEvent(player, _)       => s"Could not leave: ${error.exception.getMessage}"
        case GameStartedEvent(_)              => s"Could not start game: ${error.exception.getMessage}"
        case CardPlayedEvent(player, card, _) => s"Could not play card: ${error.exception.getMessage}"
        case PassTrickEvent(player, _)        => s"Could not pass: ${error.exception.getMessage}"
        case _                                => s"An error occurred: ${error.exception.getMessage}"

  def viewForGame(game: Game, fallbackState: Option[GameState] = None): View =
    (fallbackState.getOrElse(game.state)) match
      case GameState.WaitingForPlayers => View.Lobby
      case GameState.Starting          => View.Game
      case GameState.Playing           => View.Game
      case GameState.Aborted           => View.Result
      case GameState.Ended             => View.Result
