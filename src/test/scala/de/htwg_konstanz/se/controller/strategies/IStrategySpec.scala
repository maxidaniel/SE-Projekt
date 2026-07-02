package de.htwg_konstanz.se.controller.strategies

import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

class IStrategySpec extends AnyWordSpec:
  "IStrategy companion" should:
    "resolve PlayBestCardStrategy by class name" in:
      IStrategy.resolve("PlayBestCardStrategy") shouldBe defined
      IStrategy.resolve("PlayBestCardStrategy").get shouldBe a[PlayBestCardStrategy]

    "resolve PlayLowestPossibleCardStrategy by class name" in:
      IStrategy.resolve("PlayLowestPossibleCardStrategy") shouldBe defined
      IStrategy.resolve("PlayLowestPossibleCardStrategy").get shouldBe a[PlayLowestPossibleCardStrategy]

    "resolve PlayRandomCardStrategy by class name" in:
      IStrategy.resolve("PlayRandomCardStrategy") shouldBe defined
      IStrategy.resolve("PlayRandomCardStrategy").get shouldBe a[PlayRandomCardStrategy]

    "return None for unknown strategy" in:
      IStrategy.resolve("UnknownStrategy") shouldBe None

    "get name for PlayBestCardStrategy" in:
      IStrategy.nameOf(PlayBestCardStrategy()) shouldBe Some("PlayBestCardStrategy")

    "get name for PlayLowestPossibleCardStrategy" in:
      IStrategy.nameOf(PlayLowestPossibleCardStrategy()) shouldBe Some("PlayLowestPossibleCardStrategy")

    "get name for PlayRandomCardStrategy" in:
      IStrategy.nameOf(PlayRandomCardStrategy()) shouldBe Some("PlayRandomCardStrategy")

    "return all strategies" in:
      IStrategy.allStrategies should have size 3
      IStrategy.allStrategies should contain key "PlayBestCardStrategy"
      IStrategy.allStrategies should contain key "PlayLowestPossibleCardStrategy"
      IStrategy.allStrategies should contain key "PlayRandomCardStrategy"
