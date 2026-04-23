package de.htwg_konstanz.se

import de.htwg_konstanz.se.ui.tui.Tui

import scala.util.Using

@main def run(): Unit =
  Using(Tui()) { tui =>
    tui.run()
  }