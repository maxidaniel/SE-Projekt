import de.htwg_konstanz.se.CardKind.*
import de.htwg_konstanz.se.CardColour.*
import de.htwg_konstanz.se.{Card, CardKind, Game, Player}
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
      val card = Card(Heart, Two)
      "colour should be hearts" in {
        card.colour should be(Heart)
      }

      "value should be 2" in {
        card.kind should be(Two)
        card.kind.getValue should be(2)
      }
    }

    "ace of spades" should {
      val card = Card(Spade, Ace)
      "colour be spades" in {
        card.colour should be(Spade)
      }

      "value be ace" in {
        card.kind should be(Ace)
        card.kind.getValue should be(14)
      }
    }
  }

  "Game" when {
    "cards" should {
      val cards = Vector(Card(Heart, Seven), Card(Club, Queen), Card(Diamond, Two)) // TODO: Change order of params in Card to be (Kind, Colour)
      val game = Game(Vector.empty, cards)

      "have a length of 3" in {
        game.cards.length should be(3)
      }

      "are shuffled" in {
        game.cards should be(cards)
        val shuffled = game.shuffle()
        shuffled.cards should not be(cards)
      }
    }
  }
}