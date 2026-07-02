package de.htwg_konstanz.se

import com.google.inject.Guice
import de.htwg_konstanz.se.ui.gui.GuiPresident
import de.htwg_konstanz.se.ui.tui.TuiReisen
import net.codingwell.scalaguice.InjectorExtensions.*

import scala.util.CommandLineParser
import scala.util.CommandLineParser.FromString

enum GuiMode:
  case Gui, Tui

enum SaveFormat:
  case Xml, Json

given CommandLineParser.FromString[GuiMode] with
  override def fromString(s: String): GuiMode = GuiMode.valueOf(s)

given CommandLineParser.FromString[SaveFormat] with
  override def fromString(s: String): SaveFormat = SaveFormat.valueOf(s)

  override def fromStringOption(s: String): Option[SaveFormat] = super.fromStringOption(s)

object President {
  def main(args: Array[String]): Unit = {

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
