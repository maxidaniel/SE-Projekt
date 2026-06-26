package de.htwg_konstanz.se.util

import de.htwg_konstanz.se.models.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class ProviderSpec extends AnyWordSpec {
  private class TestProvider extends Provider

  "A Provider" should {
    "add and remove listeners" in {
      val provider = new TestProvider
      val l1 = new Listener { override def onEvent(event: GameEvent): Unit = () }
      val l2 = new Listener { override def onEvent(event: GameEvent): Unit = () }

      provider.add(l1)
      provider.add(l2)
      provider.listeners should be(Vector(l1, l2))

      provider.remove(l1)
      provider.listeners should be(Vector(l2))
    }

    "notify all listeners with the same event" in {
      val provider = new TestProvider
      val event = PlayerJoinEvent(HumanPlayer("Alice"), new Game())
      var seen1: Option[GameEvent] = None
      var seen2: Option[GameEvent] = None

      provider.add((e: GameEvent) => seen1 = Some(e))
      provider.add((e: GameEvent) => seen2 = Some(e))

      provider.notifyEvent(event)

      seen1 should be(Some(event))
      seen2 should be(Some(event))
    }
  }
}
