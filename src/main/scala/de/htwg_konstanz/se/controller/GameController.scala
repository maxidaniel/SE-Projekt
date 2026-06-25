package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.util.{Command, Provider, UndoManager}

import java.util.UUID
import scala.compiletime.uninitialized
import scala.util.{Failure, Success}

trait IController {
  
}

class GameController(private var game: Game) extends IController, Provider {
  private var undoManager = UndoManager()

  def this() = {
    this(GameFactory.create(Seq.empty))
  }

  def join(name: String): Unit = {
    val player = new Player(name)
    undoManager = undoManager.doStep(new JoinCommand(game, player))
  }

  def quit(uuid: UUID): Unit = {
    val player = getPlayer(uuid)
    if player.isEmpty then {
      notifyEvent(GameErrorEvent(PlayerQuitEvent(Player(uuid, "Unknown"), game), Failure(new Exception(s"The player with id $uuid is not part of the game."))))
    } else {
      undoManager = undoManager.doStep(new QuitCommand(game, player.get))
    }
  }

  def start(): Unit = {
    undoManager = undoManager.doStep(new StartCommand(game))
  }

  def playCard(player: Player, card: Card): Unit = {
    undoManager = undoManager.doStep(new PlayCardCommand(game, player, card))
  }

  def getPlayer(name: String): Option[Player] = {
    game.playerNames.find { case (_, n) => n == name }.map { case (id, _) => Player(id, name) }
  }

  def getPlayer(uuid: UUID): Option[Player] = {
    game.playerNames.get(uuid).map(name => Player(uuid, name))
  }

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

  private class JoinCommand(gameRef: Game, player: Player) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.join(player.id) match {
        case Success(g) =>
          val gWithNames = g.withPlayerName(player.id, player.name)
          game = gWithNames
          newGame = gWithNames
          notifyEvent(PlayerJoinEvent(player, gWithNames))
        case Failure(f) =>
          notifyEvent(GameErrorEvent(PlayerJoinEvent(player, game), Failure(f)))
      }
    }

    override def undoStep(): Unit = {
      game = oldGame
      notifyEvent(GameChangedEvent(game))
    }

    override def redoStep(): Unit = {
      game = newGame
      notifyEvent(PlayerJoinEvent(player, game))
    }
  }

  private class QuitCommand(gameRef: Game, player: Player) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.quit(player.id) match {
        case Success(g) =>
          val gWithoutName = g.withoutPlayerName(player.id)
          game = gWithoutName
          newGame = gWithoutName
          notifyEvent(PlayerQuitEvent(player, gWithoutName))
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

  private class StartCommand(gameRef: Game) extends Command {
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

  private class PlayCardCommand(gameRef: Game, player: Player, card: Card) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.playCard(player.id, card) match {
        case Success(g) =>
          game = g
          newGame = g
          if (g.state == EndedState) {
            notifyEvent(GameEndedEvent(g, player))
          } else {
            notifyEvent(CardPlayedEvent(player, card, game))
          }
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

  def reset(): Unit = {
    val playerNameIds = game.playerNames.map { case (id, name) => name -> id }.toMap
    game = GameFactory.create(playerNameIds)
    notifyEvent(GameChangedEvent(game))
  }

  def exit(): Unit = notifyEvent(GameExitEvent)

  def getGame: Game = game

  def getGameState: GameState = game.state
}
