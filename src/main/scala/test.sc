object test {
  def test = println("hi")
}

test.test

class Zug(val zugnr: Int, val spiel: String) {
    def next(): Zug = {
        new Zug(this.zugnr + 1, this.spiel)
    }
}

val zug = new Zug(1, "Hi")
zug.zugnr
zug.spiel

val newzug = zug.next()
newzug.zugnr
newzug.spiel