package de.htwg_konstanz.se.io

import de.htwg_konstanz.se.models.Game

import scala.util.Try

trait ISaveManager():
  def save(game: Game, savePath: String): Try[Unit]
  def load(savePath: String): Try[Game]
  def deleteSave(savePath: String): Try[Unit]
