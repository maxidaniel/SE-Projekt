package de.htwg_konstanz.se

import com.google.inject.{AbstractModule, Provider, Singleton}
import de.htwg_konstanz.se.controller.{GameController, IController}
import de.htwg_konstanz.se.io.{ISaveManager, JsonSaveManager, XmlSaveManager}
import de.htwg_konstanz.se.models.{Game, GameFactory}
import de.htwg_konstanz.se.ui.gui.GuiPresident
import de.htwg_konstanz.se.ui.tui.TuiReisen
import de.htwg_konstanz.se.util.{IUndoManager, UndoManager}
import net.codingwell.scalaguice.ScalaModule

class PresidentModule(val guiMode: GuiMode, val saveFormat: SaveFormat) extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[IUndoManager].to[UndoManager]

    bind[ISaveManager].to(saveFormat match {
      case SaveFormat.Json => classOf[JsonSaveManager]
      case SaveFormat.Xml  => classOf[XmlSaveManager]
    })

    bind[Game].toProvider[GameProvider]

    bind[IController].to[GameController].in[Singleton]()

    guiMode match {
      case GuiMode.Gui => bind(classOf[GuiPresident])
      case GuiMode.Tui => bind(classOf[TuiReisen])
    }
  }
}

class GameProvider extends Provider[Game] {
  override def get(): Game = GameFactory.create(Seq.empty)
}
