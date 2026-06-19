package de.htwg_konstanz.se.models

import scala.collection.mutable.ListBuffer

// TODO: Symbols
enum CardSuit(val name: String, val symbol: String) {
  case Hearts extends CardSuit("hearts", "♥")
  case Diamonds extends CardSuit("diamonds", "♦")
  case Clubs extends CardSuit("clubs", "♣")
  case Spades extends CardSuit("spades", "♠")
}

enum CardRank(val name: String, val symbol: String) {
  case Two extends CardRank("two", "2")
  case Three extends CardRank("three", "3")
  case Four extends CardRank("four", "4")
  case Five extends CardRank("five", "5")
  case Six extends CardRank("six", "6")
  case Seven extends CardRank("seven", "7")
  case Eight extends CardRank("eight", "8")
  case Nine extends CardRank("nine", "9")
  case Ten extends CardRank("ten", "10")
  case Jack extends CardRank("jack", "J")
  case Queen extends CardRank("queen", "Q")
  case King extends CardRank("king", "K")
  case Ace extends CardRank("ace", "A")
}

class Card private (val rank: CardRank, val suit: CardSuit) {
  override def toString: String = s"${if (rank == null) "?" else rank.symbol} ${if (suit == null) "?" else suit.symbol}"

  def render(scale: Int = 1): Vector[String] = {
    val r = if (rank == null) "?" else rank.symbol
    val s = if (suit == null) "?" else suit.symbol
    
    val width = 9 + (scale - 1) * 10 
    val height = 5 + (scale - 1) * 6
    
    val innerWidth = 9 + (scale - 1) * 10
    val innerHeight = 5 + (scale - 1) * 6
    val mid = innerHeight / 2

    val top = "┌" + "─" * innerWidth + "┐"
    val bottom = "└" + "─" * innerWidth + "┘"
    
    val rankLeft = if (r.length == 1) s"$r " else r
    val rankRight = if (r.length == 1) s" $r" else r
    
    val lines = ListBuffer[String]()
    lines += top
    
    // Rank line top
    lines += s"│ $rankLeft" + " " * (innerWidth - 1 - rankLeft.length) + "│"
    
    // Middle lines
    for (i <- 1 until innerHeight - 1) {
      if (i == mid) {
        val leftPad = (innerWidth - s.length) / 2
        val rightPad = innerWidth - s.length - leftPad
        lines += s"│" + " " * leftPad + s + " " * rightPad + "│"
      } else {
        lines += "│" + " " * innerWidth + "│"
      }
    }
    
    // Rank line bottom
    lines += s"│" + " " * (innerWidth - 1 - rankRight.length) + s"$rankRight │"
    
    lines += bottom
    lines.toVector
  }
}

object Card {
  case object Unknown extends Card(null, null) with Product with Serializable

  // Convenience vals for backward compatibility
  val TwoOfHearts = Card(CardRank.Two, CardSuit.Hearts)
  val ThreeOfHearts = Card(CardRank.Three, CardSuit.Hearts)
  val FourOfHearts = Card(CardRank.Four, CardSuit.Hearts)
  val FiveOfHearts = Card(CardRank.Five, CardSuit.Hearts)
  val SixOfHearts = Card(CardRank.Six, CardSuit.Hearts)
  val SevenOfHearts = Card(CardRank.Seven, CardSuit.Hearts)
  val EightOfHearts = Card(CardRank.Eight, CardSuit.Hearts)
  val NineOfHearts = Card(CardRank.Nine, CardSuit.Hearts)
  val TenOfHearts = Card(CardRank.Ten, CardSuit.Hearts)
  val JackOfHearts = Card(CardRank.Jack, CardSuit.Hearts)
  val QueenOfHearts = Card(CardRank.Queen, CardSuit.Hearts)
  val KingOfHearts = Card(CardRank.King, CardSuit.Hearts)
  val AceOfHearts = Card(CardRank.Ace, CardSuit.Hearts)

  val TwoOfDiamonds = Card(CardRank.Two, CardSuit.Diamonds)
  val ThreeOfDiamonds = Card(CardRank.Three, CardSuit.Diamonds)
  val FourOfDiamonds = Card(CardRank.Four, CardSuit.Diamonds)
  val FiveOfDiamonds = Card(CardRank.Five, CardSuit.Diamonds)
  val SixOfDiamonds = Card(CardRank.Six, CardSuit.Diamonds)
  val SevenOfDiamonds = Card(CardRank.Seven, CardSuit.Diamonds)
  val EightOfDiamonds = Card(CardRank.Eight, CardSuit.Diamonds)
  val NineOfDiamonds = Card(CardRank.Nine, CardSuit.Diamonds)
  val TenOfDiamonds = Card(CardRank.Ten, CardSuit.Diamonds)
  val JackOfDiamonds = Card(CardRank.Jack, CardSuit.Diamonds)
  val QueenOfDiamonds = Card(CardRank.Queen, CardSuit.Diamonds)
  val KingOfDiamonds = Card(CardRank.King, CardSuit.Diamonds)
  val AceOfDiamonds = Card(CardRank.Ace, CardSuit.Diamonds)

  val TwoOfClubs = Card(CardRank.Two, CardSuit.Clubs)
  val ThreeOfClubs = Card(CardRank.Three, CardSuit.Clubs)
  val FourOfClubs = Card(CardRank.Four, CardSuit.Clubs)
  val FiveOfClubs = Card(CardRank.Five, CardSuit.Clubs)
  val SixOfClubs = Card(CardRank.Six, CardSuit.Clubs)
  val SevenOfClubs = Card(CardRank.Seven, CardSuit.Clubs)
  val EightOfClubs = Card(CardRank.Eight, CardSuit.Clubs)
  val NineOfClubs = Card(CardRank.Nine, CardSuit.Clubs)
  val TenOfClubs = Card(CardRank.Ten, CardSuit.Clubs)
  val JackOfClubs = Card(CardRank.Jack, CardSuit.Clubs)
  val QueenOfClubs = Card(CardRank.Queen, CardSuit.Clubs)
  val KingOfClubs = Card(CardRank.King, CardSuit.Clubs)
  val AceOfClubs = Card(CardRank.Ace, CardSuit.Clubs)

  val TwoOfSpades = Card(CardRank.Two, CardSuit.Spades)
  val ThreeOfSpades = Card(CardRank.Three, CardSuit.Spades)
  val FourOfSpades = Card(CardRank.Four, CardSuit.Spades)
  val FiveOfSpades = Card(CardRank.Five, CardSuit.Spades)
  val SixOfSpades = Card(CardRank.Six, CardSuit.Spades)
  val SevenOfSpades = Card(CardRank.Seven, CardSuit.Spades)
  val EightOfSpades = Card(CardRank.Eight, CardSuit.Spades)
  val NineOfSpades = Card(CardRank.Nine, CardSuit.Spades)
  val TenOfSpades = Card(CardRank.Ten, CardSuit.Spades)
  val JackOfSpades = Card(CardRank.Jack, CardSuit.Spades)
  val QueenOfSpades = Card(CardRank.Queen, CardSuit.Spades)
  val KingOfSpades = Card(CardRank.King, CardSuit.Spades)
  val AceOfSpades = Card(CardRank.Ace, CardSuit.Spades)

  val standardDeckCards: Vector[Card] = {
    val builder = Vector.newBuilder[Card]
    for (rank <- CardRank.values; suit <- CardSuit.values) {
      builder += Card(rank, suit)
    }
    builder.result()
  }
}
