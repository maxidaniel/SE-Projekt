package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class CardSpec extends AnyWordSpec {
  "A Card" should {
    "have a string representation" in {
      Card.AceOfClubs.toString should be("A ♣")
    }

    "have an unknown representation" in {
      Card.Unknown.toString should be("? ?")
    }
  }
}
