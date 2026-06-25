package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.controller.strategies.IStrategy
import java.util.UUID

sealed trait IPlayer {
  def name: String
}

case object HumanPlayer extends IPlayer {
  override val name: String = "Human"
}

case object ComputerPlayer extends IPlayer {
  override val name: String = "Computer"
}

case class Player(id: UUID, name: String, playerType: IPlayer = HumanPlayer, strategy: Option[IStrategy] = None) {
  def this(name: String) = {
    this(UUID.randomUUID(), name, HumanPlayer, None)
  }

  def this(name: String, playerType: IPlayer) = {
    this(UUID.randomUUID(), name, playerType, None)
  }

  def this(name: String, playerType: IPlayer, strategy: IStrategy) = {
    this(UUID.randomUUID(), name, playerType, Some(strategy))
  }
}
