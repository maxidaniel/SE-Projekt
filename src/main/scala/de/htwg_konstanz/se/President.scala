package de.htwg_konstanz.se

import com.google.inject.Guice
import net.codingwell.scalaguice.InjectorExtensions.*
import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.ui.gui.GuiPresident
import de.htwg_konstanz.se.ui.tui.TuiReisen

@main def run(args: String*): Unit =
  val argList = args.toList
  val hasTui = argList.contains("--tui")
  val hasGui = argList.contains("--gui")

  if (hasTui && hasGui) {
    Console.err.println("Error: --tui and --gui are mutually exclusive")
    Console.err.println("Usage: president [--tui | --gui]")
    System.exit(1)
  }

  val injector = Guice.createInjector(new PresidentModule())
  val controller = injector.instance[IController]

  if (!hasGui) {
    val tui = injector.instance[TuiReisen]
    controller.add(tui)
    val tuiThread = new Thread(() => tui.run())
    tuiThread.setDaemon(true)
    tuiThread.start()
  }

  if (!hasTui) {
    val gui = injector.instance[GuiPresident]
    controller.add(gui)
    gui.main(Array.empty)
  }