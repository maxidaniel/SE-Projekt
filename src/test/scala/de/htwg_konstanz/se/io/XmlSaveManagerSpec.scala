package de.htwg_konstanz.se.io

import de.htwg_konstanz.se.controller.strategies.PlayBestCardStrategy
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.models.GameState.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Files
import scala.util.Success

class XmlSaveManagerSpec extends AnyWordSpec:
  "XmlSaveManager" should:
    "save and load a game with human players" in:
      val manager = XmlSaveManager()
      val alice = HumanPlayer("Alice")
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")
      val dave = HumanPlayer("Dave")

      val game = Game(
        playerHands = Map(
          alice -> Vector(Card.ThreeOfClubs, Card.FourOfHearts),
          bob -> Vector(Card.FiveOfClubs, Card.SixOfHearts),
          charlie -> Vector(Card.SevenOfClubs, Card.EightOfHearts),
          dave -> Vector(Card.NineOfClubs, Card.TenOfHearts)
        ),
        playedCards = Vector.empty,
        state = PlayingState,
        currentPlayer = Some(alice),
        trickCount = 0,
        trickRank = None,
        trickLeader = None,
        passedPlayers = Set.empty,
        scoredRanks = Map(alice -> 3, bob -> 1),
        roundNumber = 2,
        finishOrder = Vector(alice, bob, charlie, dave)
      )

      val tempFile = Files.createTempFile("test-save", ".xml")
      val path = tempFile.toString

      manager.save(game, path) shouldBe a[Success[?]]

      val loaded = manager.load(path)
      loaded shouldBe a[Success[?]]

      val loadedGame = loaded.get
      loadedGame.playerHands.size should be(4)
      loadedGame.state should be(PlayingState)
      loadedGame.currentPlayer should be(Some(alice))
      loadedGame.trickCount should be(0)
      loadedGame.roundNumber should be(2)
      loadedGame.scoredRanks should be(Map(alice -> 3, bob -> 1))
      loadedGame.finishOrder should be(Vector(alice, bob, charlie, dave))

      Files.deleteIfExists(tempFile)

    "save and load a game with computer players" in:
      val manager = XmlSaveManager()
      val strategy = PlayBestCardStrategy()
      val bot = ComputerPlayer("Bot", strategy)
      val alice = HumanPlayer("Alice")
      val bob = HumanPlayer("Bob")
      val charlie = HumanPlayer("Charlie")

      val game = Game(
        playerHands = Map(
          bot -> Vector(Card.ThreeOfClubs),
          alice -> Vector(Card.FourOfHearts),
          bob -> Vector(Card.FiveOfClubs),
          charlie -> Vector(Card.SixOfHearts)
        ),
        playedCards = Vector.empty,
        state = PlayingState
      )

      val tempFile = Files.createTempFile("test-save-computer", ".xml")
      val path = tempFile.toString

      manager.save(game, path) shouldBe a[Success[?]]

      val loaded = manager.load(path)
      loaded shouldBe a[Success[?]]

      val loadedGame = loaded.get
      loadedGame.playerHands.size should be(4)

      val loadedBot = loadedGame.playerHands.keys.find(_.name == "Bot")
      loadedBot shouldBe defined
      loadedBot.get.playerType.strategy shouldBe defined
      loadedBot.get.playerType.strategy.get shouldBe a[PlayBestCardStrategy]

      Files.deleteIfExists(tempFile)

    "return failure when loading non-existent file" in:
      val manager = XmlSaveManager()
      val result = manager.load("/nonexistent/path/save.xml")
      result.isFailure should be(true)

    "save to directory by appending save.xml" in:
      val manager = XmlSaveManager()
      val tempDir = Files.createTempDirectory("test-save-dir")
      val game = new Game()

      manager.save(game, tempDir.toString) shouldBe a[Success[?]]

      val saveFile = tempDir.resolve("save.xml")
      Files.exists(saveFile) should be(true)

      val loaded = manager.load(tempDir.toString)
      loaded shouldBe a[Success[?]]

      Files.deleteIfExists(saveFile)
      Files.deleteIfExists(tempDir)

    "delete save file" in:
      val manager = XmlSaveManager()
      val tempFile = Files.createTempFile("test-delete", ".xml")
      Files.writeString(tempFile, "<game/>")

      Files.exists(tempFile) should be(true)

      manager.deleteSave(tempFile.toString) shouldBe a[Success[?]]

      Files.exists(tempFile) should be(false)

    "delete save from directory" in:
      val manager = XmlSaveManager()
      val tempDir = Files.createTempDirectory("test-delete-dir")
      val saveFile = tempDir.resolve("save.xml")
      Files.writeString(saveFile, "<game/>")

      manager.deleteSave(tempDir.toString) shouldBe a[Success[?]]

      Files.exists(saveFile) should be(false)
      Files.deleteIfExists(tempDir)

    "load from directory" in:
      val manager = XmlSaveManager()
      val tempDir = Files.createTempDirectory("test-load-dir")
      val game = new Game()
      manager.save(game, tempDir.toString) shouldBe a[Success[?]]

      val loaded = manager.load(tempDir.toString)
      loaded shouldBe a[Success[?]]
      loaded.get.state should be(GameState.WaitingForPlayers)

      Files.deleteIfExists(tempDir.resolve("save.xml"))
      Files.deleteIfExists(tempDir)

    "save to non-existent parent directory" in:
      val manager = XmlSaveManager()
      val tempDir = Files.createTempDirectory("test-nested")
      val nestedPath = tempDir.resolve("sub/dir/save.xml").toString

      manager.save(new Game(), nestedPath) shouldBe a[Success[?]]

      Files.exists(java.nio.file.Paths.get(nestedPath)) should be(true)
      Files.deleteIfExists(java.nio.file.Paths.get(nestedPath))
      Files.deleteIfExists(tempDir.resolve("sub/dir"))
      Files.deleteIfExists(tempDir.resolve("sub"))
      Files.deleteIfExists(tempDir)
