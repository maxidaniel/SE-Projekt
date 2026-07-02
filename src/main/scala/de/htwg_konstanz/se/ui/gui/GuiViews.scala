package de.htwg_konstanz.se.ui.gui

import de.htwg_konstanz.se.controller.strategies.IStrategy
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.ui.gui.views.*
import scalafx.collections.ObservableHashSet
import scalafx.geometry.Pos
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.scene.{Cursor, Parent}

/** Shared view-building helpers and factory methods for view case classes. */
object GuiViews {

  private val Logo: Vector[String] =
    """|.------..------..------..------..------..------..------..------..------.
       ||P.--. ||R.--. ||E.--. ||S.--. ||I.--. ||D.--. ||E.--. ||N.--. ||T.--. |
       || :/\: || :(): || (\/) || :/\: || (\/) || :/\: || (\/) || :(): || :/\: |
       || (__) || ()() || :\/: || :\/: || :\/: || (__) || :\/: || ()() || (__) |
       || '--'P|| '--'R|| '--'E|| '--'S|| '--'I|| '--'D|| '--'E|| '--'N|| '--'T|
       |`------'`------'`------'`------'`------'`------'`------'`------'`------'""".stripMargin.linesIterator.toVector

  def logoText: String = Logo.mkString("\n")

  def menuView(p: IGuiPresenter): Parent = MenuView(p)
  def lobbyView(p: IGuiPresenter): Parent = LobbyView(p)
  def gameView(p: IGuiPresenter): Parent = GameView(p)
  def resultView(p: IGuiPresenter): Parent = ResultView(p)

  // ── Shared helpers ─────────────────────────────────────────────────────

  def statusLabel(p: IGuiPresenter): Label = new Label(p.statusMessage) {
    wrapText = true
    style =
      if p.isErrorMessage then
        "-fx-padding: 10; -fx-background-color: #fecaca; -fx-background-radius: 6; -fx-text-fill: #991b1b;"
      else "-fx-padding: 10; -fx-background-color: #f3f5f8; -fx-background-radius: 6;"
  }

  def titleLabel(text: String): Label = new Label(text) {
    style = "-fx-font-size: 28px; -fx-font-weight: bold;"
  }

  def rulesPanel(): VBox = new VBox {
    spacing = 8
    alignment = Pos.CenterLeft
    maxWidth = 560
    children = Seq(
      new Label("Controls") {
        style = "-fx-font-size: 18px; -fx-font-weight: bold;"
      },
      new Label(
        "• Add players in the lobby.\n• Start with at least two players.\n• Follow the current table and player hands during the game.\n• Abort returns to the result screen."
      ) {
        wrapText = true
      }
    )
  }

  def section(title: String, node: scalafx.scene.Node): VBox = new VBox {
    spacing = 8
    children = Seq(
      new Label(title) {
        style = "-fx-font-size: 18px; -fx-font-weight: bold;"
      },
      node
    )
  }

  private def playerLabel(player: IPlayer, p: IGuiPresenter): String = {
    val name = player.name
    player.playerType.strategy match
      case Some(strategy) => s"$name (${strategy.name})"
      case None           => name
  }

  private def strategyLabel(player: IPlayer): String = {
    player.playerType.strategy match
      case Some(strategy) => strategy.name
      case None           => ""
  }

  def playerList(p: IGuiPresenter, game: Game, allowRemove: Boolean): VBox = {
    val entries = game.playerHands.toVector.sortBy(_._1.toString)
    if entries.isEmpty then
      return new VBox {
        spacing = 8
        children = Seq(new Label("No players have joined yet."))
      }

    val maxName = entries.map { case (player, _) => player.name.length }.max
    val maxStrategy = entries.map { case (player, _) => strategyLabel(player).length }.max
    val maxCount = entries.map { case (_, c) => s"${c.size} cards".length }.max
    val maxId = entries.map { case (id, _) => id.toString.take(8).length }.max

    new VBox {
      spacing = 8
      children = entries.map { (player, cards) =>
        val name = player.name
        val strategy = strategyLabel(player)
        val cardCount = s"${cards.size} cards"
        val playerIdStr = player.id.toString.take(8)

        new HBox {
          spacing = 10
          alignment = Pos.CenterLeft
          children = Seq(
            new Label(name) {
              minWidth = (maxName + 2) * 7.5
              style = "-fx-font-weight: bold;"
            },
            new Label(strategy) {
              minWidth = (maxStrategy + 2) * 7.5
              style = "-fx-text-fill: #6b7280;"
            },
            new Label(cardCount) {
              minWidth = (maxCount + 2) * 7.5
            },
            new Label(playerIdStr) {
              minWidth = (maxId + 2) * 7.5
              style = "-fx-text-fill: #6b7280;"
            }
          ) ++ (if allowRemove then
                  Seq(new Button("Remove") {
                    onMouseClicked = _ => p.removePlayer(player.id)
                  })
                else Seq.empty)
        }
      }
    }
  }

