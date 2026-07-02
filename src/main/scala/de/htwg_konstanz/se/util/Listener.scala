package de.htwg_konstanz.se.util

import de.htwg_konstanz.se.models.GameEvent

trait Listener:
  def onEvent(event: GameEvent): Unit
