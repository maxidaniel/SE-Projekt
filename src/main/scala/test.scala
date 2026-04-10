object Test extends App {

  def test(): Unit = println("hi")

  test()

  class Zug(val zugnr: Int, val spiel: String) {
    def next(): Zug = {
      new Zug(this.zugnr + 1, this.spiel)
    }
  }

  val zug = new Zug(1, "Hi")
  println(zug.zugnr)
  println(zug.spiel)

  val newzug = zug.next()
  println(newzug.zugnr)
  println(newzug.spiel)
}