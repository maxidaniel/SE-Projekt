package de.htwg_konstanz.se.controller.commands

import de.htwg_konstanz.se.models.Game

import scala.util.Try

trait Command {
  def invoke(): Try[Game]
  def undo(): Try[Game]
  def redo(): Try[Game]
}
