package de.htwg_konstanz.se

import com.google.inject.{AbstractModule, Provider, Singleton}
import de.htwg_konstanz.se.controller.{GameController, IController}
import de.htwg_konstanz.se.io.{ISaveManager, JsonSaveManager, XmlSaveManager}
import de.htwg_konstanz.se.models.{Game, GameFactory}
import de.htwg_konstanz.se.ui.IPresenter
import de.htwg_konstanz.se.ui.gui.{GuiPresenter, GuiPresident}
import de.htwg_konstanz.se.ui.tui.{TuiPresenter, TuiReisen}
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
      case GuiMode.Gui => 
        bind[IPresenter].to[GuiPresenter]
        bind[GuiPresident].in[Singleton]()
      case GuiMode.Tui => 
        bind[IPresenter].to[TuiPresenter]
        bind[TuiReisen].in[Singleton]()
    }
  }
}

class GameProvider extends Provider[Game] {
  override def get(): Game = GameFactory.create(Seq.empty)
}
