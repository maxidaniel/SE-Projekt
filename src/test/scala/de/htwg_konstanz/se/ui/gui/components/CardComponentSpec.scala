package de.htwg_konstanz.se.ui.gui.components

import de.htwg_konstanz.se.models.{Card, CardSuit}
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, Suite}
import scalafx.application.Platform
import scalafx.Includes.jfxNode2sfx

class CardComponentSpec extends AnyWordSpec with BeforeAndAfterAll {
  self: Suite =>

  override def beforeAll(): Unit = {
    System.setProperty("javafx.platform", "headless")
    System.setProperty("prism.order", "sw")
    System.setProperty("prism.text", "t2k")
    System.setProperty("glass.platform", "Monocle")
    System.setProperty("monocle.platform", "Headless")
    Platform.startup(() => ())
  }

  "A CardComponent" should {
    "store the correct card" in {
      val component = new CardComponent(Card.AceOfClubs)
      component.card should be(Card.AceOfClubs)
    }

    "default to face-up" in {
      val component = new CardComponent(Card.ThreeOfHearts)
      component.faceDown should be(false)
    }

    "support face-down initialization" in {
      val component = new CardComponent(Card.KingOfSpades, faceDown = true)
      component.faceDown should be(true)
    }

    "flip from face-up to face-down" in {
      val component = new CardComponent(Card.AceOfHearts)
      component.faceDown should be(false)
      component.flip()
      component.faceDown should be(true)
    }

    "flip from face-down to face-up" in {
      val component = new CardComponent(Card.AceOfHearts, faceDown = true)
      component.faceDown should be(true)
      component.flip()
      component.faceDown should be(false)
    }

    "flipToFaceUp when already face-up should be a no-op" in {
      val component = new CardComponent(Card.FiveOfDiamonds)
      component.faceDown should be(false)
      component.flipToFaceUp()
      component.faceDown should be(false)
    }

    "flipToFaceDown when already face-down should be a no-op" in {
      val component = new CardComponent(Card.FiveOfDiamonds, faceDown = true)
      component.faceDown should be(true)
      component.flipToFaceDown()
      component.faceDown should be(true)
    }

    "flipToFaceUp from face-down" in {
      val component = new CardComponent(Card.SevenOfClubs, faceDown = true)
      component.flipToFaceUp()
      component.faceDown should be(false)
    }

    "flipToFaceDown from face-up" in {
      val component = new CardComponent(Card.SevenOfClubs)
      component.flipToFaceDown()
      component.faceDown should be(true)
    }

    "render correct rank and suit text for Hearts" in {
      val component = new CardComponent(Card.AceOfHearts)
      component.card.suit should be(CardSuit.Hearts)
      component.card.rank.symbol should be("A")
      component.card.suit.symbol should be("\u2665")
    }

    "render correct rank and suit text for Diamonds" in {
      val component = new CardComponent(Card.KingOfDiamonds)
      component.card.suit should be(CardSuit.Diamonds)
      component.card.rank.symbol should be("K")
      component.card.suit.symbol should be("\u2666")
    }

    "render correct rank and suit text for Clubs" in {
      val component = new CardComponent(Card.AceOfClubs)
      component.card.suit should be(CardSuit.Clubs)
      component.card.rank.symbol should be("A")
      component.card.suit.symbol should be("\u2663")
    }

    "render correct rank and suit text for Spades" in {
      val component = new CardComponent(Card.AceOfSpades)
      component.card.suit should be(CardSuit.Spades)
      component.card.rank.symbol should be("A")
      component.card.suit.symbol should be("\u2660")
    }

    "have correct preferred dimensions" in {
      val component = new CardComponent(Card.ThreeOfClubs)
      component.prefWidth.value should be(100.0)
      component.prefHeight.value should be(140.0)
    }

    "have minimum width constraint" in {
      val component = new CardComponent(Card.ThreeOfClubs)
      component.minWidth.value should be >= 40.0
    }

    "show a single child by default (front face)" in {
      val component = new CardComponent(Card.AceOfHearts)
      component.children should have size 1
    }

    "show a single child when face-down (back face)" in {
      val component = new CardComponent(Card.AceOfHearts, faceDown = true)
      component.children should have size 1
    }

    "alternate flips correctly through multiple cycles" in {
      val component = new CardComponent(Card.TenOfSpades)
      component.faceDown should be(false)

      component.flip()
      component.faceDown should be(true)

      component.flip()
      component.faceDown should be(false)

      component.flip()
      component.faceDown should be(true)

      component.flipToFaceUp()
      component.faceDown should be(false)
    }

    "have a drop shadow effect on front face" in {
      val component = new CardComponent(Card.AceOfClubs)
      val frontPane = component.children.head
      val style = frontPane.style.value
      style should include("dropshadow")
    }

    "have rounded corners on front face" in {
      val component = new CardComponent(Card.AceOfClubs)
      val frontPane = component.children.head
      val style = frontPane.style.value
      style should include("-fx-background-radius: 12")
      style should include("-fx-border-radius: 12")
    }

    "have white background on front face" in {
      val component = new CardComponent(Card.AceOfClubs)
      val frontPane = component.children.head
      val style = frontPane.style.value
      style should include("-fx-background-color: white")
    }
  }
}
