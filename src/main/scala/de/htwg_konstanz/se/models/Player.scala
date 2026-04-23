package de.htwg_konstanz.se.models

import java.util.UUID

case class Player(id: UUID, name: String) {
  def this(name: String) = {
    this(UUID.randomUUID(), name)
  }
}
