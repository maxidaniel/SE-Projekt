package de.htwg_konstanz.se.controller.commands

import de.htwg_konstanz.se.models.Game

import scala.util.Try

case class JoinCommand() extends Command {

  override def invoke(): Try[Game] = ???

  override def undo(): Try[Game] = ???

  override def redo(): Try[Game] = ???
}
