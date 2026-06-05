package de.htwg_konstanz.se

import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.ui.gui.{GuiPresident, GuiReisen}
import de.htwg_konstanz.se.ui.tui.TuiReisen

import scala.util.Using

@main def run(): Unit =
  val controller = new GameController()
  GuiPresident(controller).main(Array.empty)