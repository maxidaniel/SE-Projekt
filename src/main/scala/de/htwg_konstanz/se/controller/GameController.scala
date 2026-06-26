package de.htwg_konstanz.se.controller

import com.google.inject.{Inject, Singleton}
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.models.PlayerType.Computer
import de.htwg_konstanz.se.util.{Command, Provider, UndoManager}

import java.util.UUID
import scala.compiletime.uninitialized
import scala.util.{Failure, Success}

trait IController extends Provider {
  def join(name: String): Unit
  def quit(uuid: UUID): Unit
  def start(): Unit
  
  def playCard(player: IPlayer): Unit
  def playCard(player: IPlayer, card: Card): Unit
  def playCard(player: IPlayer, index: Int): Unit
  
  def passTrick(player: IPlayer): Unit
  def abort(): Unit
  def undo(): Unit
  def redo(): Unit
  def reset(): Unit
  def exit(): Unit
  def getGame: Game
  def getGameState: GameState

  def getPlayer(name: String): Option[IPlayer]
  def getPlayer(uuid: UUID): Option[IPlayer]
  def players: Seq[IPlayer]
  def playerCount: Int
}

@Singleton
class GameController(@Inject private var game: Game) extends IController {
  private var undoManager = UndoManager()

  def this() = {
    this(GameFactory.create(Seq.empty))
  }

  def join(name: String): Unit = {
    val player = HumanPlayer(name)
    undoManager = undoManager.doStep(new JoinCommand(game, player))
  }

  def quit(uuid: UUID): Unit = {
    val player = getPlayer(uuid)
    if player.isEmpty then {
      notifyEvent(GameErrorEvent(PlayerQuitEvent(UnknownPlayer(), game), Failure(new Exception(s"The player with id $uuid is not part of the game."))))
    } else {
      undoManager = undoManager.doStep(new QuitCommand(game, player.get))
    }
  }

  def start(): Unit = {
    undoManager = undoManager.doStep(new StartCommand(game))
  }

  def playCard(player: IPlayer, card: Card): Unit = {
    undoManager = undoManager.doStep(new PlayCardCommand(game, player, card))
  }

  def playCard(player: IPlayer, index: Int): Unit = {
    game.playerHands.get(player) match {
      case Some(hand) if index >= 0 && index < hand.size =>
        val card = hand(index)
        playCard(player, card)
      case _ =>
        notifyEvent(GameErrorEvent(
          CardPlayedEvent(player, Card.Unknown, game),
          Failure(new Exception(s"Invalid card index $index"))
        ))
    }
  }

  def playCard(player: IPlayer): Unit = {
    game.playerHands.get(player) match {
      case Some(hand) =>
        game.currentPlayer match {
          case Some(currentId) if currentId == player.id =>
            val lastPlayed = game.playedCards.lastOption
            player.playerType match {
              case Computer(strategy) =>
                val card = strategy.play(hand, lastPlayed.getOrElse(Card.ThreeOfHearts))
                playCard(player, card)
              case _ =>
                notifyEvent(GameErrorEvent(
                  CardPlayedEvent(player, Card.Unknown, game),
                  Failure(new Exception("Only computer players can use playCard(IPlayer)"))
                ))
            }
          case _ =>
            notifyEvent(GameErrorEvent(
              CardPlayedEvent(player, Card.Unknown, game),
              Failure(new Exception("Not this player's turn"))
            ))
        }
      case None =>
        notifyEvent(GameErrorEvent(
          CardPlayedEvent(player, Card.Unknown, game),
          Failure(new Exception("Player has no cards"))
        ))
    }
  }

  def passTrick(player: IPlayer): Unit = {
    game.passTrick(player) match {
      case Success(g) =>
        game = g
        notifyEvent(GameChangedEvent(game))
      case Failure(f) =>
        notifyEvent(GameErrorEvent(
          PassTrickEvent(player, game),
          Failure(f)
        ))
    }
  }

  def getPlayer(name: String): Option[IPlayer] = {
    game.playerHands.keySet.find { p => p.name == name }
  }

  def getPlayer(uuid: UUID): Option[IPlayer] = {
    game.playerHands.keySet.find { p => p.id == uuid }
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
    notifyEvent(GameChangedEvent(game))
  }

  def redo(): Unit = {
    undoManager = undoManager.redoStep()
    notifyEvent(GameChangedEvent(game))
  }

  private class JoinCommand(gameRef: Game, player: IPlayer) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.join(player) match {
        case Success(g) =>
          game = g
          newGame = g
          notifyEvent(PlayerJoinEvent(player, g))
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

  private class QuitCommand(gameRef: Game, player: IPlayer) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.quit(player) match {
        case Success(g) =>
          game = g
          newGame = g
          notifyEvent(PlayerQuitEvent(player, g))
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

  private class PlayCardCommand(gameRef: Game, player: IPlayer, card: Card) extends Command {
    private var oldGame: Game = uninitialized
    private var newGame: Game = uninitialized

    override def doStep(): Unit = {
      oldGame = game
      game.playCard(player, card) match {
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
    val playerNameIds = game.playerHands.keySet.toSeq
    game = GameFactory.create(playerNameIds)
    notifyEvent(GameChangedEvent(game))
  }

  def exit(): Unit = notifyEvent(GameExitEvent)

  def getGame: Game = game

  def getGameState: GameState = game.state

  override def players: Seq[IPlayer] = Seq.empty

  override def playerCount: Int = game.playerHands.size
}
