package de.htwg_konstanz.se.models

import de.htwg_konstanz.se.models.GameState.*
import play.api.libs.json.*
import scala.xml.*

import java.util.UUID
import scala.util.Try

case class Game(
    playerHands: Map[IPlayer, Vector[Card]],
    playedCards: Vector[Card],
    state: GameState,
    currentPlayer: Option[IPlayer] = None,
    trickCount: Int = 0,
    trickRank: Option[CardRank] = None,
    trickLeader: Option[IPlayer] = None,
    passedPlayers: Set[IPlayer] = Set.empty,
    scoredRanks: Map[IPlayer, Int] = Map.empty,
    roundNumber: Int = 1,
    finishOrder: Vector[IPlayer] = Vector.empty,
    isFirstTrick: Boolean = true
) {
  def this() = {
    this(Map.empty, Vector.empty, WaitingForPlayers, None, 0, None, None, Set.empty, Map.empty, 1, Vector.empty)
  }

  def join(player: IPlayer): Try[Game] = state.transition(this, Join(player))

  def quit(player: IPlayer): Try[Game] = state.transition(this, Quit(player))

  def start(): Try[Game] = state.transition(this, Start)

  def abort(): Try[Game] = state.transition(this, Abort)

  def deal(): Try[Game] = state.transition(this, Deal)

  def playCard(player: IPlayer, card: Card): Try[Game] = state.transition(this, PlayCard(player, card))

  def passTrick(player: IPlayer): Try[Game] = state.transition(this, PassTrick(player))

  def nextRound(): Try[Game] = state.transition(this, NextRound)
}

case class Deck(cards: Vector[Card]) {
  def this() = {
    this(Card.standardDeckCards)
  }

  def shuffle(): Deck = {
    copy(cards = scala.util.Random.shuffle(cards))
  }
}

