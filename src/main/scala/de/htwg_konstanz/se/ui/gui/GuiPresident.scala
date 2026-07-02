package de.htwg_konstanz.se.ui.gui

import com.google.inject.Inject
import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.util.Listener
import javafx.application.Platform
import scalafx.animation.FadeTransition
import scalafx.application.JFXApp3
import scalafx.geometry.Pos
import scalafx.scene.control.Label
import scalafx.scene.layout.StackPane
import scalafx.scene.{Parent, Scene}
import scalafx.util.Duration

import scala.compiletime.uninitialized

case class GuiPresident @Inject() (controller: IController) extends Listener, JFXApp3 {
  controller.add(this)

  private val presenter = new GuiPresenter(
    controller,
    onRefresh = () => refreshView()
  )

  private var listenerRegistered = false
  private var showingSplash = true
  private var splashLabel: Label = uninitialized

  override def onEvent(event: GameEvent): Unit = event match {
    case GameExitEvent => Platform.exit()
    case _             =>
      runOnFxThread {
        presenter.handleEvent(event)
      }
  }

  private def refreshView(): Unit = {
    if showingSplash then return
    val view = presenter.currentView match {
      case View.Menu   => GuiViews.menuView(presenter)
      case View.Lobby  => GuiViews.lobbyView(presenter)
      case View.Game   => GuiViews.gameView(presenter)
      case View.Result => GuiViews.resultView(presenter)
    }
    stage.scene.value.setRoot(view.delegate)
  }

  private def runOnFxThread(action: => Unit): Unit = {
    if Platform.isFxApplicationThread then action
    else Platform.runLater(() => action)
  }

  private def registerListener(): Unit = {
    if !listenerRegistered then {
      controller.add(this)
      listenerRegistered = true
    }
  }

  override def start(): Unit = {
    registerListener()
    stage = new JFXApp3.PrimaryStage {
      title = "President"
      scene = new Scene(800, 600) {
        root = splashView()
      }
    }

    runOnFxThread {
      splashLabel.opacity = 0.0
      val fadeIn = new FadeTransition(Duration(0.4)) {
        node = splashLabel
        fromValue = 0.0
        toValue = 1.0
        onFinished = _ => {
          val fadeOut = new FadeTransition(Duration(0.6)) {
            node = splashLabel
            fromValue = 1.0
            toValue = 0.0
            onFinished = _ => {
              showingSplash = false
              presenter.viewModel.currentView = View.Menu
              refreshView()
            }
          }
          fadeOut.play()
        }
      }
      fadeIn.play()
    }
  }

  private def splashView(): Parent = {
    splashLabel = new Label(GuiViews.logoText) {
      style = "-fx-font-family: 'Courier New', monospace;" +
        "-fx-font-size: 11px;" +
        "-fx-text-fill: #1f2937;" +
        "-fx-background-color: white;" +
        "-fx-padding: 40;" +
        "-fx-background-radius: 12;"
    }
    new StackPane {
      alignment = Pos.Center
      children = Seq(splashLabel)
    }
  }
}
