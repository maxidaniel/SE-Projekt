package de.htwg_konstanz.se.controller.actions

import scala.util.Try

trait BaseGameAction {
  def run(): Try[Unit]
  def undo(): Try[Unit]
  def redo(): Try[Unit]
}