package de.htwg_konstanz.se

import com.google.inject.Guice
import de.htwg_konstanz.se.ui.gui.GuiPresident
import de.htwg_konstanz.se.ui.tui.TuiReisen
import net.codingwell.scalaguice.InjectorExtensions.*

enum GuiMode:
  case Gui, Tui

enum SaveFormat:
  case Xml, Json

object President {
  def main(args: Array[String]): Unit = {
    println(args.mkString(", "))

    val mode =
      if args contains "--gui" then GuiMode.Gui
      else if args contains "--tui" then GuiMode.Tui
      else GuiMode.Gui

    val saveFormat =
      if args contains "--json" then SaveFormat.Json
      else if args contains "--xml" then SaveFormat.Xml
      else SaveFormat.Json

    val injector = Guice.createInjector(new PresidentModule(mode, saveFormat))

    mode match {
      case GuiMode.Gui =>
        val gui = injector.instance[GuiPresident]
        gui.main(Array.empty)

      case GuiMode.Tui =>
        val tui = injector.instance[TuiReisen]
        tui.run()
    }
  }
}