  def scorePanel(p: IGuiPresenter, game: Game): VBox = {
    val entries = game.playerHands.toVector.sortBy(_._1.toString)
    if entries.isEmpty then
      return new VBox {
        spacing = 8
        children = Seq(new Label("No scores yet."))
      }

    val maxName = entries.map { case (player, _) => player.name.length }.max
    val maxStrategy = entries.map { case (player, _) => strategyLabel(player).length }.max
    val maxScore = entries.map { case (player, _) => s"${game.scoredRanks.getOrElse(player, 0)} pts".length }.max
    val targetScore = 11

    new VBox {
      spacing = 6
      children = entries.map { (player, _) =>
        val name = player.name
        val strategy = strategyLabel(player)
        val score = game.scoredRanks.getOrElse(player, 0)
        val scoreProgress = math.min(score.toDouble / targetScore, 1.0)

        new HBox {
          spacing = 10
          alignment = Pos.CenterLeft
          children = Seq(
            new Label(name) {
              minWidth = (maxName + 2) * 7.5
              style = "-fx-font-weight: bold;"
            },
            new Label(strategy) {
              minWidth = (maxStrategy + 2) * 7.5
              style = "-fx-text-fill: #6b7280;"
            },
            new ProgressBar() {
              progress = scoreProgress
              prefWidth = 120
              prefHeight = 16
            },
            new Label(s"$score / $targetScore pts") {
              minWidth = (maxScore + 6) * 7.5
              style = if score >= targetScore then "-fx-text-fill: #16a34a; -fx-font-weight: bold;" else ""
            }
          )
        }
      }
    }
  }

  def handsPanel(p: IGuiPresenter, game: Game, selectedCards: ObservableHashSet[Card]): VBox = {
    val isPlaying = game.state == GameState.Playing
    val entries =
      if isPlaying then
        game.currentPlayer match {
          case Some(player) => Vector(player -> game.playerHands.getOrElse(player, Vector.empty))
          case None         => Vector.empty
        }
      else game.playerHands.toVector.sortBy(_._1.toString)

    if entries.isEmpty then
      return new VBox {
        spacing = 12
        children = Seq(new Label("No hands available."))
      }

    val maxName = entries.map { case (id, _) => id.name.length }.max

    new VBox {
      spacing = 12
      children = entries.map { (playerId, cards) =>
        val strategy = strategyLabel(playerId)
        new VBox {
          spacing = 6
          children = Seq(
            new HBox {
              spacing = 6
              alignment = Pos.CenterLeft
              children = Seq(
                new Label(playerId.name) {
                  style = "-fx-font-weight: bold;"
                },
                new Label(strategy) {
                  style = "-fx-text-fill: #6b7280;"
                }
              )
            },
            cardFlow(p, cards, Some(playerId), selectedCards)
          )
        }
      }
    }
  }

  def cardFlow(
      p: IGuiPresenter,
      cards: Seq[Card],
      playerId: Option[IPlayer],
      selectedCards: ObservableHashSet[Card]
  ): Parent = new FlowPane {
    hgap = 8
    vgap = 8
    children =
      if cards.isEmpty then Seq(new Label("No cards."))
      else cards.zipWithIndex.map { (card, index) => cardView(p, card, index, playerId, selectedCards) }
  }

  def cardView(
      p: IGuiPresenter,
      card: Card,
      index: Int,
      playerId: Option[IPlayer],
      selectedCards: ObservableHashSet[Card]
  ): VBox = {
    val red = card.suit == CardSuit.Hearts || card.suit == CardSuit.Diamonds
    val isSelectable = playerId.isDefined

    new VBox {
      alignment = Pos.Center
      spacing = 2
      minWidth = 56
      minHeight = 76
      style =
        "-fx-border-color: #1f2937; -fx-border-radius: 6; -fx-background-radius: 6; -fx-background-color: white; -fx-padding: 6;"
      val defaultStyle: String = style.value
      scaleX = 1.0
      scaleY = 1.0
      children = Seq(
        new Label(if card.rank == null then "?" else card.rank.symbol) {
          style = s"-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: ${if red then "#dc2626" else "#111827"};"
        },
        new Label(if card.suit == null then "?" else card.suit.symbol) {
          style = s"-fx-font-size: 22px; -fx-text-fill: ${if red then "#dc2626" else "#111827"};"
        }
      )

      if isSelectable then
        onMouseEntered = _ =>
          if !selectedCards.contains(card) then
            style = "-fx-border-color: #ffff00; -fx-border-radius: 6; -fx-background-radius: 6;"
          scaleX = 1.1
          scaleY = 1.1
          cursor = Cursor.Hand

        onMouseExited = _ =>
          if !selectedCards.contains(card) then style = defaultStyle
          scaleX = 1.0
          scaleY = 1.0
          cursor = Cursor.Default

        onMouseClicked = _ =>
          if selectedCards.contains(card) then
            selectedCards.remove(card)
            style = defaultStyle
          else
            selectedCards.add(card)
            style =
              "-fx-border-color: #22c55e; -fx-border-radius: 6; -fx-background-radius: 6; -fx-background-color: #f0fdf4;"
    }
  }
}
