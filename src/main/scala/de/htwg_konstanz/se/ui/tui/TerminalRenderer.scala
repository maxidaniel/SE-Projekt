package de.htwg_konstanz.se.ui.tui

import org.jline.terminal.Terminal
import org.jline.utils.InfoCmp

enum TuiView {
  case Splash
  case MainMenu
  case Playing
}

enum RenderAlignment {
  case Left
  case Centered
  case Right
}

// Represents an object rendered on terminal
case class RenderObj(
    x: Int,
    y: Int,
    lines: Vector[String],
    alignment: RenderAlignment = RenderAlignment.Left,
    width: Option[Int] = None
)

object RenderObj {
  def Left(x: Int, y: Int, lines: Vector[String], width: Option[Int] = None): RenderObj =
    RenderObj(x, y, lines, RenderAlignment.Left, width)

  def Centered(x: Int, y: Int, lines: Vector[String], width: Option[Int] = None): RenderObj =
    RenderObj(x, y, lines, RenderAlignment.Centered, width)

  def Right(x: Int, y: Int, lines: Vector[String], width: Option[Int] = None): RenderObj =
    RenderObj(x, y, lines, RenderAlignment.Right, width)
}

case class TerminalRenderer(terminal: Terminal) {
  private var currentView: Option[TuiView] = None
  private var currentRenderObjs: Seq[RenderObj] = Vector.empty
  private var initialized = false

  def initialize(): Unit = {
    if initialized then return

    terminal.enterRawMode()
    terminal.puts(InfoCmp.Capability.virtual_terminal)
    terminal.flush()
    initialized = true
  }

  def transitionTo(view: TuiView, renderObjs: Seq[RenderObj]): Unit = {
    if currentView.contains(view) && currentRenderObjs == renderObjs then return
    currentRenderObjs = renderObjs
    render(renderObjs)
    currentView = Some(view)
  }

  def render(renderObjs: Seq[RenderObj]): Unit = {
    clear()

    val terminalHeight = terminal.getHeight
    val terminalWidth = terminal.getWidth

    if terminalWidth < 80 then {
      terminal.writer().println("terminal size of 80x20 required")
      terminal.flush()
      return
    }

    if terminalHeight < 20 then {
      terminal.writer().println("terminal size of 80x20 required")
      terminal.flush()
      return
    }

    renderObjs.foreach(renderObj => renderObject(renderObj, terminalWidth, terminalHeight))

    terminal.flush()
  }

  def windowSizeChanged(): Unit = {
    render(currentRenderObjs)
  }

  def clear(): Unit = {
    terminal.puts(InfoCmp.Capability.cursor_address, 0, 0)
    terminal.puts(InfoCmp.Capability.clear_screen)
  }

  def close(): Unit = terminal.close()

  private def clipLine(line: String, x: Int, terminalWidth: Int): (Int, String) = {
    if terminalWidth <= 0 || x >= terminalWidth || x + line.length <= 0 then
      return (0, "")

    val from = math.max(0, -x)
    val until = math.min(line.length, terminalWidth - x)
    val clipped = line.slice(from, until)
    val column = math.max(0, x)
    (column, clipped)
  }

  private def renderObject(renderObj: RenderObj, terminalWidth: Int, terminalHeight: Int): Unit = {
    val alignWidth = renderObj.width.getOrElse(renderObj.lines.map(_.length).maxOption.getOrElse(0))

    renderObj.lines.zipWithIndex.foreach { case (line, lineIndex) =>
      val row = renderObj.y + lineIndex
      if row >= 0 && row < terminalHeight then
        val alignedLine = alignLine(line, renderObj.alignment, alignWidth)
        val (column, visiblePart) = clipLine(alignedLine, renderObj.x, terminalWidth)
        if visiblePart.nonEmpty then
          terminal.puts(InfoCmp.Capability.cursor_address, row, column)
          terminal.writer().print(visiblePart)
    }
  }

  private def alignLine(line: String, alignment: RenderAlignment, width: Int): String = {
    if width <= line.length then return line

    val missing = width - line.length

    alignment match {
      case RenderAlignment.Left =>
        line + (" " * missing)

      case RenderAlignment.Right =>
        (" " * missing) + line

      case RenderAlignment.Centered =>
        val leftPad = missing / 2
        val rightPad = missing - leftPad
        (" " * leftPad) + line + (" " * rightPad)
    }
  }
}
