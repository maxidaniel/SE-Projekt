import de.htwg_konstanz.se.CardKind._
import de.htwg_konstanz.se.CardColour._
import de.htwg_konstanz.se.{Card, Player}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PresidentSpec extends AnyWordSpec with Matchers {
  "Person" when {
    "name" should {
      val player = Player("Test", Vector.empty)
      "be test" in {
        player.getName should be("Test")
      }
    }
  }

  "Card" when {
    "2 of hearts" should {
      val card = Card(Heart, 2, Number)
      "colour should be hearts" in {
        card.colour should be(Heart)
      }

      "value should be 2" in {
        card.value should be(2)
      }

      "kind should be number" in {
        card.kind should be(Number)
      }
    }
  }

  "A Card" when {
    "it is a 2" should {
      "have a power of 15" in {
        val power = 15
        power shouldBe 15
      }
    }

    "comparing a King and an Ace" should {
      "identify the Ace as stronger" in {
        val ace = 14
        val king = 13
        ace should be > king
      }
    }
  }
}