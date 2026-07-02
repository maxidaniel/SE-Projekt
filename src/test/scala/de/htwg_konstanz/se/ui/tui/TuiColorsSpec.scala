package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.models.Card
import org.jline.utils.AttributedString
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

class TuiColorsSpec extends AnyWordSpec:
  "TuiColors" should {
    "emit ANSI escape sequences for styled strings" in {
      val styled = TuiColors.success("ok")

      styled should include("\u001b[")
      AttributedString.stripAnsi(styled) should be("ok")
    }

    "preserve plain text for non-colored card suits" in {
      Card.AceOfSpades.cardText should be("A ♠")
    }

    "emit ANSI escape sequences for red card suits" in {
      val hearts = Card.AceOfHearts.cardText

      hearts should include("\u001b[")
      AttributedString.stripAnsi(hearts) should be("A ♥")
    }
  }
