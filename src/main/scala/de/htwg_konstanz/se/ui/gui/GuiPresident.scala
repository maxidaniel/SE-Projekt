package de.htwg_konstanz.se.ui.gui

import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.models.*
import de.htwg_konstanz.se.util.Listener
import javafx.application.Platform
import javafx.event.{ActionEvent, EventHandler}
import scalafx.application.JFXApp3
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.{Parent, Scene}
import scalafx.scene.control.{Button, Label, ScrollPane, Separator, TextField}
import scalafx.scene.layout.{BorderPane, FlowPane, HBox, StackPane, VBox}

import java.util.UUID

case class GuiPresident(controller: GameController) extends Listener, JFXApp3 {
  private val viewModel = new PresidentViewModel(controller)
  private var listenerRegistered: Boolean = false

  override def onEvent(event: GameEvent): Unit = {
    event match {
      case GameExitEvent => Platform.exit()
      case _ => runOnFxThread {
        viewModel.handleEvent(event)
        refreshView()
      }
    }
  }

  private def menuView(): Parent = new VBox {
    spacing = 18
    padding = Insets(32)
    alignment = Pos.Center

    children = Seq(
      titleLabel("President"),
      new Label("A ScalaFX interface for the card game President.") {
        wrapText = true
        style = "-fx-font-size: 16px;"
      },
      statusLabel(),
      new HBox {
        spacing = 12
        alignment = Pos.Center
        children = Seq(
          new Button("Open Lobby") {
            onAction = eventHandler(navigateTo(View.Lobby))
          },
          new Button("Show Current Game") {
            onAction = eventHandler(navigateTo(viewModel.viewForGame(controller.getGame)))
          },
          new Button("Quit") {
            onAction = eventHandler(controller.exit())
          }
        )
      },
      rulesPanel()
    )
  }

  private def lobbyView(): Parent = {
    val game = controller.getGame
    val nameInput = new TextField {
      promptText = "Player name"
      prefColumnCount = 24
    }

    def addPlayerFromInput(): Unit = {
      val name = nameInput.text.value.trim
      if name.isEmpty then viewModel.statusMessage = "Enter a player name before joining."
      else {
        val player = Player(UUID.randomUUID(), name)
        viewModel.rememberPlayer(player)
        controller.join(player)
        nameInput.text = ""
      }

      refreshView()
    }

    nameInput.onAction = eventHandler(addPlayerFromInput())

    page(
      header = "Lobby",
      bodyContent = Seq(
        statusLabel(),
        new HBox {
          spacing = 8
          alignment = Pos.CenterLeft
          children = Seq(
            nameInput,
            new Button("Add Player") {
              onAction = eventHandler(addPlayerFromInput())
            },
            new Button("Start Game") {
              disable = game.playerHands.size < 2
              onAction = eventHandler {
                if controller.getGame.playerHands.size < 2 then {
                  viewModel.statusMessage = "At least two players are required to start."
                  refreshView()
                } else controller.start()
              }
            }
          )
        },
        new Label("Add at least two players, then start the game."),
        new Separator(),
        playerList(game, allowRemove = true)
      ),
      actions = Seq(
        new Button("Undo") {
          onAction = eventHandler(controller.undo())
        },
        new Button("Redo") {
          onAction = eventHandler(controller.redo())
        },
        new Button("Back to Menu") {
          onAction = eventHandler(navigateTo(View.Menu))
        }
      )
    )
  }

  private def gameView(): Parent = {
    val game = controller.getGame

    new StackPane {
      children = Seq(
        page(
          header = "President Game",
          bodyContent = Seq(
            statusLabel(),
            new Label(s"State: ${game.state}"),
            new Separator(),
            section("Players", playerList(game, allowRemove = false)),
            section("Cards on the table", cardFlow(game.playedCards)),
            section("Hands", handsPanel(game)),
            new Label("Card selection and trick resolution are displayed here once the controller exposes play-card actions.") {
              wrapText = true
            }
          ),
          actions = Seq(
            new Button("Undo") {
              onAction = eventHandler(controller.undo())
            },
            new Button("Redo") {
              onAction = eventHandler(controller.redo())
            },
            new Button("Abort Game") {
              disable = game.state != GameState.Playing
              onAction = eventHandler(controller.abort())
            },
            new Button("Back to Lobby") {
              onAction = eventHandler(navigateTo(View.Lobby))
            },
            new Button("Main Menu") {
              onAction = eventHandler(navigateTo(View.Menu))
            }
          )
        )
      )
    }
  }

  def resultView(): Parent = page(
    header = viewModel.resultTitle,
    bodyContent = Seq(
      statusLabel(),
      new Separator(),
      playerList(controller.getGame, allowRemove = false)
    ),
    actions = Seq(
      new Button("Back to Lobby") {
        onAction = eventHandler(navigateTo(View.Lobby))
      },
      new Button("Main Menu") {
        onAction = eventHandler(navigateTo(View.Menu))
      }
    )
  )

  private def page(header: String, bodyContent: Seq[scalafx.scene.Node], actions: Seq[scalafx.scene.Node]): Parent = new BorderPane {
    padding = Insets(24)

    top = new VBox {
      spacing = 8
      children = Seq(titleLabel(header), new Separator())
    }

    center = new ScrollPane {
      fitToWidth = true
      content = new VBox {
        spacing = 14
        padding = Insets(16, 0, 16, 0)
        children = bodyContent
      }
    }

    bottom = new HBox {
      spacing = 10
      alignment = Pos.CenterRight
      padding = Insets(12, 0, 0, 0)
      children = actions
    }
  }

  private def titleLabel(text: String): Label = new Label(text) {
    style = "-fx-font-size: 28px; -fx-font-weight: bold;"
  }

  private def statusLabel(): Label = new Label(viewModel.statusMessage) {
    wrapText = true
    style = "-fx-padding: 10; -fx-background-color: #f3f5f8; -fx-background-radius: 6;"
  }

  private def rulesPanel(): VBox = new VBox {
    spacing = 8
    alignment = Pos.CenterLeft
    maxWidth = 560
    children = Seq(
      new Label("Controls") {
        style = "-fx-font-size: 18px; -fx-font-weight: bold;"
      },
      new Label("• Add players in the lobby.\n• Start with at least two players.\n• Follow the current table and player hands during the game.\n• Abort returns to the result screen.") {
        wrapText = true
      }
    )
  }

  private def section(title: String, node: scalafx.scene.Node): VBox = new VBox {
    spacing = 8
    children = Seq(
      new Label(title) {
        style = "-fx-font-size: 18px; -fx-font-weight: bold;"
      },
      node
    )
  }

  private def playerList(game: Game, allowRemove: Boolean): VBox = new VBox {
    spacing = 8
    children =
      if game.playerHands.isEmpty then Seq(new Label("No players have joined yet."))
      else game.playerHands.toVector.sortBy(_._1.toString).map { case (playerId, cards) =>
        new HBox {
          spacing = 10
          alignment = Pos.CenterLeft
          children = Seq(
            new Label(viewModel.displayName(playerId)) {
              minWidth = 160
              style = "-fx-font-weight: bold;"
            },
            new Label(s"${cards.size} cards"),
            new Label(playerId.toString.take(8)) {
              style = "-fx-text-fill: #6b7280;"
            }
          ) ++
            (if allowRemove then Seq(new Button("Remove") {
              onAction = eventHandler(controller.quit(Player(playerId, viewModel.displayName(playerId))))
            }) else Seq.empty)
        }
      }
  }

  private def handsPanel(game: Game): VBox = new VBox {
    spacing = 12
    children =
      if game.playerHands.isEmpty then Seq(new Label("No hands available."))
      else game.playerHands.toVector.sortBy(_._1.toString).map { case (playerId, cards) =>
        new VBox {
          spacing = 6
          children = Seq(
            new Label(viewModel.displayName(playerId)) {
              style = "-fx-font-weight: bold;"
            },
            cardFlow(cards)
          )
        }
      }
  }

  private def cardFlow(cards: Seq[Card]): Parent = new FlowPane {
    hgap = 8
    vgap = 8
    children =
      if cards.isEmpty then Seq(new Label("No cards."))
      else cards.map(cardView)
  }

  private def cardView(card: Card): VBox = {
    val red = card.suit == CardSuit.Hearts || card.suit == CardSuit.Diamonds

    new VBox {
      alignment = Pos.Center
      spacing = 2
      minWidth = 56
      minHeight = 76
      style = "-fx-border-color: #1f2937; -fx-border-radius: 6; -fx-background-radius: 6; -fx-background-color: white; -fx-padding: 6;"
      children = Seq(
        new Label(if card.rank == null then "?" else card.rank.symbol) {
          style = s"-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: ${if red then "#dc2626" else "#111827"};"
        },
        new Label(if card.suit == null then "?" else card.suit.symbol) {
          style = s"-fx-font-size: 22px; -fx-text-fill: ${if red then "#dc2626" else "#111827"};"
        }
      )
    }
  }

  private def navigateTo(view: View): Unit = {
    viewModel.currentView = view
    refreshView()
  }

  private def refreshView(): Unit = {
    val view = viewModel.currentView match {
      case View.Menu => menuView()
      case View.Lobby => lobbyView()
      case View.Game => gameView()
      case View.Result => resultView()
    }

    switchToView(view)
  }

  private def switchToView(view: Parent): Unit = {
    stage.scene.value.setRoot(view.delegate)
  }


  private def eventHandler(handler: => Unit): EventHandler[ActionEvent] = (_: ActionEvent) => handler

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

  // JavaFX thread
  override def start(): Unit = {
    registerListener()

    stage = new JFXApp3.PrimaryStage {
      title = "President"
      scene = new Scene(800, 600) {
        root = menuView()
      }
    }
  }
}
