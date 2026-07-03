package de.htwg_konstanz.se.ui.gui.views

import de.htwg_konstanz.se.models.GameState
import de.htwg_konstanz.se.ui.IPresenter
import de.htwg_konstanz.se.ui.gui.GuiViews
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.FileChooser

case class ResultView(p: IPresenter) extends BorderPane {
  padding = Insets(24)

  var titleLabel: Label = GuiViews.titleLabel(p.resultTitle)
  var statusLbl: Label = GuiViews.statusLabel(p)
  var sep: Separator = new Separator()
  var playerListBox: VBox = GuiViews.playerList(p, p.controller.getGame, allowRemove = false)

  val game = p.controller.getGame
  val isEnded = game.state == GameState.Ended

  var nextRoundButton: Button = new Button("Next Round") {
    visible = isEnded
    managed = isEnded
    onMouseClicked = _ => p.nextRound()
  }

  var backToLobbyButton: Button = new Button("Back to Lobby") {
    onMouseClicked = _ => p.resetToLobby()
  }
  var mainMenuButton: Button = new Button("Main Menu") {
    onMouseClicked = _ => p.resetToMenu()
  }

  var saveButton: Button = new Button("Save") {
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

  top = new VBox {
    spacing = 8
    children = Seq(titleLabel, statusLbl, sep)
  }

  center = new ScrollPane {
    fitToWidth = true
    content = new VBox {
      spacing = 14
      padding = Insets(16, 0, 16, 0)
      children = Seq(playerListBox)
    }
  }

  bottom = new HBox {
    spacing = 10
    alignment = Pos.CenterRight
    padding = Insets(12, 0, 0, 0)
    children = Seq(nextRoundButton, saveButton, backToLobbyButton, mainMenuButton)
  }
}
