package de.htwg_konstanz.se.util

import de.htwg_konstanz.se.models.GameEvent

trait Provider:
  var listeners: Vector[Listener] = Vector()
  def add(l: Listener) = listeners = listeners :+ l
  def remove(l: Listener) = listeners = listeners.filterNot(li => li == l)
  def notifyEvent(event: GameEvent) = listeners.foreach(l => l.onEvent(event))