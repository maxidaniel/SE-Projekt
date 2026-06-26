package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.controller.strategies.IStrategy
import de.htwg_konstanz.se.models.PlayerType.{Computer, Human, Unknown}

import java.util.UUID

enum PlayerType(val strategy: Option[IStrategy]) {
  case Human extends PlayerType(None)
  case Computer(theStrategy: IStrategy) extends PlayerType(Some(theStrategy))
  case Unknown extends PlayerType(None)
}

sealed trait IPlayer(val name: String, val playerType: PlayerType, val id: UUID = UUID.randomUUID)

case class HumanPlayer(myName: String) extends IPlayer(myName, Human)
case class ComputerPlayer(myName: String, strategy: IStrategy) extends IPlayer(myName, Computer(strategy))
case class UnknownPlayer() extends IPlayer("Unknown", Unknown)

// TODO: Do runtime-exchangeable components implementation!!! (FileIO)