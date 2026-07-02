package de.htwg_konstanz.se.util

import de.htwg_konstanz.se.models.GameEvent

trait Provider:
  var listeners: Vector[Listener] = Vector()
  def add(l: Listener): Unit = listeners = listeners :+ l
  def remove(l: Listener): Unit = listeners = listeners.filterNot(li => li == l)
  def notifyEvent(event: GameEvent): Unit = listeners.foreach(l => l.onEvent(event))
