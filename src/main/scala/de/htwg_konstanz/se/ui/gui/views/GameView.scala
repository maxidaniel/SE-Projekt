package de.htwg_konstanz.se.ui.gui.views

import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.models.PlayerType.Computer
import de.htwg_konstanz.se.ui.IPresenter
import de.htwg_konstanz.se.ui.gui.{GuiViews, View}
import scalafx.Includes.*
import scalafx.collections.ObservableHashSet
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.FileChooser

case class GameView(p: IPresenter) extends BorderPane {
  padding = Insets(24)

  var game: Game = p.controller.getGame
  var selectedCards: ObservableHashSet[Card] = ObservableHashSet[Card]()

  def currentPlayer: Option[IPlayer] = game.currentPlayer

  var isPlaying: Boolean = game.state == GameState.Playing
  var mustLead: Boolean = game.trickCount == 0
  var isTrickLeader: Boolean = currentPlayer.exists(p => game.trickLeader.contains(p))

  var titleLabel: Label = GuiViews.titleLabel("President Game")
  var statusLbl: Label = GuiViews.statusLabel(p)
  var stateLabel: Label = new Label(s"State: ${game.state}")
  var sep: Separator = new Separator()

  var playersSection: VBox = GuiViews.section("Players", GuiViews.playerList(p, game, allowRemove = false))
  var scoresSection: VBox = GuiViews.section(s"Scores (Round ${game.roundNumber})", GuiViews.scorePanel(p, game))
  var tableSection: VBox =
    GuiViews.section("Cards on the table", GuiViews.cardFlow(p, game.playedCards, None, selectedCards))
  var handsSection: VBox = GuiViews.section("Hands", GuiViews.handsPanel(p, game, selectedCards))

  var playCardButton: Button = new Button("Play Card") {
    disable = true
    onMouseClicked = _ =>
      currentPlayer.foreach { player =>
        selectedCards.headOption.foreach { card =>
          p.playSelectedCard(player, card)
          selectedCards.clear()
        }
      }
  }

  var passButton: Button = new Button("Pass") {
    disable = (currentPlayer.isEmpty || !isPlaying || mustLead || isTrickLeader) && !currentPlayer.exists(p =>
      p.playerType == Computer
    )
    onMouseClicked = _ => currentPlayer.foreach(player => p.passTrick(player))
  }

  var playBotButton: Button = new Button("Play Bot Turn") {
    disable = !p.isComputerTurn
    onMouseClicked = _ => p.triggerComputerPlay()
  }

  selectedCards.onChange { (set, _) =>
    playCardButton.disable = set.isEmpty || !isPlaying || p.isComputerTurn
  }

  var undoButton: Button = new Button("Undo") {
    onMouseClicked = _ => p.undo()
  }
  var redoButton: Button = new Button("Redo") {
    onMouseClicked = _ => p.redo()
  }
  var abortButton: Button = new Button("Abort Game") {
    disable = !isPlaying
    onMouseClicked = _ => p.abortGame()
  }
  var saveButton: Button = new Button("Save Game") {
    onMouseClicked = _ => {
      val window = scene.value.getWindow
      if window != null then
        val chooser = new FileChooser()
        chooser.setTitle("Save Game")
        chooser.getExtensionFilters.add(new FileChooser.ExtensionFilter("JSON Files", "*.json"))
        chooser.getExtensionFilters.add(new FileChooser.ExtensionFilter("XML Files", "*.xml"))
        val file = chooser.showSaveDialog(window)
        if file != null then p.save(file.getAbsolutePath)
    }
  }
  var loadButton: Button = new Button("Load Game") {
    onMouseClicked = _ => {
      val window = scene.value.getWindow
      if window != null then
        val chooser = new FileChooser()
        chooser.setTitle("Load Game")
        chooser.getExtensionFilters.add(new FileChooser.ExtensionFilter("JSON Files", "*.json"))
        chooser.getExtensionFilters.add(new FileChooser.ExtensionFilter("XML Files", "*.xml"))
        val file = chooser.showOpenDialog(window)
        if file != null then
          p.load(file.getAbsolutePath)
          p.navigateTo(p.viewForGame(p.controller.getGame))
    }
  }
  var backToLobbyButton: Button = new Button("Back to Lobby") {
    onMouseClicked = _ => p.navigateTo(View.Lobby)
  }
  var mainMenuButton: Button = new Button("Main Menu") {
    onMouseClicked = _ => p.navigateTo(View.Menu)
  }

  top = new VBox {
    spacing = 8
    children = Seq(titleLabel, statusLbl, stateLabel, sep)
  }

  center = new VBox {
    spacing = 14
    padding = Insets(16, 0, 16, 0)
    children = Seq(
      new TabPane {
        tabClosingPolicy = TabPane.TabClosingPolicy.Unavailable
        VBox.setVgrow(this, Priority.Always)
        tabs = Seq(
          new Tab {
            text = "Players"
            content = playersSection
          },
          new Tab {
            text = s"Scores (Round ${game.roundNumber})"
            content = scoresSection
          },
          new Tab {
            text = "Hands"
            content = handsSection
          }
        )
      },
      tableSection
    )
  }

  right = new VBox {
    spacing = 8
    alignment = Pos.TopCenter
    padding = Insets(16, 0, 16, 16)
    minWidth = 140
    children = Seq(
      playCardButton,
      passButton,
      playBotButton,
      new Separator(),
      undoButton,
      redoButton,
      new Separator(),
      saveButton,
      loadButton,
      new Separator(),
      abortButton,
      new Separator(),
      backToLobbyButton,
      mainMenuButton
    )
  }
}