object Game:
  private def readPlayerHands(json: JsValue): JsResult[Map[IPlayer, Vector[Card]]] =
    json.validate[JsArray].flatMap { arr =>
      val results = arr.value.map { entry =>
        for
          player <- (entry \ "player").validate[IPlayer]
          cards <- (entry \ "cards").validate[Vector[Card]]
        yield player -> cards
      }
      results.foldRight(JsSuccess(Map.empty[IPlayer, Vector[Card]]): JsResult[Map[IPlayer, Vector[Card]]]) {
        case (JsSuccess(k, _), JsSuccess(acc, _)) => JsSuccess(acc + k)
        case (e: JsError, _)                      => e
        case (_, e: JsError)                      => e
      }
    }

  private def readScoredRanks(json: JsValue, players: Set[IPlayer]): JsResult[Map[IPlayer, Int]] =
    json.validate[JsArray].flatMap { arr =>
      val results = arr.value.map { entry =>
        for
          playerJson <- (entry \ "player").validate[IPlayer]
          score <- (entry \ "score").validate[Int]
        yield (playerJson.id, score)
      }
      results
        .foldRight(JsSuccess(Map.empty[UUID, Int]): JsResult[Map[UUID, Int]]) {
          case (JsSuccess((id, score), _), JsSuccess(acc, _)) => JsSuccess(acc + (id -> score))
          case (e: JsError, _)                                => e
          case (_, e: JsError)                                => e
        }
        .map { idScoreMap =>
          val idToPlayer = players.map(p => p.id -> p).toMap
          idScoreMap.flatMap { case (id, score) => idToPlayer.get(id).map(_ -> score) }
        }
    }

  given Format[Game] with
    def writes(game: Game): JsValue =
      val playerHandsJson = JsArray(game.playerHands.map { case (player, cards) =>
        Json.obj(
          "player" -> Json.toJson(player),
          "cards" -> Json.toJson(cards)
        )
      }.toSeq)

      val currentPlayerJson = game.currentPlayer.map(p => Json.toJson(p)).getOrElse(JsNull)
      val trickRankJson = game.trickRank.map(r => Json.toJson(r)).getOrElse(JsNull)
      val trickLeaderJson = game.trickLeader.map(p => Json.toJson(p)).getOrElse(JsNull)
      val passedPlayersJson = Json.toJson(game.passedPlayers.toSeq)
      val scoredRanksJson = JsArray(game.scoredRanks.map { case (player, score) =>
        Json.obj(
          "player" -> Json.toJson(player),
          "score" -> score
        )
      }.toSeq)
      val finishOrderJson = Json.toJson(game.finishOrder)

      Json.obj(
        "playerHands" -> playerHandsJson,
        "playedCards" -> Json.toJson(game.playedCards),
        "state" -> Json.toJson(game.state),
        "currentPlayer" -> currentPlayerJson,
        "trickCount" -> game.trickCount,
        "trickRank" -> trickRankJson,
        "trickLeader" -> trickLeaderJson,
        "passedPlayers" -> passedPlayersJson,
        "scoredRanks" -> scoredRanksJson,
        "roundNumber" -> game.roundNumber,
        "finishOrder" -> finishOrderJson,
        "isFirstTrick" -> game.isFirstTrick
      )

    def reads(json: JsValue): JsResult[Game] =
      val playerHandsJson = (json \ "playerHands").asOpt[JsArray].getOrElse(JsArray.empty)
      val scoredRanksJson = (json \ "scoredRanks").asOpt[JsArray].getOrElse(JsArray.empty)

      for
        playerHands <- readPlayerHands(playerHandsJson)
        playedCards <- (json \ "playedCards").validate[Vector[Card]]
        state <- (json \ "state").validate[GameState]
        currentPlayer <- (json \ "currentPlayer").validateOpt[IPlayer]
        trickCount <- (json \ "trickCount").validate[Int]
        trickRank <- (json \ "trickRank").validateOpt[CardRank]
        trickLeader <- (json \ "trickLeader").validateOpt[IPlayer]
        passedPlayers <- (json \ "passedPlayers").validate[Set[IPlayer]]
        scoredRanks <- readScoredRanks(scoredRanksJson, playerHands.keySet)
        finishOrder <- (json \ "finishOrder").validate[Vector[IPlayer]]
      yield Game(
        playerHands = playerHands,
        playedCards = playedCards,
        state = state,
        currentPlayer = currentPlayer,
        trickCount = trickCount,
        trickRank = trickRank,
        trickLeader = trickLeader,
        passedPlayers = passedPlayers,
        scoredRanks = scoredRanks,
        roundNumber = (json \ "roundNumber").validate[Int].getOrElse(1),
        finishOrder = finishOrder,
        isFirstTrick = (json \ "isFirstTrick").validate[Boolean].getOrElse(true)
      )

  def toXml(game: Game): Elem =
    val playerHandsChildren = game.playerHands.map { case (player, cards) =>
      <entry>
        {IPlayer.toXml(player)}
        <cards>{cards.map(Card.toXml)}</cards>
      </entry>
    }.toSeq

    val playedCardsChildren = game.playedCards.map(Card.toXml)
    val currentPlayerXml =
      game.currentPlayer.map(p => <currentPlayer>{IPlayer.toXml(p)}</currentPlayer>).getOrElse(<currentPlayer/>)
    val trickRankChild = game.trickRank.map(r => <trickRank>{r.name}</trickRank>).getOrElse(<trickRank/>)
    val trickLeaderXml =
      game.trickLeader.map(p => <trickLeader>{IPlayer.toXml(p)}</trickLeader>).getOrElse(<trickLeader/>)
    val passedPlayersChildren = game.passedPlayers.map(IPlayer.toXml).toSeq
    val scoredRanksChildren = game.scoredRanks.map { case (player, score) =>
      <entry>
        {IPlayer.toXml(player)}
        <score>{score}</score>
      </entry>
    }.toSeq
    val finishOrderChildren = game.finishOrder.map(IPlayer.toXml)

    <game roundNumber={game.roundNumber.toString} isFirstTrick={game.isFirstTrick.toString}>
      <playerHands>{playerHandsChildren}</playerHands>
      <playedCards>{playedCardsChildren}</playedCards>
      {GameState.toXml(game.state)}
      {currentPlayerXml}
      <trickCount>{game.trickCount}</trickCount>
      {trickRankChild}
      {trickLeaderXml}
      <passedPlayers>{passedPlayersChildren}</passedPlayers>
      <scoredRanks>{scoredRanksChildren}</scoredRanks>
      <finishOrder>{finishOrderChildren}</finishOrder>
    </game>

  def fromXml(xml: NodeSeq): Game =
    val playerHands = (xml \ "playerHands" \ "entry").map { entry =>
      val player = IPlayer.fromXml(entry \ "player")
      val cards = (entry \ "cards" \ "card").map(Card.fromXml).toVector
      player -> cards
    }.toMap

    val playedCards = (xml \ "playedCards" \ "card").map(Card.fromXml).toVector
    val state = GameState.fromXml(xml \ "state")
    val currentPlayer = (xml \ "currentPlayer").headOption.flatMap(n =>
      if (n \ "player").nonEmpty then Some(IPlayer.fromXml(n \ "player")) else None
    )
    val trickCount = (xml \ "trickCount").text.toIntOption.getOrElse(0)
    val trickRank = (xml \ "trickRank").text match
      case s if s.nonEmpty => Some(CardRank.values.find(_.name == s).getOrElse(CardRank.Three))
      case _               => None
    val trickLeader = (xml \ "trickLeader").headOption.flatMap(n =>
      if (n \ "player").nonEmpty then Some(IPlayer.fromXml(n \ "player")) else None
    )
    val passedPlayers = (xml \ "passedPlayers" \ "player").map(IPlayer.fromXml).toSet
    val scoredRanks = (xml \ "scoredRanks" \ "entry").map { entry =>
      val player = IPlayer.fromXml(entry \ "player")
      val score = (entry \ "score").text.toIntOption.getOrElse(0)
      player -> score
    }.toMap
    val finishOrder = (xml \ "finishOrder" \ "player").map(IPlayer.fromXml).toVector
    val roundNumber = (xml \ "@roundNumber").text.toIntOption.getOrElse(1)
    val isFirstTrick = (xml \ "@isFirstTrick").text.toBooleanOption.getOrElse(true)

    Game(
      playerHands = playerHands,
      playedCards = playedCards,
      state = state,
      currentPlayer = currentPlayer,
      trickCount = trickCount,
      trickRank = trickRank,
      trickLeader = trickLeader,
      passedPlayers = passedPlayers,
      scoredRanks = scoredRanks,
      roundNumber = roundNumber,
      finishOrder = finishOrder,
      isFirstTrick = isFirstTrick
    )

  private val rankPower: Map[CardRank, Int] = Map(
    CardRank.Three -> 1,
    CardRank.Four -> 2,
    CardRank.Five -> 3,
    CardRank.Six -> 4,
    CardRank.Seven -> 5,
    CardRank.Eight -> 6,
    CardRank.Nine -> 7,
    CardRank.Ten -> 8,
    CardRank.Jack -> 9,
    CardRank.Queen -> 10,
    CardRank.King -> 11,
    CardRank.Ace -> 12,
    CardRank.Two -> 13
  )

  def getPower(card: Card): Int = rankPower(card.rank)

  def getRankPower(rank: CardRank): Int = rankPower(rank)

  def canBeat(current: Card, previous: Card): Boolean = {
    rankPower(current.rank) > rankPower(previous.rank)
  }

  def isBurnCard(card: Card): Boolean = card.rank == CardRank.Two

  def isFourOfAKind(cards: Seq[Card]): Boolean = {
    cards.length == 4 && cards.forall(_.rank == cards.head.rank)
  }

  val PresidentScore = 2
  val VicePresidentScore = 1
  val OtherScore = 0

  def scoreForPosition(position: Int, totalPlayers: Int): Int = position match {
    case 0 => PresidentScore
    case 1 => VicePresidentScore
    case _ => OtherScore
  }

  def getBestCards(hand: Vector[Card], count: Int): Vector[Card] = {
    hand.sortBy(c => -getPower(c)).take(count)
  }

  def getWorstCards(hand: Vector[Card], count: Int): Vector[Card] = {
    hand.sortBy(c => getPower(c)).take(count)
  }

  def exchangeCards(
      president: IPlayer,
      scum: IPlayer,
      vicePresident: Option[IPlayer],
      viceScum: Option[IPlayer],
      playerHands: Map[IPlayer, Vector[Card]]
  ): Map[IPlayer, Vector[Card]] = {
    var hands = playerHands

    val scumBest = getBestCards(hands(scum), 2)
    val presWorst = getWorstCards(hands(president), 2)
    hands = hands.updated(president, hands(president).filterNot(presWorst.contains) ++ scumBest)
    hands = hands.updated(scum, hands(scum).filterNot(scumBest.contains) ++ presWorst)

    (vicePresident, viceScum) match {
      case (Some(vp), Some(vscum)) =>
        val vscumBest = getBestCards(hands(vscum), 1)
        val vpWorst = getWorstCards(hands(vp), 1)
        hands = hands.updated(vp, hands(vp).filterNot(vpWorst.contains) ++ vscumBest)
        hands = hands.updated(vscum, hands(vscum).filterNot(vscumBest.contains) ++ vpWorst)
      case _ =>
    }

    hands
  }
