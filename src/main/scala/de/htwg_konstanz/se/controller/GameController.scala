package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.models.GameState.Starting
import de.htwg_konstanz.se.util.Provider

import scala.util.{Failure, Success, Try}

class GameController(private var game: Game, private var players: Seq[Player]) extends Provider {
  def this() = {
    this(new Game(), Seq.empty)
  }

  def join(player: Player): Unit = {
    val result = game.join(player.id)
    result match {
      case Success(g) =>
        game = g
        notifyEvent(PlayerJoinEvent(player, game))
      case Failure(f) => 
        println(s"Join failed: ${f.getMessage}")
        notifyEvent(GameErrorEvent(PlayerJoinEvent(player, game), Failure(f)))
    }
  }

  def quit(player: Player): Unit = {
    val result = game.leave(player.id)
    result match {
      case Success(g) =>
        game = g
        notifyEvent(PlayerQuitEvent(player, game))
      case Failure(f) =>
        println(s"Quit failed: ${f.getMessage}")
        notifyEvent(GameErrorEvent(PlayerQuitEvent(player, game), Failure(f)))
    }
  }

  def start(): Unit = {
    val result = game.start()
    result match {
      case Success(g) =>
        game = g
        notifyEvent(GameStartedEvent(game))
      case Failure(f) => 
        println(s"Start failed: ${f.getMessage}")
        notifyEvent(GameErrorEvent(GameStartedEvent(game), Failure(f)))
    }
  }

  def abort(): Unit = {
    val result = game.abort()
    result match {
      case Success(g) =>
        game = g
        notifyEvent(GameAbortedEvent(game))

      case Failure(f) =>
    }
  }

  def getGame: Game = game

  def getGameState: GameState = game.state
}
