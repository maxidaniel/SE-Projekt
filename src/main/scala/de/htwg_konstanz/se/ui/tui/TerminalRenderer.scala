package de.htwg_konstanz.se.ui.tui

import org.jline.terminal.Terminal
import org.jline.utils.InfoCmp

enum TuiView {
  case Splash
  case MainMenu
  case Playing
}

case class TerminalRenderer(terminal: Terminal) {
  private var currentView: Option[TuiView] = None
  private var currentFrame: Vector[String] = Vector.empty
  private var initialized = false

  def initialize(): Unit = {
    if initialized then return

    terminal.enterRawMode()
    terminal.puts(InfoCmp.Capability.virtual_terminal)
    terminal.flush()
    initialized = true
  }

  def transitionTo(view: TuiView, frame: Vector[String]): Unit = {
    if currentView.contains(view) && currentFrame == frame then return
    currentFrame = frame
    render(frame)
    currentView = Some(view)
  }

  def render(frame: Vector[String]): Unit = {
    clear()

    val terminalHeight = terminal.getRows
    val terminalWidth = terminal.getColumns

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

    frame.zipWithIndex.foreach { case (line, row) =>
      if row < terminalHeight then
        val visible = if line.length > terminalWidth then line.take(terminalWidth) else line
        terminal.puts(InfoCmp.Capability.cursor_address, row, 0)
        terminal.writer().print(visible)
    }

    terminal.flush()
  }

  def windowSizeChanged(): Unit = {
    render(currentFrame)
  }

  def clear(): Unit = {
    terminal.puts(InfoCmp.Capability.cursor_address, 0, 0)
    terminal.puts(InfoCmp.Capability.clear_screen)
  }

  def close(): Unit = terminal.close()

}
