package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.util.{Command, Provider, UndoManager}

import java.util.UUID
import scala.compiletime.uninitialized
import scala.util.{Failure, Success}

class GameController(private var game: Game, private var players: Seq[Player]) extends Provider {
  private var undoManager = UndoManager()

  def this() = {
    this(new Game(), Seq.empty)
  }

  def join(name: String): Unit = {
    undoManager = undoManager.doStep(new JoinCommand(new Player(name)))
  }

  def quit(uuid: UUID): Unit = {
    val player = players.find(p => p.id == uuid)
    if player.isEmpty then return
    undoManager = undoManager.doStep(new QuitCommand(player.get))
  }

  def start(): Unit = {
    undoManager = undoManager.doStep(new StartCommand())
  }

  def playCard(player: Player, card: Card): Unit = {
    undoManager = undoManager.doStep(new PlayCardCommand(player, card))
  }

  def getPlayer(name: String): Option[Player] = players.find(p => p.name == name)

  def getPlayer(uuid: UUID): Option[Player] = players.find(p => p.id == uuid)

  def abort(): Unit = {
    game.abort() match {
      case Success(g) =>
        game = g
        notifyEvent(GameAbortedEvent(game))
      case Failure(f) =>
    }
  }

  def undo(): Unit = {
    undoManager = undoManager.undoStep()
  }

  def redo(): Unit = {
    undoManager = undoManager.redoStep()
  }

  private class JoinCommand(player: Player) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.join(player.id) match {
        case Success(g) =>
          game = g
          newGame = g
          players = players :+ player
          notifyEvent(PlayerJoinEvent(player, game))
        case Failure(f) =>
          notifyEvent(GameErrorEvent(PlayerJoinEvent(player, game), Failure(f)))
      }
    }

    override def undoStep(): Unit = {
      game = oldGame
      players = players.filterNot(p => p.id == player.id)
      notifyEvent(GameChangedEvent(game))
    }

    override def redoStep(): Unit = {
      game = newGame
      notifyEvent(PlayerJoinEvent(player, game))
    }
  }

  private class QuitCommand(player: Player) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.quit(player.id) match {
        case Success(g) =>
          game = g
          newGame = g
          notifyEvent(PlayerQuitEvent(player, game))
        case Failure(f) =>
          notifyEvent(GameErrorEvent(PlayerQuitEvent(player, game), Failure(f)))
      }
    }

    override def undoStep(): Unit = {
      game = oldGame
      notifyEvent(GameChangedEvent(game))
    }

    override def redoStep(): Unit = {
      game = newGame
      notifyEvent(PlayerQuitEvent(player, game))
    }
  }

  private class StartCommand() extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.start() match {
        case Success(g) =>
          game = g
          newGame = g
          notifyEvent(GameStartedEvent(game))
        case Failure(f) =>
          notifyEvent(GameErrorEvent(GameStartedEvent(game), Failure(f)))
      }
    }

    override def undoStep(): Unit = {
      game = oldGame
      notifyEvent(GameChangedEvent(game))
    }

    override def redoStep(): Unit = {
      game = newGame
      notifyEvent(GameStartedEvent(game))
    }
  }

  private class PlayCardCommand(player: Player, card: Card) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.playCard(player.id, card) match {
        case Success(g) =>
          game = g
          newGame = g
          notifyEvent(CardPlayedEvent(player, card, game))
        case Failure(f) =>
          notifyEvent(GameErrorEvent(CardPlayedEvent(player, card, game), Failure(f)))
      }
    }

    override def undoStep(): Unit = {
      game = oldGame
      notifyEvent(GameChangedEvent(game))
    }

    override def redoStep(): Unit = {
      game = newGame
      notifyEvent(CardPlayedEvent(player, card, game))
    }
  }

  def exit(): Unit = notifyEvent(GameExitEvent)

  def getGame: Game = game

  def getGameState: GameState = game.state
}
