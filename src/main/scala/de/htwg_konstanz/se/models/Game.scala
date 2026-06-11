package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState.{Aborted, Playing, WaitingForPlayers}

import java.util.UUID
import scala.util.{Failure, Success, Try}

case class Game(playerHands: Map[UUID, Vector[Card]], playedCards: Vector[Card], state: GameState) {
  def this() = {
    this(Map.empty, Vector.empty, state = WaitingForPlayers)
  }

  def join(playerId: UUID): Try[Game] = {
    // Only allow players to join outside of a running game
    if state != WaitingForPlayers then Failure(Exception("Cannot join a running game."))
    else if playerHands.contains(playerId) then Failure(Exception(s"The player with id $playerId is already part of the game."))
    else Success(this.copy(playerHands = playerHands + (playerId -> Vector())))
  }

  def leave(playerId: UUID): Try[Game] = {
    // TODO: handle cards returning to deck if quit during play is allowed
    if state != WaitingForPlayers then Failure(Exception("Cannot quit a running game."))
    else if !playerHands.contains(playerId) then Failure(Exception(s"The player with id $playerId is not part of the game."))
    else Success(this.copy(playerHands = playerHands - playerId))
  }

  def start(): Try[Game] = {
    // We can only start the game if we are in the lobby
    if state != WaitingForPlayers then return Failure(Exception(s"Can only start a new game when in lobby."))
    // only start the game if we have 2 or more players
    if playerHands.size < 2 then return Failure(Exception(s"Can only start a new game with two or more players."))
    
    
    
    return Success(this.copy(state = Playing))
  }

  def abort(): Try[Game] = {
    if state != Playing then return Failure[Game](Exception("Can only abort in playing state!"))

    Success[Game](this.copy(state = Aborted))
  }

  def deal(): Game = {
    this
  }

  def playCard(): Game = {
    this
  }
}

case class Deck(cards: Vector[Card]) {
  def this() = {
    this(Card.standardDeckCards)
  }

  def shuffle(): Deck = {
    copy(cards = scala.util.Random.shuffle(cards))
  }
}