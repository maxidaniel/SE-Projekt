case class Cell(value:Int) {
 def isSet:Boolean = value != 0
}

val cell1= Cell(2)
cell1.isSet

val cell2= Cell(0)
cell2.isSet

val cell3= Cell(4)
cell3.isSet

case class Field(cells: Array[Cell])

val field1 = Field(Array.ofDim[Cell](1))
field1.cells(0)=cell1

case class House(cells:Vector[Cell])

val house = House(Vector(cell1,cell2))

house.cells(0).value
house.cells(0).isSet

import de.htwg_konstanz.se.*
import de.htwg_konstanz.se.CardColour.Club
import de.htwg_konstanz.se.CardKind.King

val player = Player("Test", Vector.empty);
player.getName

val card = Card(Club, 12, King)