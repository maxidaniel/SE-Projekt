package de.htwg_konstanz.se

import com.google.inject.AbstractModule
import de.htwg_konstanz.se.controller.{GameController, IController}
import de.htwg_konstanz.se.models.IPlayer
import net.codingwell.scalaguice.ScalaModule

class PresidentModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind(classOf[IController]).to(classOf[GameController])
  }
}
