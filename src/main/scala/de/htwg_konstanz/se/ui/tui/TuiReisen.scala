package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.models.GameState.*
import de.htwg_konstanz.se.models.{Card, Game, GameEvent, GameExitEvent}
import de.htwg_konstanz.se.util.Listener
import org.jline.terminal.Terminal.*
import org.jline.keymap.{BindingReader, KeyMap}
import org.jline.terminal.{Terminal, TerminalBuilder}
import com.google.inject.Inject

case class TuiReisen @Inject() (controller: IController) extends Listener {
  controller.add(this)

  private val Esc = "\u001b"
  private val Up = "\u001b[A"
  private val Down = "\u001b[B"
  private val Right = "\u001b[C"
  private val Left = "\u001b[D"

  // Source: https://patorjk.com/software/taag/#p=display&f=Cards&t=President&x=none&v=4&h=4&w=80&we=false
  private val logo: Vector[String] =
    """|.------..------..------..------..------..------..------..------..------.
       ||P.--. ||R.--. ||E.--. ||S.--. ||I.--. ||D.--. ||E.--. ||N.--. ||T.--. |
       || :/\: || :(): || (\/) || :/\: || (\/) || :/\: || (\/) || :(): || :/\: |
       || (__) || ()() || :\/: || :\/: || :\/: || (__) || :\/: || ()() || (__) |
       || '--'P|| '--'R|| '--'E|| '--'S|| '--'I|| '--'D|| '--'E|| '--'N|| '--'T|
       |`------'`------'`------'`------'`------'`------'`------'`------'`------'"""
      .stripMargin
      .linesIterator
      .toVector

  private var shouldClose: Boolean = false
  private var renderScale: Int = 1

  private val terminal: Terminal = TerminalBuilder.builder().system(true).build()
  private val renderer: TerminalRenderer = TerminalRenderer(terminal)

  terminal.handle(Terminal.Signal.INT, _ => {
    terminal.writer().println("Exiting")
    terminal.flush()

    controller.exit()
  })

  terminal.handle(Terminal.Signal.WINCH, _ => {
    renderer.windowSizeChanged()
  })

  renderer.initialize()

  def run(): Unit = {
    val bindingReader = new BindingReader(terminal.reader())
    val keyMap = new KeyMap[String]()
    keyMap.bind("quit", "q")
    keyMap.bind("start", "\n")
    keyMap.bind("start", "\r")
    keyMap.bind("add", "+")
    keyMap.bind("scale-down", "-")
    keyMap.bind("undo", "z")
    keyMap.bind("undo", "Z")
    keyMap.bind("redo", "y")
    keyMap.bind("redo", "Y")
    keyMap.bind("abort", Esc)
    keyMap.bind("up", Up)
    keyMap.bind("down", Down)
    keyMap.bind("right", Right)
    keyMap.bind("left", Left)
    for (i <- 0 to 9) {
      keyMap.bind(s"card-$i", i.toString)
    }
    keyMap.bind("continue-turn", "\t")
    keyMap.bind("pass", "p")

    renderer.transitionTo(TuiView.Splash, buildCanvas(Vector(centeredObject(logo))))
    Thread.sleep(2500)

    refresh()

    while (!shouldClose) {
      val operation = bindingReader.readBinding(keyMap)
      operation match {
        case "quit" => controller.exit()

        case "start" =>
          if controller.getGameState == Aborted then controller.reset()
          controller.start()

        case "add" => handlePlus(controller.getGame)

        case "scale-down" => handleMinus(controller.getGame)

        case "undo" => controller.undo()

        case "redo" => controller.redo()

        case "abort" =>
          if controller.getGameState == Playing then controller.abort()

        case "continue-turn" =>
          handleContinueTurn(controller.getGame)

        case "pass" =>
          handlePass(controller.getGame)

        case _ if operation.startsWith("card-") =>
          val index = operation.drop(5).toInt
          handleCardPlay(controller.getGame, index)

        case _ =>
      }
    }

    renderer.clear()
    renderer.close()
  }

  private def refresh(): Unit = {
    val (view, renderObjs) = renderObjsForState(controller.getGame)
    renderer.transitionTo(view, buildCanvas(renderObjs))
  }

  override def onEvent(event: GameEvent): Unit = {
    event match {
      case GameExitEvent => shouldClose = true
      case _ => refresh()
    }
  }

  private[tui] def handlePlus(game: Game): Unit = {
    game.state match {
      case Playing =>
        renderScale = math.min(3, renderScale + 1)
      case WaitingForPlayers =>
        controller.join("")
      case _ =>
    }
  }

  private[tui] def handleMinus(game: Game): Unit = {
    game.state match {
      case Playing =>
        renderScale = math.max(1, renderScale - 1)
      case _ =>
    }
  }

  private[tui] def handleCardPlay(game: Game, index: Int): Unit = {
    game.state match {
      case Playing =>
        game.currentPlayer match {
          case Some(currentId) =>
            controller.getPlayer(currentId) match {
              case Some(player) =>
                controller.playCardByIndex(player, index)
              case None =>
            }
          case None =>
        }
      case _ =>
    }
  }

  private[tui] def handleContinueTurn(game: Game): Unit = {
    game.state match {
      case Playing =>
        game.currentPlayer match {
          case Some(currentId) =>
            controller.getPlayer(currentId) match {
              case Some(player) =>
                controller.playCardByComputer(player)
              case None =>
            }
          case None =>
        }
      case _ =>
    }
  }

  private[tui] def handlePass(game: Game): Unit = {
    game.state match {
      case Playing =>
        game.currentPlayer match {
          case Some(currentId) =>
            controller.getPlayer(currentId) match {
              case Some(player) =>
                controller.passTrick(player)
              case None =>
            }
          case None =>
        }
      case _ =>
    }
  }

  private[tui] def renderObjsForState(game: Game): (TuiView, Vector[RenderObj]) = {
    val playersPanel = playerPanelRenderObjs(game)

    game.state match {
      case WaitingForPlayers =>
        val playerIds = game.playerHands.keys.toVector.sorted

        (
          TuiView.MainMenu,
          Vector(
            RenderObj(2, 1, Vector("Waiting For Players")),
            RenderObj(2, 3, Vector("+: Add player  Enter: Start game  q: Quit")),
            RenderObj(2, 5, Vector(s"Players connected: ${playerIds.size}"))
          ) ++ playersPanel
        )

      case Aborted =>
        val playerIds = game.playerHands.keys.toVector.sorted

        (
          TuiView.MainMenu,
          Vector(
            RenderObj(2, 1, Vector("Game Aborted")),
            RenderObj(2, 3, Vector("Enter: Reset & Start  q: Quit")),
            RenderObj(2, 5, Vector(s"Players connected: ${playerIds.size}"))
          ) ++ playersPanel
        )

      case Playing =>
        val active = activeCards(game)
        val currentPlayerId = game.currentPlayer
        val currentPlayerName = currentPlayerId.flatMap(id => game.playerNames.get(id))

        val cardRender = CardRenderer.render(
          cards = active,
          terminalWidth = terminal.getColumns - 4,
          terminalHeight = terminal.getRows - 10,
          options = CardRendererOptions(userScale = renderScale)
        )
        val cardsWidth = cardRender.lines.map(_.length).maxOption.getOrElse(0)
        val cardsX = math.max(0, (terminal.getColumns - cardsWidth) / 2)
        val cardsY = math.max(6, terminal.getRows - cardRender.lines.length - 1)

        val turnInfo = currentPlayerName match {
          case Some(name) => Vector(s"Current turn: $name")
          case None => Vector("Current turn: ?")
        }

        val controls = Vector(
          "0-9: Play card  p: Pass  Tab: Auto-play  Esc: Abort  q: Quit"
        )

        (
          TuiView.Playing,
          Vector(
            RenderObj(2, 1, Vector("Game Running")),
            RenderObj(2, 3, Vector("Esc: Abort game  q: Quit")),
            RenderObj(2, 4, Vector(s"Scale: $renderScale ( +: up  -: down )")),
            RenderObj(2, 6, turnInfo),
            RenderObj(2, 7, controls),
            RenderObj(cardsX, cardsY, cardRender.lines)
          ) ++ playersPanel
        )
      case e => (
        TuiView.MainMenu,
        Vector(
          RenderObj(2, 1, Vector(s"Unhandled game state: $e"))
        ) ++ playersPanel
      )
    }
  }

  private[tui] def centeredObject(lines: Vector[String]): RenderObj = {
    val startY = math.max(0, terminal.getRows / 2 - lines.length / 2)
    RenderObj.Centered(0, startY, lines, width = Some(terminal.getColumns))
  }

  private[tui] def playerPanelRenderObjs(game: Game): Vector[RenderObj] = {
    val panelWidth = math.max(30, math.min(56, terminal.getColumns / 2))
    val panelX = math.max(0, terminal.getColumns - panelWidth - 2)
    val cardsWidth = math.max(10, panelWidth - 18)
    val namesWidth = panelWidth - cardsWidth - 1

    val header = Vector(
      RenderObj.Right(panelX, 1, Vector("Players"), width = Some(panelWidth)),
      RenderObj.Right(panelX, 2, Vector("-" * panelWidth), width = Some(panelWidth))
    )

    val rows =
      if game.playerHands.isEmpty then
        Vector(
          RenderObj.Left(panelX, 3, Vector("-"), width = Some(cardsWidth)),
          RenderObj.Right(panelX + cardsWidth + 1, 3, Vector("None"), width = Some(namesWidth))
        )
      else
        game.playerHands.toVector.sortBy(_._1.toString).zipWithIndex.flatMap { case ((id, cards), index) =>
          val y = 3 + index
          val cardsText =
            if cards.isEmpty then "-"
            else cards.zipWithIndex.map { case (card, cardIndex) =>
              val marker = if (game.currentPlayer.contains(id)) "*" else " "
              s"[$cardIndex:$marker]${card.toString}"
            }.mkString(" ")
          val playerName = s"${game.playerNames.get(id).getOrElse("Player")} ${id.toString.take(8)}"

          Vector(
            RenderObj.Left(panelX, y, Vector(cardsText), width = Some(cardsWidth)),
            RenderObj.Right(panelX + cardsWidth + 1, y, Vector(playerName), width = Some(namesWidth))
          )
        }

    header ++ rows
  }

  private[tui] def activeCards(game: Game): Vector[Card] = game.playedCards

  private[tui] def buildCanvas(renderObjs: Seq[RenderObj]): Vector[String] = {
    ConsoleCanvas.renderFrame(terminal.getColumns, terminal.getRows, renderObjs)
  }

  private[tui] def currentRenderScale: Int = renderScale

  private[tui] def setRenderScale(scale: Int): Unit = renderScale = scale

  private[tui] def closeForTest(): Unit = renderer.close()

}
