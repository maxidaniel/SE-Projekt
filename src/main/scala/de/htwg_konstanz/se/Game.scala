package de.htwg_konstanz.se

import scala.util.Random

case class Game(players: Vector[Player], cards: Vector[Card]) {
  def shuffle(): Game = {
    copy(cards = Random.shuffle(cards))
  }
}
