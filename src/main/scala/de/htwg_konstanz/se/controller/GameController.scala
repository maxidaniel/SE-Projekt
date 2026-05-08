package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.models.{Game, GameState, JoinEvent, Player, StartEvent}
import de.htwg_konstanz.se.util.Provider

import java.util.UUID

class GameController(private var game: Game, private var players: Map[UUID, Player]) extends Provider {
  def this() = {
    this(new Game(), Map.empty)
  }
  
  def join(player: Player): Unit = {
    game = game.join(player.id)
    notifyEvent(JoinEvent(player, game))
  }
  
  def start(): Unit = {
    game = game.start()
    notifyEvent(StartEvent(game))
  }
  
  // TODO: temporary
  def setGame(newGame: Game): Unit = this.game = newGame
  
  def getGame: Game = game
  def getGameState: GameState = game.state
}
