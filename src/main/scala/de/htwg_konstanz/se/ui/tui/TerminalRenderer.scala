package de.htwg_konstanz.se.ui.tui

import org.jline.terminal.Terminal
import org.jline.utils.InfoCmp

enum TuiView {
  case Splash
  case MainMenu
}

case class TerminalRenderer(terminal: Terminal) {
  private var currentView: Option[TuiView] = None
  private var initialized = false

  def initialize(): Unit = {
    if initialized then return

    terminal.enterRawMode()
    terminal.puts(InfoCmp.Capability.virtual_terminal)
    terminal.flush()
    initialized = true
  }

  def transitionTo(view: TuiView, lines: Vector[String]): Unit = {
    if currentView.contains(view) then return
    render(lines)
    currentView = Some(view)
  }

  def render(lines: Vector[String]): Unit = {
    clear()

    val terminalHeight = terminal.getHeight
    val terminalWidth = terminal.getWidth
    val startY = math.max(0, terminalHeight / 2 - lines.length / 2)

    lines.zipWithIndex.foreach { case (line, index) =>
      val row = startY + index
      if row >= 0 && row < terminalHeight then
        val startX = terminalWidth / 2 - line.length / 2
        val (column, visiblePart) = clipLine(line, startX, terminalWidth)
        if visiblePart.nonEmpty then
          terminal.puts(InfoCmp.Capability.cursor_address, row, column)
          terminal.writer().print(visiblePart)
    }

    terminal.flush()
  }

  def clear(): Unit =
    terminal.puts(InfoCmp.Capability.cursor_address, 0, 0)
    terminal.puts(InfoCmp.Capability.clear_screen)

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
}
