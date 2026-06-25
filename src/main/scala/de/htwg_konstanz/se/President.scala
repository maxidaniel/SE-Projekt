package de.htwg_konstanz.se

import com.google.inject.Guice
import net.codingwell.scalaguice.InjectorExtensions.*
import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.ui.gui.GuiPresident
import de.htwg_konstanz.se.ui.tui.TuiReisen

@main def run(): Unit =
  val injector = Guice.createInjector(new PresidentModule())

  val controller = injector.instance[GameController]

  val tui = TuiReisen(controller)
  controller.add(tui)

  val tuiThread = new Thread(() => tui.run())
  tuiThread.setDaemon(true)
  tuiThread.start()

  GuiPresident(controller).main(Array.empty)