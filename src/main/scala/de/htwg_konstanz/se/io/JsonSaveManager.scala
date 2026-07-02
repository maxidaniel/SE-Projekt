package de.htwg_konstanz.se.io

import de.htwg_konstanz.se.models.Game
import play.api.libs.json.Json

import java.io.File
import java.nio.file.{Files, Paths}
import scala.util.{Failure, Success, Try}

case class JsonSaveManager() extends ISaveManager:
  override def save(game: Game, savePath: String): Try[Unit] =
    Try {
      var file = File(savePath)
      if file.isDirectory then file = Paths.get(file.getPath, "save.json").toFile

      val parent = file.getParentFile
      if parent != null && !parent.exists() then parent.mkdirs()

      val json = Json.toJson(game)
      Files.writeString(file.toPath, Json.prettyPrint(json))
    }

  override def load(savePath: String): Try[Game] =
    Try {
      var file = File(savePath)
      if file.isDirectory then file = Paths.get(file.getPath, "save.json").toFile

      if !file.exists() then throw NoSaveFileException(file.getAbsolutePath)

      val content = Files.readString(file.toPath)
      val json = Json.parse(content)
      json.validate[Game].getOrElse(throw new Exception("Invalid save file format"))
    }

  override def deleteSave(savePath: String): Try[Unit] =
    Try {
      var file = File(savePath)
      if file.isDirectory then file = Paths.get(file.getPath, "save.json").toFile

      Files.deleteIfExists(file.toPath)
    }

case class NoSaveFileException(saveFile: String) extends Exception(s"Could not find the save file: $saveFile")
