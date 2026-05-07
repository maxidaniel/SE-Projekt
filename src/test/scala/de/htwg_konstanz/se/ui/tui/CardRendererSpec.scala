package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.models.Card
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class CardRendererSpec extends AnyWordSpec {
  "A CardRenderer" should {
    "render with requested user scale if terminal allows it" in {
      val renderer = CardRenderer()
      val result = renderer.render(
        cards = Seq(Card.AceOfSpades),
        terminalWidth = 80,
        terminalHeight = 30,
        options = CardRendererOptions(userScale = 2)
      )

      result.scale should be(2)
      result.lines.head should be("┌───────────────────┐")
    }

    "overlap cards" in {
      val renderer = CardRenderer()
      val result = renderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 80,
        terminalHeight = 30
      )

      val topLine = result.lines.head
      topLine.count(_ == '┌') should be(2)
      topLine.length should be < 22
    }

    "reduce scale to fit terminal size" in {
      val renderer = CardRenderer()
      val result = renderer.render(
        cards = Seq(Card.AceOfSpades),
        terminalWidth = 11,
        terminalHeight = 7,
        options = CardRendererOptions(userScale = 3)
      )

      result.scale should be(1)
      result.lines.head should be("┌─────────┐")
    }

    "clip output to tiny terminal dimensions" in {
      val renderer = CardRenderer()
      val result = renderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 6,
        terminalHeight = 4,
        options = CardRendererOptions(userScale = 2)
      )

      result.lines.length should be <= 4
      all(result.lines.map(_.length)) should be <= 6
    }

    "return an empty render for invalid dimensions or empty cards" in {
      val renderer = CardRenderer()

      renderer.render(Seq.empty, terminalWidth = 80, terminalHeight = 24).lines should be(Vector.empty)
      renderer.render(Seq(Card.AceOfSpades), terminalWidth = 0, terminalHeight = 24).lines should be(Vector.empty)
      renderer.render(Seq(Card.AceOfSpades), terminalWidth = 80, terminalHeight = 0).lines should be(Vector.empty)
    }

    "force minimum step when terminal is narrower than a card" in {
      val renderer = CardRenderer()
      val result = renderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 5,
        terminalHeight = 10
      )

      result.step should be(1)
      result.lines should not be empty
    }
  }
}
