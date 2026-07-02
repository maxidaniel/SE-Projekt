package de.htwg_konstanz.se.ui.tui

import org.jline.terminal.Terminal
import org.jline.utils.{AttributedString, InfoCmp}

enum TuiView:
  case Splash, MainMenu, Playing

case class TerminalSize(columns: Int, rows: Int):
  def isSufficient: Boolean = columns >= 80 && rows >= 20

case class TerminalRenderer(terminal: Terminal):
  private var currentView: Option[TuiView] = None
  private var currentFrame: Vector[String] = Vector.empty
  private var initialized = false

  def initialize(): Unit =
    if initialized then return
    terminal.enterRawMode()
    terminal.puts(InfoCmp.Capability.virtual_terminal)
    terminal.puts(InfoCmp.Capability.enter_ca_mode)
    terminal.puts(InfoCmp.Capability.keypad_xmit)
    terminal.flush()
    initialized = true

  def size: TerminalSize = TerminalSize(terminal.getColumns, terminal.getRows)

  def transitionTo(view: TuiView, frame: Vector[String]): Unit =
    if currentView.contains(view) && currentFrame == frame then return
    currentFrame = frame
    render(frame)
    currentView = Some(view)

  def render(frame: Vector[String]): Unit =
    val termSize = size
    if !termSize.isSufficient then
      terminal.writer().println("Terminal size of 80x20 required")
      terminal.flush()
      return

    terminal.puts(InfoCmp.Capability.cursor_address, 0, 0)
    terminal.puts(InfoCmp.Capability.clear_screen)

    frame.zipWithIndex.foreach { (line, row) =>
      if row < termSize.rows then
        val visible = AttributedString
          .fromAnsi(line, terminal)
          .columnSubSequence(terminal, 0, termSize.columns)
          .toAnsi(terminal)
        terminal.puts(InfoCmp.Capability.cursor_address, row, 0)
        terminal.writer().print(visible)
    }
    terminal.flush()

  def windowSizeChanged(): Unit = render(currentFrame)

  def clear(): Unit =
    terminal.puts(InfoCmp.Capability.cursor_address, 0, 0)
    terminal.puts(InfoCmp.Capability.clear_screen)
    terminal.flush()

  def close(): Unit =
    terminal.puts(InfoCmp.Capability.keypad_local)
    terminal.puts(InfoCmp.Capability.exit_ca_mode)
    terminal.puts(InfoCmp.Capability.exit_attribute_mode)
    terminal.flush()
    terminal.close()
end TerminalRenderer
