package de.htwg_konstanz.se.controller

import de.htwg_konstanz.se.controller.strategies.PlayLowestPossibleCardStrategy
import de.htwg_konstanz.se.io.StubSaveManager
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.models.GameState.{Aborted, Ended, Playing, WaitingForPlayers}
import de.htwg_konstanz.se.util.{Listener, UndoManager}
import org.scalatest.OptionValues.*
import org.scalatest.TryValues.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class GameControllerSpec extends AnyWordSpec {
  "A GameController" should {
    val alice = HumanPlayer("Alice")
    val bob = HumanPlayer("Bob")
    val charlie = HumanPlayer("Charlie")
    val dave = HumanPlayer("Dave")
    val stubSaveManager = new StubSaveManager()

    "start with a waiting game by default" in {
      val controller = new GameController(new Game(), new UndoManager(), stubSaveManager)
      controller.getGameState should be(WaitingForPlayers)
      controller.getGame.playerHands should be(Map.empty)
    }

    "join a player and emit a JoinEvent" in {
      val controller = new GameController(new Game(), new UndoManager(), stubSaveManager)
      var observed: Option[PlayerJoinEvent] = None

      controller.add {
        case e: PlayerJoinEvent => observed = Some(e)
        case _                  =>
      }

      controller.join("Alice")
      val player = controller.getPlayer("Alice").value

      controller.getGame.playerHands.keySet should contain(player)
      observed.map(_.player) should be(Some(player))
      observed.map(_.game.playerHands.keySet.contains(player)) should be(Some(true))
    }

    "start a game and emit StartEvent when enough players exist" in {
      val game = Game(
        Map(
          alice -> Vector.empty,
          bob -> Vector.empty,
          HumanPlayer("Charlie") -> Vector.empty,
          HumanPlayer("Dave") -> Vector.empty
        ),
        Vector.empty,
        WaitingForPlayers
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameStartedEvent] = None

      controller.add {
        case e: GameStartedEvent => observed = Some(e)
        case _                   =>
      }

      controller.start()

      controller.getGameState should be(Playing)
      observed.map(_.game.state) should be(Some(Playing))
    }

    "set and expose game instance" in {
      val customGame = Game(Map.empty, Vector.empty, Playing)
      val controller = new GameController(customGame, new UndoManager(), stubSaveManager)

      controller.getGame should be(customGame)
      controller.getGameState should be(Playing)
    }

    "not join a player when game is running and emit GameErrorEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Playing), new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.join("Alice")
      observed.isDefined should be(true)
    }

    "not play a card and emit GameErrorEvent" in {
      val controller =
        new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers), new UndoManager(), stubSaveManager)
      controller.join("Alice")
      val player = controller.getPlayer("Alice").value
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(player, Card.TenOfSpades)
      observed.value.cause should be(
        CardPlayedEvent(player = player, card = Card.TenOfSpades, game = controller.getGame)
      )
      observed.value.error.failure.exception should have message "Can only play cards in playing state."
    }

    "quit a player and emit a QuitEvent" in {
      val controller = new GameController(
        Game(Map(alice -> Vector.empty), Vector.empty, WaitingForPlayers),
        new UndoManager(),
        stubSaveManager
      )
      var observed: Option[PlayerQuitEvent] = None
      controller.add {
        case e: PlayerQuitEvent => observed = Some(e)
        case _                  =>
      }
      controller.quit(alice.id)
      controller.getGame.playerHands.keySet should not contain alice
      observed.map(_.player) should be(Some(alice))
    }

    "not quit a player that is not part of the game and emit GameErrorEvent" in {
      val controller =
        new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers), new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.quit(alice.id)
      observed.value.cause should be(PlayerQuitEvent(UnknownPlayer, controller.getGame))
    }

    "return early from quit when player is not in players list" in {
      val controller = new GameController(new Game(), new UndoManager(), stubSaveManager)
      controller.join("Alice")
      val differentPlayer = HumanPlayer("Nobody")
      var observedEvents: List[GameEvent] = List.empty
      controller.add { (e: GameEvent) =>
        observedEvents = observedEvents :+ e
      }
      controller.quit(differentPlayer.id)
      observedEvents should not be empty
      observedEvents.head shouldBe a[GameErrorEvent]
    }

    "abort a game and emit a GameAbortedEvent" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Playing), new UndoManager(), stubSaveManager)
      var observed: Option[GameAbortedEvent] = None
      controller.add {
        case e: GameAbortedEvent => observed = Some(e)
        case _                   =>
      }
      controller.abort()
      controller.getGameState should be(Aborted)
      observed.isDefined should be(true)
    }

    "not abort a game when not playing" in {
      val controller = new GameController(new Game(), new UndoManager(), stubSaveManager)
      controller.abort()
      controller.getGameState should not be Aborted
    }

    "reset an aborted game to waiting state with existing players" in {
      val game = Game(
        Map(alice -> Vector(Card.ThreeOfHearts), bob -> Vector(Card.FourOfClubs)),
        Vector(Card.FiveOfSpades),
        Aborted
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)

      controller.reset()
      controller.getGameState should be(WaitingForPlayers)
      controller.getGame.playerHands.keySet should contain(alice)
      controller.getGame.playerHands.keySet should contain(bob)
      controller.getGame.playerHands(alice) should be(Vector.empty)
      controller.getGame.playerHands(bob) should be(Vector.empty)
      controller.getGame.playedCards should be(Vector.empty)
    }

    "reset an ended game to waiting state with existing players" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector(Card.KingOfHearts)),
        Vector(Card.AceOfSpades),
        Ended
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)

      controller.reset()
      controller.getGameState should be(WaitingForPlayers)
      controller.getGame.playerHands.keySet should contain(alice)
      controller.getGame.playerHands.keySet should contain(bob)
    }

    "notify listeners on reset" in {
      val controller = new GameController(Game(Map.empty, Vector.empty, Aborted), new UndoManager(), stubSaveManager)
      var observed: Option[GameEvent] = None
      controller.add((e: GameEvent) => observed = Some(e))
      controller.reset()
      observed.isDefined should be(true)
      observed.get match {
        case GameChangedEvent(_) => // correct
        case _                   => fail("Expected GameChangedEvent")
      }
    }

    "not start a game when fewer than 2 players and emit GameErrorEvent" in {
      val controller =
        new GameController(Game(Map.empty, Vector.empty, WaitingForPlayers), new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.start()
      observed.isDefined should be(true)
    }

    "emit GameExitEvent when exit is called" in {
      val controller = new GameController(new Game(), new UndoManager(), stubSaveManager)
      var observed: Boolean = false
      controller.add {
        case GameExitEvent => observed = true
        case _             =>
      }
      controller.exit()
      observed should be(true)
    }

    "emit GameEndedEvent when a player plays their last card" in {
      val game = Game(
        Map(alice -> Vector(Card.TwoOfSpades), bob -> Vector(Card.KingOfHearts, Card.QueenOfClubs)),
        Vector(Card.AceOfClubs),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Ace),
        Some(bob)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameEndedEvent] = None
      controller.add {
        case e: GameEndedEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(controller.getPlayer("Alice").value, Card.TwoOfSpades)
      observed.isDefined should be(true)
      observed.get.winner.name should be("Alice")
    }

    "reject playing a card when it is not the player turn" in {
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(bob)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(alice, Card.KingOfHearts)
      observed.isDefined should be(true)
      observed.value.error.failure.exception should have message "It is not this players' turn."
    }

    "play a card by index successfully" in {
      val game = Game(
        Map(alice -> Vector(Card.FiveOfClubs, Card.SixOfSpades), bob -> Vector(Card.SixOfHearts)),
        Vector(Card.FourOfDiamonds),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Four),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[CardPlayedEvent] = None
      controller.add {
        case e: CardPlayedEvent => observed = Some(e)
        case _                  =>
      }
      controller.playCard(alice, 0)
      observed.isDefined should be(true)
      observed.get.card should be(Card.FiveOfClubs)
      controller.getGame.playerHands(alice) should be(Vector(Card.SixOfSpades))
    }

    "reject playing a card by invalid index" in {
      val game = Game(
        Map(alice -> Vector(Card.ThreeOfHearts)),
        Vector.empty,
        Playing,
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(alice, 5)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Invalid card index")
    }

    "play a card by computer using strategy" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val comp = ComputerPlayer("Com", strategy)
      val game = Game(
        Map(comp -> Vector(Card.FiveOfClubs, Card.SixOfSpades), alice -> Vector(Card.SixOfHearts)),
        Vector(Card.FourOfDiamonds),
        PlayingState,
        Some(comp),
        1,
        Some(CardRank.Four),
        Some(comp)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[CardPlayedEvent] = None
      controller.add {
        case e: CardPlayedEvent => observed = Some(e)
        case _                  =>
      }
      controller.playCard(comp)
      observed.value.card should be(Card.FiveOfClubs)
    }

    "reject computer play when not player's turn" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val comp = ComputerPlayer("Com", strategy)
      val game = Game(
        Map(comp -> Vector(Card.ThreeOfHearts), alice -> Vector(Card.FourOfClubs)),
        Vector(Card.FiveOfSpades),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(comp)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Not this player's turn")
    }

    "start next round from ended state" in {
      val game = Game(
        Map(
          alice -> Vector.empty,
          bob -> Vector(Card.FourOfHearts),
          charlie -> Vector(Card.SixOfHearts),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector.empty,
        GameState.Ended,
        None,
        0,
        None,
        None,
        Set.empty,
        Map.empty,
        1,
        Vector(alice, bob, charlie, dave)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[NextRoundEvent] = None
      controller.add {
        case e: NextRoundEvent => observed = Some(e)
        case _                 =>
      }
      controller.nextRound()
      observed.isDefined should be(true)
      controller.getGameState should be(Playing)
      controller.getGame.roundNumber should be(2)
    }

    "fail next round when game is over" in {
      val game = Game(
        Map(
          alice -> Vector.empty,
          bob -> Vector(Card.FourOfHearts),
          charlie -> Vector(Card.SixOfHearts),
          dave -> Vector(Card.SevenOfHearts)
        ),
        Vector.empty,
        GameState.Ended,
        None,
        0,
        None,
        None,
        Set.empty,
        Map(alice -> 11),
        6,
        Vector(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.nextRound()
      observed.isDefined should be(true)
    }

    "pass a trick successfully" in {
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades), charlie -> Vector(Card.QueenOfHearts)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[PassTrickEvent] = None
      controller.add {
        case e: PassTrickEvent => observed = Some(e)
        case _                 =>
      }
      controller.passTrick(bob)
      observed.isDefined should be(true)
      controller.getGame.passedPlayers should contain(bob)
    }

    "fail to pass when no trick has been led" in {
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades)),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.passTrick(alice)
      observed.isDefined should be(true)
    }

    "reject computer play when player is not in game" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val comp = ComputerPlayer("Com", strategy)
      val game = Game(
        Map(alice -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfSpades),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(comp)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Player has no cards")
    }

    "reject human player using playCard(IPlayer)" in {
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(alice),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(alice)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include(
        "Only computer players can use playCard(IPlayer)"
      )
    }

    "join a computer player" in {
      val controller = new GameController(new Game(), new UndoManager(), stubSaveManager)
      var observed: Option[PlayerJoinEvent] = None
      controller.add {
        case e: PlayerJoinEvent => observed = Some(e)
        case _                  =>
      }
      val strategy = PlayLowestPossibleCardStrategy()
      controller.joinComputer("Bot", strategy)
      val player = controller.getPlayer("Bot").value
      controller.getGame.playerHands.keySet should contain(player)
      player.playerType should be(PlayerType.Computer(strategy))
      observed.isDefined should be(true)
    }

    "expose players as empty sequence" in {
      val controller = new GameController(new Game(), new UndoManager(), stubSaveManager)
      controller.players should be(Seq.empty)
    }

    "return correct playerCount" in {
      val game = Game(
        Map(alice -> Vector.empty, bob -> Vector.empty, charlie -> Vector.empty, dave -> Vector.empty),
        Vector.empty,
        PlayingState
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      controller.playerCount should be(4)
    }

    "handle pass trick failure when trick leader tries to pass" in {
      val game = Game(
        Map(alice -> Vector(Card.KingOfHearts), bob -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.passTrick(alice)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("trick leader cannot pass")
    }

    "play a card with invalid index (negative)" in {
      val game = Game(
        Map(alice -> Vector(Card.ThreeOfHearts)),
        Vector.empty,
        PlayingState,
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(alice, -1)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Invalid card index")
    }

    "play a card when player is not in the game" in {
      val game = Game(
        Map(alice -> Vector(Card.ThreeOfHearts)),
        Vector.empty,
        PlayingState,
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(bob, 0)
      observed.isDefined should be(true)
      observed.value.error.failure.exception.getMessage should include("Invalid card index")
    }

    "emit TableClearedEvent with BurnByTwo when a Two is played" in {
      val game = Game(
        Map(
          alice -> Vector(Card.ThreeOfHearts),
          bob -> Vector(Card.TwoOfHearts, Card.FourOfHearts),
          charlie -> Vector(Card.AceOfHearts),
          dave -> Vector(Card.KingOfHearts)
        ),
        Vector(Card.AceOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Ace),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observedTableClear: Option[TableClearedEvent] = None
      controller.add {
        case e: TableClearedEvent => observedTableClear = Some(e)
        case _                    =>
      }
      controller.playCard(bob, Card.TwoOfHearts)
      observedTableClear.isDefined should be(true)
      observedTableClear.get.reason should be(TableClearReason.BurnByTwo)
    }

    "emit TableClearedEvent with FourOfAKindBomb when leading four of a kind" in {
      val game = Game(
        Map(
          alice -> Vector(
            Card.SevenOfHearts,
            Card.SevenOfClubs,
            Card.SevenOfSpades,
            Card.SevenOfDiamonds,
            Card.ThreeOfHearts
          ),
          bob -> Vector(Card.TenOfHearts),
          charlie -> Vector(Card.JackOfHearts),
          dave -> Vector(Card.QueenOfHearts)
        ),
        Vector.empty,
        PlayingState,
        Some(alice),
        0,
        None,
        None
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observedTableClear: Option[TableClearedEvent] = None
      controller.add {
        case e: TableClearedEvent => observedTableClear = Some(e)
        case _                    =>
      }
      controller.playCard(alice, Card.SevenOfHearts)
      observedTableClear.isDefined should be(true)
      observedTableClear.get.reason should be(TableClearReason.FourOfAKindBomb)
    }

    "emit TableClearedEvent with TrickWon when all others pass" in {
      val game = Game(
        Map(
          alice -> Vector(Card.KingOfHearts),
          bob -> Vector(Card.AceOfSpades, Card.ThreeOfHearts),
          charlie -> Vector(Card.QueenOfHearts),
          dave -> Vector(Card.JackOfHearts)
        ),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice),
        Set(charlie, dave)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observedTableClear: Option[TableClearedEvent] = None
      controller.add {
        case e: TableClearedEvent => observedTableClear = Some(e)
        case _                    =>
      }
      controller.passTrick(bob)
      observedTableClear.isDefined should be(true)
      observedTableClear.get.reason should be(TableClearReason.TrickWon)
    }

    "emit TableClearedEvent with TrickWon when passTrick wins trick" in {
      val game = Game(
        Map(
          alice -> Vector(Card.KingOfHearts),
          bob -> Vector(Card.AceOfSpades),
          charlie -> Vector(Card.QueenOfHearts),
          dave -> Vector(Card.JackOfHearts)
        ),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(bob),
        1,
        Some(CardRank.Five),
        Some(alice),
        Set(charlie, dave)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observedTableClear: Option[TableClearedEvent] = None
      controller.add {
        case e: TableClearedEvent => observedTableClear = Some(e)
        case _                    =>
      }
      controller.passTrick(bob)
      observedTableClear.isDefined should be(true)
      observedTableClear.get.reason should be(TableClearReason.TrickWon)
    }

    "playCard(IPlayer) when computer has no playable cards and passes" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val comp = ComputerPlayer("Com", strategy)
      val game = Game(
        Map(comp -> Vector(Card.ThreeOfHearts), alice -> Vector(Card.AceOfSpades)),
        Vector(Card.AceOfClubs),
        PlayingState,
        Some(comp),
        1,
        Some(CardRank.Ace),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[PassTrickEvent] = None
      controller.add {
        case e: PassTrickEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(comp)
      observed.isDefined should be(true)
    }

    "playCard(IPlayer) when computer has empty hand and passes trick" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val comp = ComputerPlayer("Com", strategy)
      val game = Game(
        Map(comp -> Vector.empty, alice -> Vector(Card.AceOfSpades)),
        Vector(Card.FiveOfClubs),
        PlayingState,
        Some(comp),
        1,
        Some(CardRank.Five),
        Some(alice)
      )
      val controller = new GameController(game, new UndoManager(), stubSaveManager)
      var observed: Option[PassTrickEvent] = None
      controller.add {
        case e: PassTrickEvent => observed = Some(e)
        case _                 =>
      }
      controller.playCard(comp)
      observed.isDefined should be(true)
    }

    "load a game successfully" in {
      val saveManager = new StubSaveManager()
      val game = Game(
        Map(alice -> Vector(Card.ThreeOfClubs)),
        Vector.empty,
        WaitingForPlayers
      )
      saveManager.loadResult = scala.util.Success(game)
      val controller = new GameController(new Game(), new UndoManager(), saveManager)
      var observed: Option[GameChangedEvent] = None
      controller.add {
        case e: GameChangedEvent => observed = Some(e)
        case _                   =>
      }
      controller.load("test-path")
      observed.isDefined should be(true)
      controller.getGame should be(game)
    }

    "handle load failure" in {
      val saveManager = new StubSaveManager()
      saveManager.loadResult = scala.util.Failure(new Exception("load failed"))
      val controller = new GameController(new Game(), new UndoManager(), saveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.load("test-path")
      observed.isDefined should be(true)
    }

    "save a game successfully" in {
      val saveManager = new StubSaveManager()
      val controller = new GameController(new Game(), new UndoManager(), saveManager)
      var observed: Option[GameChangedEvent] = None
      controller.add {
        case e: GameChangedEvent => observed = Some(e)
        case _                   =>
      }
      controller.save("test-path")
      observed.isDefined should be(true)
    }

    "handle save failure" in {
      val saveManager = new StubSaveManager()
      saveManager.saveResult = scala.util.Failure(new Exception("save failed"))
      val controller = new GameController(new Game(), new UndoManager(), saveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.save("test-path")
      observed.isDefined should be(true)
    }

    "delete save successfully" in {
      val saveManager = new StubSaveManager()
      val controller = new GameController(new Game(), new UndoManager(), saveManager)
      controller.deleteSave("test-path")
    }

    "handle delete save failure" in {
      val saveManager = new StubSaveManager()
      saveManager.deleteResult = scala.util.Failure(new Exception("delete failed"))
      val controller = new GameController(new Game(), new UndoManager(), saveManager)
      var observed: Option[GameErrorEvent] = None
      controller.add {
        case e: GameErrorEvent => observed = Some(e)
        case _                 =>
      }
      controller.deleteSave("test-path")
      observed.isDefined should be(true)
    }
  }
}
