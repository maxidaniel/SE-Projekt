package de.htwg_konstanz.se.ui.tui

import org.jline.terminal.Terminal
import org.jline.utils.InfoCmp
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.io.{PrintWriter, StringWriter}
import java.lang.reflect.{InvocationHandler, Method, Proxy}

class TerminalRendererSpec extends AnyWordSpec {
  private case class TerminalState(
      var width: Int = 80,
      var height: Int = 24,
      writerBuffer: StringWriter = StringWriter(),
      var putsCalls: Vector[Vector[AnyRef]] = Vector.empty,
      var flushCount: Int = 0,
      var rawModeCount: Int = 0,
      var closeCount: Int = 0
  ) {
    val writer: PrintWriter = PrintWriter(writerBuffer)
  }

  private def fakeTerminal(state: TerminalState): Terminal = {
    val handler = new InvocationHandler {
      override def invoke(proxy: Any, method: Method, args: Array[AnyRef] | Null): AnyRef = {
        val name = method.getName
        val params = Option(args).map(_.toVector).getOrElse(Vector.empty)

        name match {
          case "getWidth" => Integer.valueOf(state.width)
          case "getHeight" => Integer.valueOf(state.height)
          case "writer" => state.writer
          case "flush" =>
            state.flushCount += 1
            null
          case "puts" =>
            state.putsCalls = state.putsCalls :+ params
            java.lang.Boolean.TRUE
          case "enterRawMode" =>
            state.rawModeCount += 1
            null
          case "close" =>
            state.closeCount += 1
            null
          case "toString" => "FakeTerminal"
          case _ =>
            method.getReturnType match {
              case java.lang.Boolean.TYPE => java.lang.Boolean.FALSE
              case java.lang.Integer.TYPE => Integer.valueOf(0)
              case java.lang.Long.TYPE => java.lang.Long.valueOf(0L)
              case java.lang.Double.TYPE => java.lang.Double.valueOf(0d)
              case java.lang.Float.TYPE => java.lang.Float.valueOf(0f)
              case java.lang.Short.TYPE => java.lang.Short.valueOf(0.toShort)
              case java.lang.Byte.TYPE => java.lang.Byte.valueOf(0.toByte)
              case java.lang.Character.TYPE => java.lang.Character.valueOf('\u0000')
              case _ => null
            }
        }
      }
    }

    Proxy
      .newProxyInstance(
        classOf[Terminal].getClassLoader,
        Array(classOf[Terminal]),
        handler
      )
      .asInstanceOf[Terminal]
  }

  "A TerminalRenderer" should {
    "initialize only once" in {
      val state = TerminalState()
      val renderer = TerminalRenderer(fakeTerminal(state))

      renderer.initialize()
      renderer.initialize()

      state.rawModeCount should be(1)
      state.putsCalls.exists(_.headOption.contains(InfoCmp.Capability.virtual_terminal)) should be(true)
      state.flushCount should be >= 1
    }

    "skip duplicate transitions to same view and same frame" in {
      val state = TerminalState()
      val renderer = TerminalRenderer(fakeTerminal(state))
      val frame = Vector("hello")

      renderer.transitionTo(TuiView.MainMenu, frame)
      val putsAfterFirst = state.putsCalls.size

      renderer.transitionTo(TuiView.MainMenu, frame)

      state.putsCalls.size should be(putsAfterFirst)
    }

    "print a warning when terminal width is too small" in {
      val state = TerminalState(width = 79, height = 24)
      val renderer = TerminalRenderer(fakeTerminal(state))

      renderer.render(Vector("line"))

      state.writer.flush()
      state.writerBuffer.toString should include("terminal size of 80x20 required")
    }

    "print a warning when terminal height is too small" in {
      val state = TerminalState(width = 80, height = 19)
      val renderer = TerminalRenderer(fakeTerminal(state))

      renderer.render(Vector("line"))

      state.writer.flush()
      state.writerBuffer.toString should include("terminal size of 80x20 required")
    }

    "render clipped lines and respect terminal height" in {
      val state = TerminalState(width = 80, height = 20)
      val renderer = TerminalRenderer(fakeTerminal(state))

      val longLine = "x" * 100
      val manyLines = Vector.fill(25)("row")
      renderer.render(Vector(longLine) ++ manyLines)

      state.writer.flush()
      state.writerBuffer.toString should include("x" * 80)
      state.putsCalls.count(_.headOption.contains(InfoCmp.Capability.cursor_address)) should be(21)
    }
    
    "rerender on window size changes using last frame" in {
      val state = TerminalState(width = 80, height = 24)
      val renderer = TerminalRenderer(fakeTerminal(state))
      renderer.transitionTo(TuiView.Playing, Vector("frame-1"))
      val putsAfterTransition = state.putsCalls.size

      renderer.windowSizeChanged()

      state.putsCalls.size should be > putsAfterTransition
    }

    "clear and close terminal" in {
      val state = TerminalState(width = 80, height = 24)
      val renderer = TerminalRenderer(fakeTerminal(state))

      renderer.clear()
      renderer.close()

      state.putsCalls.exists(_.headOption.contains(InfoCmp.Capability.clear_screen)) should be(true)
      state.closeCount should be(1)
    }
  }
}
