package de.htwg_konstanz.se

import com.google.inject.Guice
import de.htwg_konstanz.se.ui.gui.GuiPresident
import de.htwg_konstanz.se.ui.tui.TuiReisen
import net.codingwell.scalaguice.InjectorExtensions.*

enum GuiMode:
  case Gui, Tui, Both

enum SaveFormat:
  case Xml, Json

object President {
  def main(args: Array[String]): Unit = {
    val mode =
      if args contains "--gui" then GuiMode.Gui
      else if args contains "--tui" then GuiMode.Tui
      else GuiMode.Both

    val saveFormat =
      if args contains "--json" then SaveFormat.Json
      else if args contains "--xml" then SaveFormat.Xml
      else SaveFormat.Json

    println(mode)

    val injector = Guice.createInjector(new PresidentModule(mode, saveFormat))

    if (mode == GuiMode.Tui || mode == GuiMode.Both) {
      val tui = injector.instance[TuiReisen]
      if (mode == GuiMode.Both) {
        val thread = Thread(() => tui.run())
        thread.setDaemon(true)
        thread.setName("President - Tui")
        thread.start()
      } else {
        tui.run()
      }
    }

    if (mode == GuiMode.Gui || mode == GuiMode.Both) {
      val gui = injector.instance[GuiPresident]
      gui.main(Array.empty)
    }
  }
}
