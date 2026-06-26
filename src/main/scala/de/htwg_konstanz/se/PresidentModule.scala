package de.htwg_konstanz.se

import com.google.inject.AbstractModule
import de.htwg_konstanz.se.controller.{GameController, IController}
import de.htwg_konstanz.se.controller.strategies.{IStrategy, PlayLowestPossibleCardStrategy}
import de.htwg_konstanz.se.models.{Game, IGame}
import de.htwg_konstanz.se.ui.gui.{GuiPresident, PresidentViewModel}
import de.htwg_konstanz.se.ui.tui.TuiReisen
import com.google.inject.{Provider, Singleton}
import net.codingwell.scalaguice.ScalaModule

class PresidentModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind(classOf[IController]).to(classOf[GameController])
    bind(classOf[IGame]).to(classOf[Game])
    bind(classOf[IStrategy]).to(classOf[PlayLowestPossibleCardStrategy])
    bind(classOf[TuiReisen]).toProvider(new TuiReisenProvider)
    bind(classOf[PresidentViewModel]).toProvider(new PresidentViewModelProvider)
    bind(classOf[GuiPresident]).toProvider(new GuiPresidentProvider)
  }
}

class TuiReisenProvider extends Provider[TuiReisen] {
  @com.google.inject.Inject
  private var controller: IController = null
  
  def get(): TuiReisen = TuiReisen(controller)
}

class PresidentViewModelProvider extends Provider[PresidentViewModel] {
  @com.google.inject.Inject
  private var controller: IController = null
  
  def get(): PresidentViewModel = new PresidentViewModel(controller)
}

class GuiPresidentProvider extends Provider[GuiPresident] {
  @com.google.inject.Inject
  private var controller: IController = null
  
  def get(): GuiPresident = GuiPresident(controller)
}
