package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.{Card, Game}
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

class StrategySpec extends AnyWordSpec {

  "PlayLowestPossibleCardStrategy" should {
    "have the correct name" in {
      val strategy = PlayLowestPossibleCardStrategy()
      strategy.name should be("Lowest possible card strategy")
    }

    "play the lowest card that beats the last played" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)
      val lastPlayed = Card.FourOfHearts

      val result = strategy.play(cards, lastPlayed)
      result should be(Card.FiveOfHearts)
    }

    "play the lowest card from all cards when none beat lastPlayed" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.SevenOfHearts, Card.FiveOfSpades, Card.TenOfDiamonds)
      val lastPlayed = Card.QueenOfHearts

      val result = strategy.play(cards, lastPlayed)
      result should be(Card.FiveOfSpades)
    }

    "return the lowest card overall when filtered is empty" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.ThreeOfHearts, Card.FiveOfSpades, Card.QueenOfDiamonds)
      val lastPlayed = Card.TwoOfHearts

      val result = strategy.play(cards, lastPlayed)
      result should be(Card.ThreeOfHearts)
    }

    "return the lowest card overall from mixed set" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.QueenOfHearts, Card.FiveOfSpades, Card.ThreeOfDiamonds)
      val lastPlayed = Card.FourOfHearts

      val result = strategy.play(cards, lastPlayed)
      result should be(Card.ThreeOfDiamonds)
    }
  }

  "PlayRandomCardStrategy" should {
    "have the correct name" in {
      val strategy = PlayRandomCardStrategy()
      strategy.name should be("Random card strategy")
    }

    "play a card that beats the last played" in {
      val strategy = PlayRandomCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)
      val lastPlayed = Card.FourOfHearts

      val result = strategy.play(cards, lastPlayed)
      Game.canBeat(result, lastPlayed) should be(true)
    }

    "play one of the valid cards" in {
      val strategy = PlayRandomCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)
      val lastPlayed = Card.FourOfHearts

      (1 to 100).foreach { _ =>
        val result = strategy.play(cards, lastPlayed)
        cards should contain(result)
      }
    }
  }
}
