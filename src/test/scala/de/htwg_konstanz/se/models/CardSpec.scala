package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.ui.tui.CardRenderer
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsNull, Json}

class CardSpec extends AnyWordSpec {
  "A Card" should {
    "have a string representation" in {
      Card.AceOfClubs.toString should be("A ♣")
    }

    "have an unknown representation" in {
      Card.Unknown.toString should be("? ?")
    }

    "render unknown card placeholders" in {
      val rendered = Card.Unknown.render()
      rendered(1) should include("?")
      rendered(3) should include("?")
      rendered(5) should include("?")
    }

    "be renderable in 2D" in {
      val rendered = Card.AceOfClubs.render()
      rendered should have size 7
      rendered(0) should be("┌─────────┐")
      rendered(1) should be("│ A       │")
      rendered(3) should be("│    ♣    │")
      rendered(5) should be("│       A │")
      rendered(6) should be("└─────────┘")
    }

    "be renderable in 2D for 10" in {
      val rendered = Card.TenOfHearts.render()
      rendered(1) should be("│ 10      │")
      rendered(5) should be("│      10 │")
    }

    "be renderable in 2D with scale" in {
      val rendered = Card.AceOfClubs.render(1)
      rendered should have size 7
      rendered(0) should be("┌─────────┐")
    }

    "be renderable in 2D with scale 2" in {
      val rendered = Card.AceOfClubs.render(2)
      rendered should have size 13
      rendered(0) should be("┌───────────────────┐")
    }

    "scale dimensions consistently for higher scales" in {
      val rendered = Card.AceOfClubs.render(3)
      rendered should have size 19
      rendered.head.length should be(31)
      rendered.last.length should be(31)
      rendered.count(_.contains("♣")) should be(1)
    }

    "keep rank padding stable for one-digit and two-digit ranks" in {
      val ace = Card.AceOfSpades.render()
      val ten = Card.TenOfSpades.render()

      ace(1) should startWith("│ A ")
      ace(5) should endWith(" A │")
      ten(1) should startWith("│ 10")
      ten(5) should endWith("10 │")
    }

    "produce symmetric top and bottom frame widths for all tested scales" in {
      for scale <- 1 to 4 do
        val rendered = Card.QueenOfDiamonds.render(scale)
        rendered.head.length should be(rendered.last.length)
        rendered.head.head should be('┌')
        rendered.last.head should be('└')
        rendered.head.last should be('┐')
        rendered.last.last should be('┘')
    }
  }

  "Card JSON serialization" should {
    "serialize a normal card" in {
      val card = Card.AceOfClubs
      val json = Json.toJson(card)
      (json \ "rank").as[CardRank] should be(CardRank.Ace)
      (json \ "suit").as[CardSuit] should be(CardSuit.Clubs)
    }

    "serialize Card.Unknown as null rank and suit" in {
      val json = Json.toJson(Card.Unknown)
      (json \ "rank").get should be(JsNull)
      (json \ "suit").get should be(JsNull)
    }

    "deserialize a normal card" in {
      val json = Json.obj("rank" -> "ace", "suit" -> "clubs")
      val card = json.as[Card]
      card should be(Card.AceOfClubs)
    }

    "deserialize missing rank as Unknown" in {
      val json = Json.obj("suit" -> "clubs")
      val card = json.as[Card]
      card should be(Card.Unknown)
    }

    "deserialize missing suit as Unknown" in {
      val json = Json.obj("rank" -> "ace")
      val card = json.as[Card]
      card should be(Card.Unknown)
    }
  }

  "Card XML serialization" should {
    "toXml a normal card" in {
      val xml = Card.toXml(Card.AceOfClubs)
      (xml \ "@rank").text should be("ace")
      (xml \ "@suit").text should be("clubs")
    }

    "toXml Card.Unknown" in {
      val xml = Card.toXml(Card.Unknown)
      (xml \ "@rank").text should be("")
      (xml \ "@suit").text should be("")
    }

    "fromXml a normal card" in {
      val xml = <card rank="ace" suit="clubs"/>
      Card.fromXml(xml) should be(Card.AceOfClubs)
    }

    "fromXml with empty attributes returns Unknown" in {
      val xml = <card rank="" suit=""/>
      Card.fromXml(xml) should be(Card.Unknown)
    }
  }

  "CardSuit.fromXml" should {
    "parse all suits" in {
      CardSuit.fromXml(scala.xml.Text("hearts")) should be(CardSuit.Hearts)
      CardSuit.fromXml(scala.xml.Text("diamonds")) should be(CardSuit.Diamonds)
      CardSuit.fromXml(scala.xml.Text("clubs")) should be(CardSuit.Clubs)
      CardSuit.fromXml(scala.xml.Text("spades")) should be(CardSuit.Spades)
    }

    "throw on unknown suit" in {
      an[IllegalArgumentException] should be thrownBy CardSuit.fromXml(scala.xml.Text("unknown"))
    }
  }

  "CardRank.fromXml" should {
    "parse all ranks" in {
      CardRank.fromXml(scala.xml.Text("two")) should be(CardRank.Two)
      CardRank.fromXml(scala.xml.Text("three")) should be(CardRank.Three)
      CardRank.fromXml(scala.xml.Text("four")) should be(CardRank.Four)
      CardRank.fromXml(scala.xml.Text("five")) should be(CardRank.Five)
      CardRank.fromXml(scala.xml.Text("six")) should be(CardRank.Six)
      CardRank.fromXml(scala.xml.Text("seven")) should be(CardRank.Seven)
      CardRank.fromXml(scala.xml.Text("eight")) should be(CardRank.Eight)
      CardRank.fromXml(scala.xml.Text("nine")) should be(CardRank.Nine)
      CardRank.fromXml(scala.xml.Text("ten")) should be(CardRank.Ten)
      CardRank.fromXml(scala.xml.Text("jack")) should be(CardRank.Jack)
      CardRank.fromXml(scala.xml.Text("queen")) should be(CardRank.Queen)
      CardRank.fromXml(scala.xml.Text("king")) should be(CardRank.King)
      CardRank.fromXml(scala.xml.Text("ace")) should be(CardRank.Ace)
    }

    "throw on unknown rank" in {
      an[IllegalArgumentException] should be thrownBy CardRank.fromXml(scala.xml.Text("unknown"))
    }
  }

  "CardRank JSON" should {
    "deserialize valid rank" in {
      val json = play.api.libs.json.JsString("ace")
      json.validate[CardRank].asOpt should be(Some(CardRank.Ace))
    }

    "fail on unknown rank name" in {
      val json = play.api.libs.json.JsString("nonexistent")
      json.validate[CardRank].isError should be(true)
    }

    "fail on non-string input" in {
      val json = play.api.libs.json.JsNumber(42)
      json.validate[CardRank].isError should be(true)
    }
  }

  "CardSuit JSON" should {
    "deserialize valid suit" in {
      val json = play.api.libs.json.JsString("hearts")
      json.validate[CardSuit].asOpt should be(Some(CardSuit.Hearts))
    }

    "fail on unknown suit name" in {
      val json = play.api.libs.json.JsString("nonexistent")
      json.validate[CardSuit].isError should be(true)
    }

    "fail on non-string input" in {
      val json = play.api.libs.json.JsNumber(42)
      json.validate[CardSuit].isError should be(true)
    }
  }
}
