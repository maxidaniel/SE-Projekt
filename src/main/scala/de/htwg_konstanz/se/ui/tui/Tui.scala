package de.htwg_konstanz.se.ui.tui

import org.jline.terminal.Terminal.*
import org.jline.terminal.{Terminal, TerminalBuilder}
import org.jline.utils.InfoCmp

import scala.util.Try

// This class is going to be updated by a service in the future. Refactor it in such a way, that we call Service.register(Tui),
// which establishes event handling in the tui, and then call Service.run(), which then handles all game state.
case class Tui() extends AutoCloseable {
  private var shouldClose: Boolean = false

  private val terminal: Terminal = TerminalBuilder.builder().system(true).build()
  terminal.handle(Terminal.Signal.INT, (signal: Signal) => {
    terminal.writer().println("Exiting")
    terminal.flush()

    shouldClose = true
  })

  private def clear(): Unit = {
    if (!terminal.puts(InfoCmp.Capability.clear_screen)) {
      System.err.println("Failed to clear screen!")
    }
  }

  clear()

  terminal.enterRawMode()


  terminal.puts(InfoCmp.Capability.virtual_terminal)
  terminal.flush()

  def run(): Unit = {
    terminal.puts(InfoCmp.Capability.cursor_address, 0, 0)

    terminal.writer().println("Welcome to President, the totally not friend-group splitting game!")
    terminal.flush()

    while (!shouldClose) {
      Thread.sleep(100)
    }

    terminal.close()
  }

  override def close(): Unit = terminal.close()
}
