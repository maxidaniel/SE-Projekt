import de.htwg_konstanz.se.Player
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PresidentSpec extends AnyWordSpec with Matchers {

  "Person" when {
    "name" should {
      val player = Player("Test")
      "be test" in {
        player.getName should be("Test")
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