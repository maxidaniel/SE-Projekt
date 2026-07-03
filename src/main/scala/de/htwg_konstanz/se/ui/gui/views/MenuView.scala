package de.htwg_konstanz.se.ui.gui.views

import de.htwg_konstanz.se.ui.IPresenter
import de.htwg_konstanz.se.ui.gui.{GuiViews, View}
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.FileChooser

case class MenuView(p: IPresenter) extends BorderPane {
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
  var deleteSaveButton: Button = new Button("Delete Save") {
    onMouseClicked = _ => {
      val window = scene.value.getWindow
      if window != null then
        val chooser = new FileChooser()
        chooser.setTitle("Delete Save File")
        chooser.getExtensionFilters.add(new FileChooser.ExtensionFilter("JSON Files", "*.json"))
        chooser.getExtensionFilters.add(new FileChooser.ExtensionFilter("XML Files", "*.xml"))
        val file = chooser.showOpenDialog(window)
        if file != null then p.deleteSave(file.getAbsolutePath)
    }
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
        children = Seq(openLobbyButton, loadButton, deleteSaveButton, showGameButton, quitButton)
      },
      rulesPanel
    )
  }
}
