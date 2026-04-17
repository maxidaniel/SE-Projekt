package de.htwg_konstanz.se

enum CardKind(val value: Int) {
  case Two extends CardKind(2)
  case Three extends CardKind(3)
  case Four extends CardKind(4)
  case Five extends CardKind(5)
  case Six extends CardKind(6)
  case Seven extends CardKind(7)
  case Eight extends CardKind(8)
  case Nine extends CardKind(9)
  case Ten extends CardKind(10)
  case Jack extends CardKind(11)
  case Queen extends CardKind(12)
  case King extends CardKind(13)
  case Ace extends CardKind(14)
}