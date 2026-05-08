package de.htwg_konstanz.se.ui.tui

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class ConsoleCanvasSpec extends AnyWordSpec {
  "A ConsoleCanvas" should {
    "render exactly terminal-sized lines" in {
      val canvas = ConsoleCanvas(5, 3)
      val lines = canvas.render()

      lines should have size 3
      all(lines.map(_.length)) should be(5)
    }

    "draw text and clip at boundaries" in {
      val canvas = ConsoleCanvas(5, 2)
      canvas.drawText(3, 0, "abcd")
      canvas.drawText(-1, 1, "xy")
      val lines = canvas.render()

      lines(0) should be("   ab")
      lines(1) should be("y    ")
    }

    "draw horizontal and vertical lines" in {
      val canvas = ConsoleCanvas(5, 5)
      canvas.drawHorizontalLine(1, 2, 3, '=')
      canvas.drawVerticalLine(3, 1, 3, '!')
      val lines = canvas.render()

      lines(2) should be(" ==! ")
      lines(1).charAt(3) should be('!')
      lines(3).charAt(3) should be('!')
    }

    "draw bordered boxes" in {
      val canvas = ConsoleCanvas(6, 4)
      canvas.drawBox(1, 1, 4, 3)
      val lines = canvas.render()

      lines(1).slice(1, 5) should be("┌──┐")
      lines(2).slice(1, 5) should be("│  │")
      lines(3).slice(1, 5) should be("└──┘")
    }

    "fill rectangles" in {
      val canvas = ConsoleCanvas(4, 3)
      canvas.fillRect(1, 1, 2, 2, '#')
      val lines = canvas.render()

      lines(0) should be("    ")
      lines(1) should be(" ## ")
      lines(2) should be(" ## ")
    }

    "draw render objects with left, centered and right alignment" in {
      val canvas = ConsoleCanvas(12, 3)
      canvas.drawRenderObj(RenderObj.Left(0, 0, Vector("L"), width = Some(3)))
      canvas.drawRenderObj(RenderObj.Centered(4, 0, Vector("C"), width = Some(3)))
      canvas.drawRenderObj(RenderObj.Right(8, 0, Vector("R"), width = Some(3)))

      val line = canvas.render().head
      line.slice(0, 3) should be("L  ")
      line.slice(4, 7) should be(" C ")
      line.slice(8, 11) should be("  R")
    }

    "clear the whole canvas" in {
      val canvas = ConsoleCanvas(4, 2)
      canvas.drawText(0, 0, "ABCD")
      canvas.clear('.')

      canvas.render() should be(Vector("....", "...."))
    }

    "compose a full frame from render objects via renderFrame helper" in {
      val frame = ConsoleCanvas.renderFrame(
        width = 6,
        height = 2,
        renderObjs = Vector(
          RenderObj.Left(0, 0, Vector("ABC")),
          RenderObj.Right(0, 1, Vector("Z"), width = Some(6))
        )
      )

      frame should be(Vector("ABC   ", "     Z"))
    }

    "handle invalid or minimal geometry edge cases safely" in {
      val canvas = ConsoleCanvas(-1, -1)
      canvas.drawText(0, 0, "x")
      canvas.drawHorizontalLine(0, -1, 5, '*')
      canvas.drawVerticalLine(-1, 0, 5, '*')
      canvas.drawBox(0, 0, 1, 1)
      canvas.drawBox(0, 0, 0, 0)
      canvas.fillRect(0, 0, 0, 0, '#')
      canvas.render() should be(Vector.empty)
    }

    "align lines using intrinsic width when explicit width is absent" in {
      val canvas = ConsoleCanvas(8, 1)
      canvas.drawRenderObj(RenderObj.Right(0, 0, Vector("abc"), width = None))
      canvas.render().head should startWith("abc")
    }
  }
}
