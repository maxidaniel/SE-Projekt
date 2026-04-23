package de.htwg_konstanz.se.models

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

enum Card(val rank: CardRank, val suit: CardSuit) {
  override def toString: String = s"${if (rank == null) '?' else rank.symbol} ${if (suit == null) '?' else suit.symbol}"

  case Unknown extends Card(null, null)

  case TwoOfHearts extends Card(CardRank.Two, CardSuit.Hearts)
  case ThreeOfHearts extends Card(CardRank.Three, CardSuit.Hearts)
  case FourOfHearts extends Card(CardRank.Four, CardSuit.Hearts)
  case FiveOfHearts extends Card(CardRank.Five, CardSuit.Hearts)
  case SixOfHearts extends Card(CardRank.Six, CardSuit.Hearts)
  case SevenOfHearts extends Card(CardRank.Seven, CardSuit.Hearts)
  case EightOfHearts extends Card(CardRank.Eight, CardSuit.Hearts)
  case NineOfHearts extends Card(CardRank.Nine, CardSuit.Hearts)
  case TenOfHearts extends Card(CardRank.Ten, CardSuit.Hearts)
  case JackOfHearts extends Card(CardRank.Jack, CardSuit.Hearts)
  case QueenOfHearts extends Card(CardRank.Queen, CardSuit.Hearts)
  case KingOfHearts extends Card(CardRank.King, CardSuit.Hearts)
  case AceOfHearts extends Card(CardRank.Ace, CardSuit.Hearts)

  case TwoOfDiamonds extends Card(CardRank.Two, CardSuit.Diamonds)
  case ThreeOfDiamonds extends Card(CardRank.Three, CardSuit.Diamonds)
  case FourOfDiamonds extends Card(CardRank.Four, CardSuit.Diamonds)
  case FiveOfDiamonds extends Card(CardRank.Five, CardSuit.Diamonds)
  case SixOfDiamonds extends Card(CardRank.Six, CardSuit.Diamonds)
  case SevenOfDiamonds extends Card(CardRank.Seven, CardSuit.Diamonds)
  case EightOfDiamonds extends Card(CardRank.Eight, CardSuit.Diamonds)
  case NineOfDiamonds extends Card(CardRank.Nine, CardSuit.Diamonds)
  case TenOfDiamonds extends Card(CardRank.Ten, CardSuit.Diamonds)
  case JackOfDiamonds extends Card(CardRank.Jack, CardSuit.Diamonds)
  case QueenOfDiamonds extends Card(CardRank.Queen, CardSuit.Diamonds)
  case KingOfDiamonds extends Card(CardRank.King, CardSuit.Diamonds)
  case AceOfDiamonds extends Card(CardRank.Ace, CardSuit.Diamonds)

  case TwoOfClubs extends Card(CardRank.Two, CardSuit.Clubs)
  case ThreeOfClubs extends Card(CardRank.Three, CardSuit.Clubs)
  case FourOfClubs extends Card(CardRank.Four, CardSuit.Clubs)
  case FiveOfClubs extends Card(CardRank.Five, CardSuit.Clubs)
  case SixOfClubs extends Card(CardRank.Six, CardSuit.Clubs)
  case SevenOfClubs extends Card(CardRank.Seven, CardSuit.Clubs)
  case EightOfClubs extends Card(CardRank.Eight, CardSuit.Clubs)
  case NineOfClubs extends Card(CardRank.Nine, CardSuit.Clubs)
  case TenOfClubs extends Card(CardRank.Ten, CardSuit.Clubs)
  case JackOfClubs extends Card(CardRank.Jack, CardSuit.Clubs)
  case QueenOfClubs extends Card(CardRank.Queen, CardSuit.Clubs)
  case KingOfClubs extends Card(CardRank.King, CardSuit.Clubs)
  case AceOfClubs extends Card(CardRank.Ace, CardSuit.Clubs)

  case TwoOfSpades extends Card(CardRank.Two, CardSuit.Spades)
  case ThreeOfSpades extends Card(CardRank.Three, CardSuit.Spades)
  case FourOfSpades extends Card(CardRank.Four, CardSuit.Spades)
  case FiveOfSpades extends Card(CardRank.Five, CardSuit.Spades)
  case SixOfSpades extends Card(CardRank.Six, CardSuit.Spades)
  case SevenOfSpades extends Card(CardRank.Seven, CardSuit.Spades)
  case EightOfSpades extends Card(CardRank.Eight, CardSuit.Spades)
  case NineOfSpades extends Card(CardRank.Nine, CardSuit.Spades)
  case TenOfSpades extends Card(CardRank.Ten, CardSuit.Spades)
  case JackOfSpades extends Card(CardRank.Jack, CardSuit.Spades)
  case QueenOfSpades extends Card(CardRank.Queen, CardSuit.Spades)
  case KingOfSpades extends Card(CardRank.King, CardSuit.Spades)
  case AceOfSpades extends Card(CardRank.Ace, CardSuit.Spades)
}

object Card {
  val standardDeckCards: Vector[Card] = Card.values.filterNot(card => card == Card.Unknown).toVector
}
