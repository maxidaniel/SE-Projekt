package de.htwg_konstanz.se

import com.google.inject.Guice
import de.htwg_konstanz.se.ui.gui.GuiPresident
import de.htwg_konstanz.se.ui.tui.TuiReisen
import net.codingwell.scalaguice.InjectorExtensions.*

@main def run(): Unit =
  val injector = Guice.createInjector(new PresidentModule())
  
  val tui = injector.instance[TuiReisen]
  val tuiThread = new Thread(() => tui.run())
  tuiThread.setDaemon(true)
  tuiThread.start()

  val gui = injector.instance[GuiPresident]
  gui.main(Array.empty)