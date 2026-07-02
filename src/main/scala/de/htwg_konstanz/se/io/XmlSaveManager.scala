package de.htwg_konstanz.se.io

import de.htwg_konstanz.se.models.Game

import java.io.File
import java.nio.file.{Files, Paths}
import scala.util.Try
import scala.xml.{XML, NodeSeq}

case class XmlSaveManager() extends ISaveManager:
  override def save(game: Game, savePath: String): Try[Unit] =
    Try {
      var file = File(savePath)
      if file.isDirectory then file = Paths.get(file.getPath, "save.xml").toFile

      val parent = file.getParentFile
      if parent != null && !parent.exists() then parent.mkdirs()

      val xml = Game.toXml(game)
      XML.save(file.getAbsolutePath, xml, "UTF-8", true, null)
    }

  override def load(savePath: String): Try[Game] =
    Try {
      var file = File(savePath)
      if file.isDirectory then file = Paths.get(file.getPath, "save.xml").toFile

      if !file.exists() then throw NoSaveFileException(file.getAbsolutePath)

      val xml = XML.loadFile(file)
      Game.fromXml(xml)
    }

  override def deleteSave(savePath: String): Try[Unit] =
    Try {
      var file = File(savePath)
      if file.isDirectory then file = Paths.get(file.getPath, "save.xml").toFile

      Files.deleteIfExists(file.toPath)
    }
