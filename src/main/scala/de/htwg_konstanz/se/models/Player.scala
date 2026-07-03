package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.controller.strategies.IStrategy
import de.htwg_konstanz.se.models.PlayerType.{Computer, Human, Unknown}
import play.api.libs.json.*
import scala.xml.*

import java.util.UUID

enum PlayerType(val strategy: Option[IStrategy]) {
  case Human extends PlayerType(None)
  case Computer(theStrategy: IStrategy) extends PlayerType(Some(theStrategy))
  case Unknown extends PlayerType(None)
}

sealed trait IPlayer(val name: String, val playerType: PlayerType, val id: UUID = UUID.randomUUID()) {
  override def equals(other: Any): Boolean = other match {
    case p: IPlayer => p.id == this.id
    case _          => false
  }
  override def hashCode(): Int = id.hashCode()
}

object IPlayer:
  given Writes[IPlayer] with
    def writes(player: IPlayer): JsValue = player match
      case h: HumanPlayer =>
        Json.obj(
          "type" -> "Human",
          "name" -> h.name,
          "id" -> h.id.toString
        )
      case c: ComputerPlayer =>
        Json.obj(
          "type" -> "Computer",
          "name" -> c.name,
          "id" -> c.id.toString,
          "strategy" -> IStrategy.nameOf(c.playerType.strategy.orNull).getOrElse("Unknown")
        )
      case UnknownPlayer =>
        Json.obj(
          "type" -> "Unknown",
          "name" -> "Unknown",
          "id" -> UnknownPlayer.id.toString
        )

  given Reads[IPlayer] = Reads { json =>
    val playerType = (json \ "type").asOpt[String].getOrElse("Unknown")
    val name = (json \ "name").asOpt[String].getOrElse("Unknown")
    val id = (json \ "id").asOpt[String].map(UUID.fromString).getOrElse(UUID.randomUUID())

    playerType match
      case "Human"    => JsSuccess(HumanPlayer(name, id))
      case "Computer" =>
        val strategyName = (json \ "strategy").asOpt[String].getOrElse("")
        IStrategy.resolve(strategyName) match
          case Some(strategy) => JsSuccess(ComputerPlayer(name, strategy, id))
          case None           => JsError(s"Unknown strategy: $strategyName")
      case _ => JsSuccess(UnknownPlayer)
  }

  def toXml(player: IPlayer): Elem = player match
    case h: HumanPlayer =>
      <player type="Human" name={h.name} id={h.id.toString}/>
    case c: ComputerPlayer =>
      val strategyName = IStrategy.nameOf(c.playerType.strategy.orNull).getOrElse("Unknown")
      <player type="Computer" name={c.name} id={c.id.toString} strategy={strategyName}/>
    case UnknownPlayer =>
      <player type="Unknown" name="Unknown" id={UnknownPlayer.id.toString}/>

  def fromXml(xml: NodeSeq): IPlayer =
    val playerType = (xml \ "@type").text
    val name = (xml \ "@name").text
    val id = (xml \ "@id").text match
      case s if s.nonEmpty => UUID.fromString(s)
      case _               => UUID.randomUUID()

    playerType match
      case "Human"    => HumanPlayer(name, id)
      case "Computer" =>
        val strategyName = (xml \ "@strategy").text
        IStrategy.resolve(strategyName) match
          case Some(strategy) => ComputerPlayer(name, strategy, id)
          case None           => UnknownPlayer
      case _ => UnknownPlayer

case class HumanPlayer(myName: String, override val id: UUID = UUID.randomUUID()) extends IPlayer(myName, Human, id)

case class ComputerPlayer(myName: String, strategy: IStrategy, override val id: UUID = UUID.randomUUID())
    extends IPlayer(myName, Computer(strategy), id)

object UnknownPlayer extends IPlayer("Unknown", Unknown)
