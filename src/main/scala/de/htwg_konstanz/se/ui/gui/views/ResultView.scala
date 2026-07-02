package de.htwg_konstanz.se.ui.gui.views

import de.htwg_konstanz.se.ui.gui.{GuiViews, IGuiPresenter}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*

case class ResultView(p: IGuiPresenter) extends BorderPane {
  padding = Insets(24)

  var titleLabel: Label = GuiViews.titleLabel(p.resultTitle)
  var statusLbl: Label = GuiViews.statusLabel(p)
  var sep: Separator = new Separator()
  var playerListBox: VBox = GuiViews.playerList(p, p.controller.getGame, allowRemove = false)

  var backToLobbyButton: Button = new Button("Back to Lobby") {
    onMouseClicked = _ => p.resetToLobby()
  }
  var mainMenuButton: Button = new Button("Main Menu") {
    onMouseClicked = _ => p.resetToMenu()
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
    children = Seq(backToLobbyButton, mainMenuButton)
  }
}
