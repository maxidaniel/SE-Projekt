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
  private enum View {
    case Menu, Lobby, Game, Result
  }

  private var currentView: View = View.Menu
  private var knownPlayers: Map[UUID, String] = Map.empty
  private var statusMessage: String = "Welcome to President. Create a lobby to begin."
  private var resultTitle: String = "Game finished"
  private var listenerRegistered: Boolean = false

  override def onEvent(event: GameEvent): Unit = {
    runOnFxThread {
      event match {
        case PlayerJoinEvent(player, game) =>
          rememberPlayer(player)
          statusMessage = s"${displayName(player.id)} joined the lobby."
          currentView = if game.state == GameState.Playing then View.Game else View.Lobby

        case PlayerQuitEvent(player, game) =>
          statusMessage = s"${displayName(player.id)} left the game."
          knownPlayers = knownPlayers - player.id
          currentView = if game.state == GameState.Playing then View.Game else View.Lobby

        case GameStartedEvent(game) =>
          statusMessage = "The game has started."
          currentView = View.Game

        case GameAbortedEvent(game) =>
          resultTitle = "Game aborted"
          statusMessage = "The current game was aborted."
          currentView = View.Result

        case GameEndedEvent(game, winner) =>
          rememberPlayer(winner)
          resultTitle = "Game finished"
          statusMessage = s"${displayName(winner.id)} wins the game."
          currentView = View.Result

        case GameStateChangedEvent(state, game) =>
          statusMessage = state match {
            case GameState.WaitingForPlayers => "Waiting for players."
            case GameState.Starting =>
              if game.state == GameState.Playing then "The game has started."
              else "At least two players are required to start."
            case GameState.Playing => "The game is running."
            case GameState.Aborting => "Aborting the current game."
            case GameState.Ending => "The game is ending."
          }
          currentView = viewForGame(game, fallbackState = Some(state))
      }

      refreshView()
    }
  }

  def menuView(): Parent = new VBox {
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
            onAction = eventHandler(navigateTo(viewForGame(controller.getGame)))
          },
          new Button("Quit") {
            onAction = eventHandler(Platform.exit())
          }
        )
      },
      rulesPanel()
    )
  }

  def lobbyView(): Parent = {
    val game = controller.getGame
    val nameInput = new TextField {
      promptText = "Player name"
      prefColumnCount = 24
    }

    def addPlayerFromInput(): Unit = {
      val name = nameInput.text.value.trim
      if name.isEmpty then statusMessage = "Enter a player name before joining."
      else {
        val player = Player(UUID.randomUUID(), name)
        rememberPlayer(player)
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
                  statusMessage = "At least two players are required to start."
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
        new Button("Back to Menu") {
          onAction = eventHandler(navigateTo(View.Menu))
        }
      )
    )
  }

  def gameView(): Parent = {
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
    header = resultTitle,
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

  private def statusLabel(): Label = new Label(statusMessage) {
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
            new Label(displayName(playerId)) {
              minWidth = 160
              style = "-fx-font-weight: bold;"
            },
            new Label(s"${cards.size} cards"),
            new Label(playerId.toString.take(8)) {
              style = "-fx-text-fill: #6b7280;"
            }
          ) ++
            (if allowRemove then Seq(new Button("Remove") {
              onAction = eventHandler(controller.quit(Player(playerId, displayName(playerId))))
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
            new Label(displayName(playerId)) {
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
    currentView = view
    refreshView()
  }

  private def refreshView(): Unit = {
    val view = currentView match {
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

  private def viewForGame(game: Game, fallbackState: Option[GameState] = None): View = {
    fallbackState match {
      case Some(GameState.Aborting | GameState.Ending) => View.Result
      case _ =>
        game.state match {
          case GameState.WaitingForPlayers => View.Lobby
          case GameState.Starting | GameState.Playing => View.Game
          case GameState.Aborting | GameState.Ending => View.Result
        }
    }
  }

  private def rememberPlayer(player: Player): Unit = {
    knownPlayers = knownPlayers + (player.id -> player.name)
  }

  private def displayName(playerId: UUID): String = {
    knownPlayers.get(playerId).filter(_.nonEmpty).getOrElse(s"Player ${playerId.toString.take(8)}")
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
