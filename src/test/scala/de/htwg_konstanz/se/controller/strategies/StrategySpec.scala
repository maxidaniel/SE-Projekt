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
      val lastPlayed = Some(Card.FourOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(true)
      val result = strategy.play(cards, lastPlayed)
      result should be(Some(Card.FiveOfHearts))
    }

    "report canPlay false when no cards beat lastPlayed" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.SevenOfHearts, Card.FiveOfSpades, Card.TenOfDiamonds)
      val lastPlayed = Some(Card.QueenOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(false)
    }

    "report canPlay false when all cards are lower than lastPlayed" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.ThreeOfHearts, Card.FiveOfSpades, Card.QueenOfDiamonds)
      val lastPlayed = Some(Card.TwoOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(false)
    }

    "report canPlay true when at least one card beats lastPlayed" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.QueenOfHearts, Card.FiveOfSpades, Card.ThreeOfDiamonds)
      val lastPlayed = Some(Card.FourOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(true)
      val result = strategy.play(cards, lastPlayed)
      result should be(Some(Card.FiveOfSpades))
    }

    "use playedCards parameter" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades)
      val lastPlayed = Some(Card.FourOfHearts)
      val playedCards = Vector(Card.ThreeOfClubs)

      strategy.canPlay(cards, lastPlayed, playedCards) should be(true)
      val result = strategy.play(cards, lastPlayed, playedCards)
      result should be(Some(Card.FiveOfHearts))
    }

    "play lowest card when no lastPlayed" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)

      strategy.canPlay(cards, None) should be(true)
      val result = strategy.play(cards, None)
      result should be(Some(Card.FiveOfHearts))
    }

    "always accept exchange" in {
      val strategy = PlayLowestPossibleCardStrategy()
      val hand = Vector(Card.ThreeOfHearts, Card.FourOfHearts)
      val offered = Vector(Card.KingOfHearts, Card.AceOfHearts)

      strategy.shouldAcceptExchange(hand, offered, "president") should be(true)
      strategy.shouldAcceptExchange(hand, offered, "scum") should be(true)
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
      val lastPlayed = Some(Card.FourOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(true)
      val result = strategy.play(cards, lastPlayed)
      result shouldBe defined
      Game.canBeat(result.get, lastPlayed.get) should be(true)
    }

    "play one of the valid cards" in {
      val strategy = PlayRandomCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)
      val lastPlayed = Some(Card.FourOfHearts)

      (1 to 100).foreach { _ =>
        val result = strategy.play(cards, lastPlayed)
        result shouldBe defined
        cards should contain(result.get)
      }
    }

    "report canPlay false when no cards beat lastPlayed" in {
      val strategy = PlayRandomCardStrategy()
      val cards = Vector(Card.ThreeOfHearts, Card.FiveOfSpades)
      val lastPlayed = Some(Card.TwoOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(false)
    }

    "use playedCards parameter" in {
      val strategy = PlayRandomCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)
      val lastPlayed = Some(Card.FourOfHearts)
      val playedCards = Vector(Card.ThreeOfClubs)

      (1 to 100).foreach { _ =>
        val result = strategy.play(cards, lastPlayed, playedCards)
        result shouldBe defined
        Game.canBeat(result.get, lastPlayed.get) should be(true)
      }
    }

    "play a random card when no lastPlayed" in {
      val strategy = PlayRandomCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)

      strategy.canPlay(cards, None) should be(true)
      (1 to 100).foreach { _ =>
        val result = strategy.play(cards, None)
        result shouldBe defined
        cards should contain(result.get)
      }
    }

    "accept exchange randomly" in {
      val strategy = PlayRandomCardStrategy()
      val hand = Vector(Card.ThreeOfHearts, Card.FourOfHearts)
      val offered = Vector(Card.KingOfHearts, Card.AceOfHearts)

      val results = (1 to 100).map(_ => strategy.shouldAcceptExchange(hand, offered, "president"))
      results should contain(true)
      results should contain(false)
    }

    "play returns None when cards are empty" in {
      val strategy = PlayRandomCardStrategy()
      strategy.play(Vector.empty, Some(Card.FourOfHearts)) should be(None)
    }

    "play returns None when no cards beat lastPlayed" in {
      val strategy = PlayRandomCardStrategy()
      val cards = Vector(Card.ThreeOfHearts, Card.FiveOfSpades)
      strategy.play(cards, Some(Card.TwoOfHearts)) should be(None)
    }
  }

  "BestPlayStrategy" should {
    "have the correct name" in {
      val strategy = PlayBestCardStrategy()
      strategy.name should be("Best play strategy")
    }

    "play the highest card that beats the last played" in {
      val strategy = PlayBestCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)
      val lastPlayed = Some(Card.FourOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(true)
      val result = strategy.play(cards, lastPlayed)
      result should be(Some(Card.KingOfDiamonds))
    }

    "report canPlay false when no cards beat lastPlayed" in {
      val strategy = PlayBestCardStrategy()
      val cards = Vector(Card.SevenOfHearts, Card.FiveOfSpades, Card.TenOfDiamonds)
      val lastPlayed = Some(Card.QueenOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(false)
    }

    "play the single valid card when only one beats lastPlayed" in {
      val strategy = PlayBestCardStrategy()
      val cards = Vector(Card.ThreeOfHearts, Card.AceOfSpades, Card.FiveOfDiamonds)
      val lastPlayed = Some(Card.KingOfHearts)

      strategy.canPlay(cards, lastPlayed) should be(true)
      val result = strategy.play(cards, lastPlayed)
      result should be(Some(Card.AceOfSpades))
    }

    "use playedCards parameter" in {
      val strategy = PlayBestCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)
      val lastPlayed = Some(Card.FourOfHearts)
      val playedCards = Vector(Card.ThreeOfClubs)

      strategy.canPlay(cards, lastPlayed, playedCards) should be(true)
      val result = strategy.play(cards, lastPlayed, playedCards)
      result should be(Some(Card.KingOfDiamonds))
    }

    "play highest card when no lastPlayed" in {
      val strategy = PlayBestCardStrategy()
      val cards = Vector(Card.FiveOfHearts, Card.QueenOfSpades, Card.KingOfDiamonds)

      strategy.canPlay(cards, None) should be(true)
      val result = strategy.play(cards, None)
      result should be(Some(Card.KingOfDiamonds))
    }

    "accept exchange when offered cards are better than hand average" in {
      val strategy = PlayBestCardStrategy()
      val hand = Vector(Card.ThreeOfHearts, Card.FourOfHearts, Card.FiveOfHearts)
      val offered = Vector(Card.KingOfHearts, Card.AceOfHearts)

      strategy.shouldAcceptExchange(hand, offered, "president") should be(true)
    }

    "reject exchange when offered cards are worse than hand average" in {
      val strategy = PlayBestCardStrategy()
      val hand = Vector(Card.KingOfHearts, Card.AceOfHearts, Card.TwoOfHearts)
      val offered = Vector(Card.ThreeOfHearts, Card.FourOfHearts)

      strategy.shouldAcceptExchange(hand, offered, "president") should be(false)
    }

    "accept exchange as scum when offered cards improve hand" in {
      val strategy = PlayBestCardStrategy()
      val hand = Vector(Card.ThreeOfHearts, Card.FourOfHearts)
      val offered = Vector(Card.KingOfHearts)

      strategy.shouldAcceptExchange(hand, offered, "scum") should be(true)
    }

    "reject exchange when offered cards are empty" in {
      val strategy = PlayBestCardStrategy()
      val hand = Vector(Card.ThreeOfHearts, Card.FourOfHearts)

      strategy.shouldAcceptExchange(hand, Vector.empty, "president") should be(false)
    }

    "reject exchange when hand is empty" in {
      val strategy = PlayBestCardStrategy()
      val offered = Vector(Card.KingOfHearts)

      strategy.shouldAcceptExchange(Vector.empty, offered, "president") should be(false)
    }

    "accept exchange when offered cards equal hand average" in {
      val strategy = PlayBestCardStrategy()
      val hand = Vector(Card.FiveOfHearts, Card.FiveOfClubs)
      val offered = Vector(Card.FiveOfSpades)

      strategy.shouldAcceptExchange(hand, offered, "vice_president") should be(false)
    }

    "accept exchange for unknown position" in {
      val strategy = PlayBestCardStrategy()
      val hand = Vector(Card.ThreeOfHearts, Card.FourOfHearts)
      val offered = Vector(Card.KingOfHearts, Card.AceOfHearts)

      strategy.shouldAcceptExchange(hand, offered, "unknown_position") should be(true)
    }

    "play returns None when cards are empty" in {
      val strategy = PlayBestCardStrategy()
      strategy.play(Vector.empty, Some(Card.FourOfHearts)) should be(None)
    }

    "play returns None when no cards beat lastPlayed" in {
      val strategy = PlayBestCardStrategy()
      val cards = Vector(Card.ThreeOfHearts, Card.FiveOfSpades)
      strategy.play(cards, Some(Card.TwoOfHearts)) should be(None)
    }
  }
}
