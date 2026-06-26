package de.htwg_konstanz.se

import com.google.inject.Guice
import net.codingwell.scalaguice.InjectorExtensions.*
import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.ui.gui.GuiPresident
import de.htwg_konstanz.se.ui.tui.TuiReisen

@main def run(): Unit =
  val injector = Guice.createInjector(new PresidentModule())
  val controller = injector.instance[IController]

  val tui = injector.instance[TuiReisen]
  controller.add(tui)
  val tuiThread = new Thread(() => tui.run())
  tuiThread.setDaemon(true)
  tuiThread.start()

  val gui = injector.instance[GuiPresident]
  controller.add(gui)
  gui.main(Array.empty)