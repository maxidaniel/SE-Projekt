package de.htwg_konstanz.se.models

import scala.collection.mutable.ListBuffer
import scala.xml.*
import play.api.libs.json.*

enum CardSuit(val name: String, val symbol: String) {
  case Hearts extends CardSuit("hearts", "♥")
  case Diamonds extends CardSuit("diamonds", "♦")
  case Clubs extends CardSuit("clubs", "♣")
  case Spades extends CardSuit("spades", "♠")
}

object CardSuit:
  given Writes[CardSuit] = (suit: CardSuit) => JsString(suit.name)
  given Reads[CardSuit] = Reads {
    case JsString(name) =>
      CardSuit.values.find(_.name == name) match
        case Some(suit) => JsSuccess(suit)
        case None       => JsError(s"Unknown card suit: $name")
    case other => JsError(s"Expected string for CardSuit, got: $other")
  }

  def fromXml(xml: NodeSeq): CardSuit = xml.text match
    case "hearts"   => CardSuit.Hearts
    case "diamonds" => CardSuit.Diamonds
    case "clubs"    => CardSuit.Clubs
    case "spades"   => CardSuit.Spades
    case _          => throw new IllegalArgumentException(s"Unknown card suit: ${xml.text}")

enum CardRank(val name: String, val symbol: String, val value: Int) {
  case Two extends CardRank("two", "2", 2)
  case Three extends CardRank("three", "3", 3)
  case Four extends CardRank("four", "4", 4)
  case Five extends CardRank("five", "5", 5)
  case Six extends CardRank("six", "6", 6)
  case Seven extends CardRank("seven", "7", 7)
  case Eight extends CardRank("eight", "8", 8)
  case Nine extends CardRank("nine", "9", 9)
  case Ten extends CardRank("ten", "10", 10)
  case Jack extends CardRank("jack", "J", 11)
  case Queen extends CardRank("queen", "Q", 12)
  case King extends CardRank("king", "K", 13)
  case Ace extends CardRank("ace", "A", 14)
}

object CardRank:
  given Writes[CardRank] = (rank: CardRank) => JsString(rank.name)
  given Reads[CardRank] = Reads {
    case JsString(name) =>
      CardRank.values.find(_.name == name) match
        case Some(rank) => JsSuccess(rank)
        case None       => JsError(s"Unknown card rank: $name")
    case other => JsError(s"Expected string for CardRank, got: $other")
  }

  def fromXml(xml: NodeSeq): CardRank = xml.text match
    case "two"   => CardRank.Two
    case "three" => CardRank.Three
    case "four"  => CardRank.Four
    case "five"  => CardRank.Five
    case "six"   => CardRank.Six
    case "seven" => CardRank.Seven
    case "eight" => CardRank.Eight
    case "nine"  => CardRank.Nine
    case "ten"   => CardRank.Ten
    case "jack"  => CardRank.Jack
    case "queen" => CardRank.Queen
    case "king"  => CardRank.King
    case "ace"   => CardRank.Ace
    case _       => throw new IllegalArgumentException(s"Unknown card rank: ${xml.text}")

case class Card(rank: CardRank, suit: CardSuit) {
  override def toString: String = s"${if (rank == null) "?" else rank.symbol} ${if (suit == null) "?" else suit.symbol}"

  def render(scale: Int = 1): Vector[String] = {
    val r = if (rank == null) "?" else rank.symbol
    val s = if (suit == null) "?" else suit.symbol

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

object Card:
  val Unknown = Card(null, null)

  given Format[Card] with
    def writes(card: Card): JsValue =
      if card.rank == null || card.suit == null then Json.obj("rank" -> JsNull, "suit" -> JsNull)
      else Json.obj("rank" -> card.rank, "suit" -> card.suit)

    def reads(json: JsValue): JsResult[Card] =
      (json \ "rank").validate[CardRank] match
        case JsError(_)         => JsSuccess(Card.Unknown)
        case JsSuccess(rank, _) =>
          (json \ "suit").validate[CardSuit] match
            case JsError(_)         => JsSuccess(Card.Unknown)
            case JsSuccess(suit, _) => JsSuccess(Card(rank, suit))

  def toXml(card: Card): Elem =
    if card.rank == null || card.suit == null then
      <card rank="" suit=""/>
    else <card rank={card.rank.name} suit={card.suit.name}/>

  def fromXml(xml: NodeSeq): Card =
    val rankAttr = (xml \ "@rank").text
    val suitAttr = (xml \ "@suit").text
    if rankAttr.isEmpty || suitAttr.isEmpty then Card.Unknown
    else Card(CardRank.fromXml(Text(rankAttr)), CardSuit.fromXml(Text(suitAttr)))

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

  val standardDeckCards: Vector[Card] =
    val builder = Vector.newBuilder[Card]
    for rank <- CardRank.values; suit <- CardSuit.values do builder += Card(rank, suit)
    builder.result()
