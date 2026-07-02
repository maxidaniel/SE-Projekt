package de.htwg_konstanz.se.ui.gui.views

import de.htwg_konstanz.se.controller.strategies.{
  IStrategy,
  PlayBestCardStrategy,
  PlayLowestPossibleCardStrategy,
  PlayRandomCardStrategy
}
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.ui.gui.{GuiViews, IGuiPresenter, View}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*

case class LobbyView(p: IGuiPresenter) extends BorderPane {
  padding = Insets(24)

  var game: Game = p.controller.getGame

  var titleLabel: Label = GuiViews.titleLabel("Lobby")
  var statusLbl: Label = GuiViews.statusLabel(p)
  var sep: Separator = new Separator()

  var nameInput: TextField = new TextField {
    promptText = "Player name"
    prefColumnCount = 24
  }

  var addButton: Button = new Button("Add Player") {
    onMouseClicked = _ => {
      p.addPlayer(nameInput.text.value.trim)
      nameInput.text = ""
    }
  }
  var startButton: Button = new Button("Start Game") {
    disable = game.playerHands.size < 2
    onMouseClicked = _ => p.startGame()
  }
  var instructionLabel: Label = new Label("Add at least two players, then start the game.")
  var separator2: Separator = new Separator()
  var playerListBox: VBox = GuiViews.playerList(p, game, allowRemove = true)

  var undoButton: Button = new Button("Undo") { onMouseClicked = _ => p.undo() }
  var redoButton: Button = new Button("Redo") { onMouseClicked = _ => p.redo() }
  var backToMenuButton: Button = new Button("Back to Menu") { onMouseClicked = _ => p.navigateTo(View.Menu) }

  private def createStrategyComboBox(): ComboBox[String] = new ComboBox[String] {
    items = ObservableBuffer("Lowest possible card", "Random card", "Best play", "Random")
    value = "Lowest possible card"
  }

  private def getStrategy(name: String) = name match {
    case "Random card" => PlayRandomCardStrategy()
    case "Best play"   => PlayBestCardStrategy()
    case _             => PlayLowestPossibleCardStrategy()
  }

  private def getRandomStrategy(): IStrategy = {
    val strategies = Vector(PlayLowestPossibleCardStrategy(), PlayRandomCardStrategy(), PlayBestCardStrategy())
    strategies(scala.util.Random.nextInt(strategies.length))
  }

  var addComputerButton: Button = new Button("Add Computer Player") {
    onMouseClicked = _ => {
      val nameField = new TextField {
        promptText = "Enter bot name"
      }
      val strategyBox = createStrategyComboBox()

      val dialog = new Dialog[Boolean]() {
        title = "Add Computer Player"
        headerText = "Configure computer player"
        contentText = null
      }

      dialog.dialogPane.value.getButtonTypes.addAll(ButtonType.OK, ButtonType.Cancel)

      val grid = new GridPane {
        hgap = 10
        vgap = 8
        add(new Label("Name:"), 0, 0)
        add(nameField, 1, 0)
        add(new Label("Strategy:"), 0, 1)
        add(strategyBox, 1, 1)
      }

      dialog.dialogPane.value.setContent(grid)

      dialog.resultConverter = {
        case ButtonType.OK => true
        case _             => false
      }

      val result = dialog.showAndWait()
      result match {
        case Some(true) =>
          val selectedStrategy = strategyBox.value.value
          val strategy = if selectedStrategy == "Random" then getRandomStrategy() else getStrategy(selectedStrategy)
          p.addComputerPlayer(nameField.text.value.trim, strategy)
        case _ =>
      }
    }
  }

  var bulkAddButton: Button = new Button("Bulk Add Computers") {
    onMouseClicked = _ => {
      val countSpinner = new Spinner[Int](1, 8, 2) {
        editable = true
        prefWidth = 100
      }
      val prefixField = new TextField {
        promptText = "e.g. Bot"
        text = "Bot"
      }
      val strategyBox = createStrategyComboBox()

      val dialog = new Dialog[Boolean]() {
        title = "Bulk Add Computer Players"
        headerText = "Add multiple computer players"
        contentText = null
      }

      dialog.dialogPane.value.getButtonTypes.addAll(ButtonType.OK, ButtonType.Cancel)

      val grid = new GridPane {
        hgap = 10
        vgap = 8
        add(new Label("Number:"), 0, 0)
        add(countSpinner, 1, 0)
        add(new Label("Name prefix:"), 0, 1)
        add(prefixField, 1, 1)
        add(new Label("Strategy:"), 0, 2)
        add(strategyBox, 1, 2)
      }

      dialog.dialogPane.value.setContent(grid)

      dialog.resultConverter = {
        case ButtonType.OK => true
        case _             => false
      }

      val result = dialog.showAndWait()
      result match {
        case Some(true) =>
          val count = countSpinner.value.value
          val prefix = prefixField.text.value.trim
          val selectedStrategy = strategyBox.value.value
          val offset = p.controller.playerCount
          for i <- 1 to count do
            val strategy = if selectedStrategy == "Random" then getRandomStrategy() else getStrategy(selectedStrategy)
            p.addComputerPlayer(s"$prefix ${offset + i}", strategy)
        case _ =>
      }
    }
  }

  top = new VBox {
    spacing = 8
    children = Seq(titleLabel, statusLbl, sep)
  }

  center = new ScrollPane {
    fitToWidth = true
    content = new VBox {
      spacing = 14
      padding = Insets(16, 0, 16, 0)
      children = Seq(instructionLabel, separator2, playerListBox)
    }
  }

  right = new VBox {
    spacing = 10
    padding = Insets(0, 0, 0, 16)
    prefWidth = 240
    children = Seq(
      new Label("Add Player") {
        style = "-fx-font-weight: bold;"
      },
      nameInput,
      addButton,
      new Separator(),
      new Label("Add Computer") {
        style = "-fx-font-weight: bold;"
      },
      addComputerButton,
      bulkAddButton,
      new Separator(),
      startButton,
      new Separator(),
      undoButton,
      redoButton,
      backToMenuButton
    )
  }
}
