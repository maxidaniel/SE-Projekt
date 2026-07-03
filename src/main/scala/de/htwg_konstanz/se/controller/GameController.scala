package de.htwg_konstanz.se.controller

import com.google.inject.Inject
import de.htwg_konstanz.se.controller.strategies.IStrategy
import de.htwg_konstanz.se.io.ISaveManager
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.models.PlayerType.Computer
import de.htwg_konstanz.se.util.{Command, UndoManager}

import java.util.UUID
import scala.compiletime.uninitialized
import scala.util.{Failure, Success}

class GameController @Inject() (
    private var game: Game,
    private var undoManager: UndoManager,
    private val saveManager: ISaveManager
) extends IController {
  def join(name: String): Unit = {
    val player = HumanPlayer(name)
    undoManager = undoManager.doStep(new JoinCommand(game, player))
  }

  def joinComputer(name: String, strategy: IStrategy): Unit = {
    val player = ComputerPlayer(name, strategy)
    undoManager = undoManager.doStep(new JoinCommand(game, player))
  }

  def quit(uuid: UUID): Unit = {
    val player = getPlayer(uuid)
    if player.isEmpty then {
      notifyEvent(
        GameErrorEvent(
          PlayerQuitEvent(UnknownPlayer, game),
          Failure(new Exception(s"The player with id $uuid is not part of the game."))
        )
      )
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
        notifyEvent(
          GameErrorEvent(
            CardPlayedEvent(player, Card.Unknown, game),
            Failure(new Exception(s"Invalid card index $index"))
          )
        )
    }
  }

  def playCard(player: IPlayer): Unit = {
    game.playerHands.get(player) match {
      case Some(hand) =>
        game.currentPlayer match {
          case Some(curPlayer) if curPlayer == player =>
            val lastPlayed = game.playedCards.lastOption
            player.playerType match {
              case Computer(strategy) =>
                val card = strategy.play(hand, lastPlayed, game.playedCards)
                card match
                  case Some(c) => playCard(player, c)
                  case None    => passTrick(player)
              case _ =>
                notifyEvent(
                  GameErrorEvent(
                    CardPlayedEvent(player, Card.Unknown, game),
                    Failure(new Exception("Only computer players can use playCard(IPlayer)"))
                  )
                )
            }
          case _ =>
            notifyEvent(
              GameErrorEvent(
                CardPlayedEvent(player, Card.Unknown, game),
                Failure(new Exception("Not this player's turn"))
              )
            )
        }
      case None =>
        notifyEvent(
          GameErrorEvent(
            CardPlayedEvent(player, Card.Unknown, game),
            Failure(new Exception("Player has no cards"))
          )
        )
    }
  }

  def passTrick(player: IPlayer): Unit = {
    val oldGame = game
    game.passTrick(player) match {
      case Success(g) =>
        game = g
        if (g.state == EndedState) {
          val winner = g.finishOrder.headOption.getOrElse(player)
          notifyEvent(GameEndedEvent(g, winner))
        } else {
          notifyEvent(PassTrickEvent(player, game))
          if (oldGame.playedCards.nonEmpty && g.playedCards.isEmpty) {
            val leader = g.currentPlayer.getOrElse(player)
            notifyEvent(TableClearedEvent(leader, game, TableClearReason.TrickWon))
          }
        }
      case Failure(f) =>
        notifyEvent(
          GameErrorEvent(
            PassTrickEvent(player, game),
            Failure(f)
          )
        )
    }
  }

  def nextRound(): Unit = {
    game.nextRound() match {
      case Success(g) =>
        game = g
        notifyEvent(NextRoundEvent(game))
      case Failure(f) =>
        notifyEvent(
          GameErrorEvent(
            GameEndedEvent(game, game.finishOrder.headOption.getOrElse(UnknownPlayer)),
            Failure(f)
          )
        )
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
          notifyTableClearedIfNeeded(oldGame, g, player, card)
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

  def save(path: String): Unit = {
    saveManager.save(game, path) match {
      case Success(_) => notifyEvent(GameChangedEvent(game))
      case Failure(f) => notifyEvent(GameErrorEvent(GameChangedEvent(game), Failure(f)))
    }
  }

  def load(path: String): Unit = {
    saveManager.load(path) match {
      case Success(loadedGame) =>
        game = loadedGame
        undoManager = UndoManager()
        notifyEvent(GameChangedEvent(game))
      case Failure(f) => notifyEvent(GameErrorEvent(GameChangedEvent(game), Failure(f)))
    }
  }

  def deleteSave(path: String): Unit = {
    saveManager.deleteSave(path) match {
      case Success(_) => ()
      case Failure(f) => notifyEvent(GameErrorEvent(GameChangedEvent(game), Failure(f)))
    }
  }

  def exit(): Unit = notifyEvent(GameExitEvent)

  private def notifyTableClearedIfNeeded(oldGame: Game, newGame: Game, player: IPlayer, card: Card): Unit = {
    if (oldGame.playedCards.nonEmpty && newGame.playedCards.isEmpty) {
      val reason =
        if (Game.isBurnCard(card)) TableClearReason.BurnByTwo
        else TableClearReason.TrickWon
      notifyEvent(TableClearedEvent(player, game, reason))
    } else if (
      oldGame.playedCards.isEmpty && newGame.playedCards.isEmpty && newGame.trickCount == 0 && newGame.currentPlayer
        .contains(player)
    ) {
      notifyEvent(TableClearedEvent(player, game, TableClearReason.FourOfAKindBomb))
    }
  }

  def getGame: Game = game

  def getGameState: GameState = game.state

  override def players: Seq[IPlayer] = Seq.empty

  override def playerCount: Int = game.playerHands.size
}
