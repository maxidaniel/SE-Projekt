package de.htwg_konstanz.se.controller.actions

import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.models.{Game, Player}

import scala.util.Try

class JoinAction(controller: GameController, player: Player) extends BaseGameAction {
  private var oldGame: Option[Game] = None
  
  override def run(): Try[Unit] = Try {
    oldGame = Some(controller.getGame)
    controller.join(player)
  }

  override def undo(): Try[Unit] = ???

  override def redo(): Try[Unit] = ???
}

object JoinAction {
  
}
