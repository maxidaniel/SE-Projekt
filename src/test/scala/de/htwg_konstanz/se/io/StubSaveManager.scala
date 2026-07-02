package de.htwg_konstanz.se.io

import de.htwg_konstanz.se.models.Game

import scala.util.{Failure, Success, Try}

class StubSaveManager extends ISaveManager:
  var lastSaved: Option[(Game, String)] = None
  var saveResult: Try[Unit] = Success(())
  var loadResult: Try[Game] = Failure(new Exception("No game loaded"))
  var deleteResult: Try[Unit] = Success(())

  override def save(game: Game, savePath: String): Try[Unit] =
    lastSaved = Some((game, savePath))
    saveResult

  override def load(savePath: String): Try[Game] = loadResult

  override def deleteSave(savePath: String): Try[Unit] = deleteResult
