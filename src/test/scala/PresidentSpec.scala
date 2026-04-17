import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// "AnyWordSpec" ist das Grundgerüst aus deinen Folien
// "with Matchers" bringt das "shouldBe" etc. in die Klasse
class PresidentSpec extends AnyWordSpec with Matchers {

  "A Card" when {
    "it is a 2" should {
      "have a power of 15" in {
        val power = 15
        // Hier nutzen wir "shouldBe" - das kommt durch Matchers
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