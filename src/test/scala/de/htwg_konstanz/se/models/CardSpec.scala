package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.ui.tui.CardRenderer
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

    "scale dimensions consistently for higher scales" in {
      val rendered = Card.AceOfClubs.render(3)
      rendered should have size 19
      rendered.head.length should be(31)
      rendered.last.length should be(31)
      rendered.count(_.contains("♣")) should be(1)
    }

    "keep rank padding stable for one-digit and two-digit ranks" in {
      val ace = Card.AceOfSpades.render()
      val ten = Card.TenOfSpades.render()

      ace(1) should startWith("│ A ")
      ace(5) should endWith(" A │")
      ten(1) should startWith("│ 10")
      ten(5) should endWith("10 │")
    }

    "produce symmetric top and bottom frame widths for all tested scales" in {
      for scale <- 1 to 4 do
        val rendered = Card.QueenOfDiamonds.render(scale)
        rendered.head.length should be(rendered.last.length)
        rendered.head.head should be('┌')
        rendered.last.head should be('└')
        rendered.head.last should be('┐')
        rendered.last.last should be('┘')
    }
  }
}
