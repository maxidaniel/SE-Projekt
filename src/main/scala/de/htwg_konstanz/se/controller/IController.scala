package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.controller.strategies.IStrategy
import de.htwg_konstanz.se.models.{Card, Game, GameState, IPlayer}
import de.htwg_konstanz.se.util.Provider

import java.util.UUID

trait IController extends Provider {
  def join(name: String): Unit
  def joinComputer(name: String, strategy: IStrategy): Unit
  def quit(uuid: UUID): Unit
  def start(): Unit

  def playCard(player: IPlayer): Unit
  def playCard(player: IPlayer, card: Card): Unit
  def playCard(player: IPlayer, index: Int): Unit

  def passTrick(player: IPlayer): Unit
  def abort(): Unit
  def undo(): Unit
  def redo(): Unit
  def reset(): Unit
  def exit(): Unit
  def nextRound(): Unit
  def getGame: Game
  def getGameState: GameState

  def getPlayer(name: String): Option[IPlayer]
  def getPlayer(uuid: UUID): Option[IPlayer]
  def players: Seq[IPlayer]
  def playerCount: Int

  def save(path: String): Unit
  def load(path: String): Unit
  def deleteSave(path: String): Unit
}
