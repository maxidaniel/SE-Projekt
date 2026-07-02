package de.htwg_konstanz.se.controller.strategies

import de.htwg_konstanz.se.models.Card

trait IStrategy {
  def name: String
  def play(cards: Vector[Card], lastPlayed: Option[Card], playedCards: Vector[Card]): Option[Card]
  def canPlay(cards: Vector[Card], lastPlayed: Option[Card], playedCards: Vector[Card]): Boolean
  def shouldAcceptExchange(hand: Vector[Card], offeredCards: Vector[Card], position: String): Boolean
}

object IStrategy:
  private val strategies: Map[String, IStrategy] = Map(
    classOf[PlayBestCardStrategy].getSimpleName -> PlayBestCardStrategy(),
    classOf[PlayLowestPossibleCardStrategy].getSimpleName -> PlayLowestPossibleCardStrategy(),
    classOf[PlayRandomCardStrategy].getSimpleName -> PlayRandomCardStrategy()
  )

  def resolve(name: String): Option[IStrategy] = strategies.get(name)

  def nameOf(strategy: IStrategy): Option[String] =
    strategies.find(_._2.getClass == strategy.getClass).map(_._1)

  def allStrategies: Map[String, IStrategy] = strategies
