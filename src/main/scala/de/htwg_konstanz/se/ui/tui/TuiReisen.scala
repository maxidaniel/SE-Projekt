package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.models.Card.{TenOfSpades, Unknown}
import org.jline.terminal.Terminal.*
import org.jline.terminal.{Terminal, TerminalBuilder}

// This class is going to be updated by a service in the future. Refactor it in such a way, that we call Service.register(Tui),
// which establishes event handling in the tui, and then call Service.run(), which then handles all game state.
case class TuiReisen() {
  private val logo: Vector[String] = Vector(
    ".------..------..------..------..------..------..------..------..------.",
    "|P.--. ||R.--. ||E.--. ||S.--. ||I.--. ||D.--. ||E.--. ||N.--. ||T.--. |",
    "| :/\\: || :(): || (\\/) || :/\\: || (\\/) || :/\\: || (\\/) || :(): || :/\\: |",
    "| (__) || ()() || :\\/: || :\\/: || :\\/: || (__) || :\\/: || ()() || (__) |",
    "| '--'P|| '--'R|| '--'E|| '--'S|| '--'I|| '--'D|| '--'E|| '--'N|| '--'T|",
    "`------'`------'`------'`------'`------'`------'`------'`------'`------'"
  )

  private var shouldClose: Boolean = false

  private val terminal: Terminal = TerminalBuilder.builder().system(true).build()
  private val renderer: TerminalRenderer = TerminalRenderer(terminal)

  terminal.handle(Terminal.Signal.INT, (signal: Signal) => {
    terminal.writer().println("Exiting")
    terminal.flush()

    shouldClose = true
  })

  renderer.initialize()

  def run(): Unit = {
    renderer.transitionTo(TuiView.Splash, logo)
    Thread.sleep(2500)
    renderer.transitionTo(TuiView.MainMenu, Vector("Press Ctrl+C to exit."))

    while (!shouldClose) {
      val c = CardRenderer();
      val rendered = c.render(Vector(TenOfSpades, TenOfSpades, TenOfSpades, TenOfSpades), terminal.getWidth, terminal.getHeight)
      renderer.transitionTo(TuiView.Splash, rendered.lines)
      Thread.sleep(100)
    }

    renderer.close()
  }
}
