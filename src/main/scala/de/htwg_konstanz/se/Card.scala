package de.htwg_konstanz.se

class Card(colour: CardColour, value: Int, kind: CardKind) {
  def getColour: CardColour = this.colour
  def getValue: Int = this.value
  def getKind: CardKind = this.kind
}
