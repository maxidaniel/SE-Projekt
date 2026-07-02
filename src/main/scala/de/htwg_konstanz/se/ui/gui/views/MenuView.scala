package de.htwg_konstanz.se.ui.gui.views

import de.htwg_konstanz.se.ui.gui.{GuiViews, IGuiPresenter, View}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*

case class MenuView(p: IGuiPresenter) extends BorderPane {
  padding = Insets(32)

  var titleLabel: Label = GuiViews.titleLabel("President")
  var statusLbl: Label = GuiViews.statusLabel(p)
  var sep: Separator = new Separator()

  var openLobbyButton: Button = new Button("Open Lobby") {
    onMouseClicked = _ => p.navigateTo(View.Lobby)
  }
  var showGameButton: Button = new Button("Show Current Game") {
    onMouseClicked = _ => p.navigateTo(p.viewForGame(p.controller.getGame))
  }
  var quitButton: Button = new Button("Quit") {
    onMouseClicked = _ => p.controller.exit()
  }
  var rulesPanel: VBox = GuiViews.rulesPanel()

  top = new VBox {
    spacing = 8
    children = Seq(titleLabel, statusLbl, sep)
  }

  center = new VBox {
    spacing = 18
    alignment = Pos.Center
    padding = Insets(16)
    children = Seq(
      new HBox {
        spacing = 12
        alignment = Pos.Center
        children = Seq(openLobbyButton, showGameButton, quitButton)
      },
      rulesPanel
    )
  }
}
