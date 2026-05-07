package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.models.Card
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class CardRendererSpec extends AnyWordSpec {
  "A CardRenderer" should {
    "render with requested user scale if terminal allows it" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades),
        terminalWidth = 80,
        terminalHeight = 30,
        options = CardRendererOptions(userScale = 2)
      )

      result.scale should be(2)
      result.lines.head should be("┌───────────────────┐")
    }

    "overlap cards" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 80,
        terminalHeight = 30
      )

      val topLine = result.lines.head
      topLine.count(_ == '┌') should be(2)
      topLine.length should be < 22
    }

    "reduce scale to fit terminal size" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades),
        terminalWidth = 11,
        terminalHeight = 7,
        options = CardRendererOptions(userScale = 3)
      )

      result.scale should be(1)
      result.lines.head should be("┌─────────┐")
    }

    "clip output to tiny terminal dimensions" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 6,
        terminalHeight = 4,
        options = CardRendererOptions(userScale = 2)
      )

      result.lines.length should be <= 4
      all(result.lines.map(_.length)) should be <= 6
    }

    "return an empty render for invalid dimensions or empty cards" in {
      CardRenderer.render(Seq.empty, terminalWidth = 80, terminalHeight = 24).lines should be(Vector.empty)
      CardRenderer.render(Seq(Card.AceOfSpades), terminalWidth = 0, terminalHeight = 24).lines should be(Vector.empty)
      CardRenderer.render(Seq(Card.AceOfSpades), terminalWidth = 80, terminalHeight = 0).lines should be(Vector.empty)
    }

    "force minimum step when terminal is narrower than a card" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 5,
        terminalHeight = 10
      )

      result.step should be(1)
      result.lines should not be empty
    }

    "clamp non-positive user scale to 1" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades),
        terminalWidth = 80,
        terminalHeight = 30,
        options = CardRendererOptions(userScale = 0)
      )

      result.scale should be(1)
    }

    "compute centered offsets when terminal is larger than render target" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades),
        terminalWidth = 40,
        terminalHeight = 20
      )

      result.offsetX should be(14)
      result.offsetY should be(6)
    }

    "reduce horizontal step to fit all cards in constrained width" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts, Card.QueenOfClubs),
        terminalWidth = 13,
        terminalHeight = 20
      )

      result.step should be(1)
      result.lines.head.length should be <= 13
    }

    "treat negative overlapColumns as zero overlap" in {
      val noOverlap = CardRenderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 80,
        terminalHeight = 20,
        options = CardRendererOptions(overlapColumns = 0)
      )
      val negativeOverlap = CardRenderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 80,
        terminalHeight = 20,
        options = CardRendererOptions(overlapColumns = -10)
      )

      negativeOverlap.step should be(noOverlap.step)
      negativeOverlap.lines should be(noOverlap.lines)
    }

    "allow full overlap with very large overlapColumns" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades, Card.KingOfHearts),
        terminalWidth = 80,
        terminalHeight = 20,
        options = CardRendererOptions(overlapColumns = 999)
      )

      result.step should be(1)
      result.lines.head.count(_ == '┌') should be(2)
    }

    "fallback to scale 1 if terminal height is smaller than any card scale" in {
      val result = CardRenderer.render(
        cards = Seq(Card.AceOfSpades),
        terminalWidth = 80,
        terminalHeight = 1,
        options = CardRendererOptions(userScale = 5)
      )

      result.scale should be(1)
      result.lines.length should be(1)
    }
  }
}
