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

    "render unknown card placeholders" in {
      val rendered = Card.Unknown.render()
      rendered(1) should include("?")
      rendered(3) should include("?")
      rendered(5) should include("?")
    }

    "be renderable in 2D" in {
      val rendered = Card.AceOfClubs.render()
      rendered should have size 7
      rendered(0) should be("┌─────────┐")
      rendered(1) should be("│ A       │")
      rendered(3) should be("│    ♣    │")
      rendered(5) should be("│       A │")
      rendered(6) should be("└─────────┘")
    }

    "be renderable in 2D for 10" in {
      val rendered = Card.TenOfHearts.render()
      rendered(1) should be("│ 10      │")
      rendered(5) should be("│      10 │")
    }

    "be renderable in 2D with scale" in {
      val rendered = Card.AceOfClubs.render(1)
      rendered should have size 7
      rendered(0) should be("┌─────────┐")
    }

    "be renderable in 2D with scale 2" in {
      val rendered = Card.AceOfClubs.render(2)
      rendered should have size 13
      rendered(0) should be("┌───────────────────┐")
    }
  }
}
