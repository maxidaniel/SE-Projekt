package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class PlayerSpec extends AnyWordSpec {
  "A player" should {
    "be created with a name" in {
      val player = new Player("Test Player")
      player.name should be("Test Player")
    }

    "be created with a name, and id" in {
      val id = UUID.randomUUID()
      val player = new Player(id, "Test Player")
      player.id should be(id)
      player.name should be("Test Player")
    }
  }

}
