package de.htwg_konstanz.se.io

import de.htwg_konstanz.se.models.Game

import scala.util.Try

case class JsonSaveManager() extends ISaveManager {
  override def save(game: Game, savePath: String): Try[Unit] = ???

  override def load(savePath: String): Try[Game] = ???

  override def deleteSave(savePath: String): Try[Unit] = ???
}
