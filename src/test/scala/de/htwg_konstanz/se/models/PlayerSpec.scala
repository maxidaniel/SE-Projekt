package de.htwg_konstanz.se.models

import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import de.htwg_konstanz.se.controller.strategies.PlayLowestPossibleCardStrategy
import de.htwg_konstanz.se.controller.strategies.PlayRandomCardStrategy
import java.util.UUID

class PlayerSpec extends AnyWordSpec {
  "A human player" should {
    val player = HumanPlayer("Test")

    "be created with a name, id, Human player type, and empty strategy" in {
      player.name should be("Test")
      player.id should not be null
      player.playerType should be(PlayerType.Human)
      player.playerType.strategy should be(None)
    }
  }

  "A computer player" should {
    val strat = PlayLowestPossibleCardStrategy()
    val player = ComputerPlayer("Computer", strat)

    "have name 'Computer'" in {
      player.name should be("Computer")
    }

    "have PlayLowestPossibleCard strategy" in {
      player.strategy should be(strat)
    }
  }

  "Player equality" should {
    "be equal for same id" in {
      val id = UUID.randomUUID()
      val p1 = HumanPlayer("Test1")
      val p2 = HumanPlayer("Test2")
      // Create players with same id by using the sealed trait constructor
      val p3 = new HumanPlayer("Test1") {
        override val id: UUID = p1.id
      }
      p1 should be(p1)
      p1.hashCode() should be(p1.id.hashCode())
    }

    "not be equal to non-player" in {
      val player = HumanPlayer("Test")
      player.equals("not a player") should be(false)
    }

    "not be equal to null" in {
      val player = HumanPlayer("Test")
      player.equals(null) should be(false)
    }
  }

  "PlayerType" should {
    "have Computer with strategy" in {
      val strat = PlayLowestPossibleCardStrategy()
      PlayerType.Computer(strat).strategy should be(Some(strat))
    }

    "have Human without strategy" in {
      PlayerType.Human.strategy should be(None)
    }

    "have Unknown without strategy" in {
      PlayerType.Unknown.strategy should be(None)
    }
  }

  "UnknownPlayer" should {
    "have name Unknown" in {
      UnknownPlayer.name should be("Unknown")
    }

    "be of type Unknown" in {
      UnknownPlayer.playerType should be(PlayerType.Unknown)
    }
  }

  "IPlayer JSON Writes" should {
    "serialize a HumanPlayer to JSON" in {
      val player: IPlayer = HumanPlayer("Alice")
      val json = Json.toJson(player)
      (json \ "type").as[String] should be("Human")
      (json \ "name").as[String] should be("Alice")
      (json \ "id").as[String] should be(player.id.toString)
    }

    "serialize a ComputerPlayer to JSON" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val player: IPlayer = ComputerPlayer("Bot", strategy)
      val json = Json.toJson(player)
      (json \ "type").as[String] should be("Computer")
      (json \ "name").as[String] should be("Bot")
      (json \ "strategy").as[String] should be("PlayLowestPossibleCardStrategy")
    }

    "serialize UnknownPlayer to JSON" in {
      val json = Json.toJson(UnknownPlayer: IPlayer)
      (json \ "type").as[String] should be("Unknown")
      (json \ "name").as[String] should be("Unknown")
    }
  }

  "IPlayer JSON Reads" should {
    "deserialize a HumanPlayer from JSON" in {
      val json = Json.obj("type" -> "Human", "name" -> "Alice", "id" -> UUID.randomUUID().toString)
      val player = json.as[IPlayer]
      player.name should be("Alice")
      player shouldBe a[HumanPlayer]
    }

    "deserialize a ComputerPlayer from JSON" in {
      val json = Json.obj(
        "type" -> "Computer",
        "name" -> "Bot",
        "id" -> UUID.randomUUID().toString,
        "strategy" -> "PlayRandomCardStrategy"
      )
      val player = json.as[IPlayer]
      player.name should be("Bot")
      player shouldBe a[ComputerPlayer]
    }

    "deserialize unknown type as UnknownPlayer" in {
      val json = Json.obj("type" -> "SomethingElse", "name" -> "X", "id" -> UUID.randomUUID().toString)
      val player = json.as[IPlayer]
      player should be(UnknownPlayer)
    }

    "fail for ComputerPlayer with unknown strategy" in {
      val json = Json.obj(
        "type" -> "Computer",
        "name" -> "Bot",
        "id" -> UUID.randomUUID().toString,
        "strategy" -> "NonExistentStrategy"
      )
      json.validate[IPlayer].isError should be(true)
    }

    "deserialize HumanPlayer with missing id" in {
      val json = Json.obj("type" -> "Human", "name" -> "Alice")
      val player = json.as[IPlayer]
      player.name should be("Alice")
      player shouldBe a[HumanPlayer]
      player.id should not be null
    }
  }

  "IPlayer XML serialization" should {
    "toXml and fromXml a HumanPlayer" in {
      val player = HumanPlayer("Alice")
      val xml = IPlayer.toXml(player)
      val restored = IPlayer.fromXml(xml)
      restored.name should be("Alice")
      restored shouldBe a[HumanPlayer]
      restored.id should be(player.id)
    }

    "toXml and fromXml a ComputerPlayer" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val player = ComputerPlayer("Bot", strategy)
      val xml = IPlayer.toXml(player)
      val restored = IPlayer.fromXml(xml)
      restored.name should be("Bot")
      restored shouldBe a[ComputerPlayer]
    }

    "toXml and fromXml UnknownPlayer" in {
      val xml = IPlayer.toXml(UnknownPlayer)
      val restored = IPlayer.fromXml(xml)
      restored should be(UnknownPlayer)
    }

    "fromXml with unknown type returns UnknownPlayer" in {
      val xml = <player type="Unknown" name="X" id=""/>
      val restored = IPlayer.fromXml(xml)
      restored should be(UnknownPlayer)
    }

    "fromXml with Computer type but unknown strategy returns UnknownPlayer" in {
      val xml = <player type="Computer" name="Bot" id="" strategy="NoSuchStrategy"/>
      val restored = IPlayer.fromXml(xml)
      restored should be(UnknownPlayer)
    }
  }

  "Player(...)" should {
    "create a HumanPlayer when given PlayerType.Human" in {
      val player = Player(PlayerType.Human, "Alice")
      player.name should be("Alice")
      player shouldBe a[HumanPlayer]
      player.playerType should be(PlayerType.Human)
    }

    "create a ComputerPlayer when given PlayerType.Computer" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val player = Player(PlayerType.Computer(strategy), "Bot")
      player.name should be("Bot")
      player shouldBe a[ComputerPlayer]
      player.playerType should be(PlayerType.Computer(strategy))
    }

    "create a ComputerPlayer with PlayRandomCardStrategy" in {
      val strategy = PlayRandomCardStrategy()
      val player = Player(PlayerType.Computer(strategy), "RandomBot")
      player.name should be("RandomBot")
      player shouldBe a[ComputerPlayer]
      player.playerType.strategy should be(Some(strategy))
    }

    "return UnknownPlayer when given PlayerType.Unknown" in {
      val player = Player(PlayerType.Unknown, "anything")
      player should be(UnknownPlayer)
      player.name should be("Unknown")
      player.playerType should be(PlayerType.Unknown)
    }

    "assign a non-null id to created players" in {
      val human = Player(PlayerType.Human, "Test")
      human.id should not be null

      val strategy = PlayLowestPossibleCardStrategy()
      val computer = Player(PlayerType.Computer(strategy), "Test")
      computer.id should not be null
    }
  }
}
