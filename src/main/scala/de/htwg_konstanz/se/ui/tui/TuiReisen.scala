package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.controller.GameController
import de.htwg_konstanz.se.models.{Card, Game, GameEvent, Player}
import de.htwg_konstanz.se.models.Card.TenOfSpades
import de.htwg_konstanz.se.models.GameState.{Playing, WaitingForPlayers}
import de.htwg_konstanz.se.util.Listener
import org.jline.terminal.Terminal.*
import org.jline.terminal.{Terminal, TerminalBuilder}

import java.util.UUID

// This class is going to be updated by a service in the future. Refactor it in such a way, that we call Service.register(Tui),
// which establishes event handling in the tui, and then call Service.run(), which then handles all game state.
case class TuiReisen(controller: GameController) extends Listener {
  private val fallbackActiveCards: Vector[Card] = Vector(TenOfSpades, TenOfSpades, TenOfSpades, TenOfSpades)

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

    shouldClose = true
  })

  terminal.handle(Terminal.Signal.WINCH, _ => {
    renderer.windowSizeChanged()
  })

  renderer.initialize()

  def run(): Unit = {
    val reader = terminal.reader()
    renderer.transitionTo(TuiView.Splash, buildCanvas(Vector(centeredObject(logo))))
    Thread.sleep(2500)

    while (!shouldClose) {
      val game = controller.getGame
      val (view, renderObjs) = renderObjsForState(game)
      renderer.transitionTo(view, buildCanvas(renderObjs))

      reader.read() match {
        // exit if exit is requested
        case 'q' => shouldClose = true

        // Enter key
        case 13 => controller.setGame(handleEnter(game))

        // +
        case 43 =>
          handlePlus(game)
          controller.join(Player(UUID.randomUUID(), ""))

        // -
        case 45 => handleMinus(game)

        // Escape key
        case 27 =>
          if controller.getGameState == Playing then controller.setGame(handleEscape(game))
          else reader.read() match {
            // aux key code '['
            case 91 =>
              reader.read() match {
                // Up key
                case 65 =>

                // Down key
                case 66 =>

                // Right key
                case 67 =>

                // Left key
                case 68 => print("left")

                case _ =>
              }
            case _ =>
          }
        case _ =>
      }

      Thread.sleep(100)
    }

    renderer.clear()
    renderer.close()
  }

  override def onEvent(event: GameEvent): Unit = {
    println(s"received event ${event.toString}")
  }

  // This is the start round/continue to next person handler
  private[tui] def handleEnter(game: Game): Game = {
    game.state match {
      case WaitingForPlayers => game.start()
      case _ => game
    }
  }

  // Add player
  private[tui] def handlePlus(game: Game): Game = {
    game.state match {
      case Playing =>
        renderScale = math.min(3, renderScale + 1)
        game
      case _ => game
    }
  }

  private[tui] def handleMinus(game: Game): Unit = {
    game.state match {
      case Playing =>
        renderScale = math.max(1, renderScale - 1)
      case _ =>
    }
  }

  private[tui] def handleEscape(game: Game): Game = {
    game.state match {
      case Playing =>
        renderScale = 1
        game.copy(playedCards = Vector.empty, state = WaitingForPlayers)
      case _ =>
        game
    }
  }

  // Select
  private[tui] def handleSpace(game: Game): Game = {
    game.state match {
      case _ => game
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

      case Playing =>
        val active = activeCards(game)
        val maxScale = 3
        renderScale = math.min(renderScale, maxScale)

        val cardRender = CardRenderer.render(
          cards = active,
          terminalWidth = terminal.getWidth - 4,
          terminalHeight = terminal.getHeight - 10,
          options = CardRendererOptions(userScale = renderScale)
        )
        val cardsWidth = cardRender.lines.map(_.length).maxOption.getOrElse(0)
        val cardsX = math.max(0, (terminal.getWidth - cardsWidth) / 2)
        val cardsY = math.max(6, terminal.getHeight - cardRender.lines.length - 1)

        (
          TuiView.Playing,
          Vector(
            RenderObj(2, 1, Vector("Game Running")),
            RenderObj(2, 3, Vector("Esc: Abort game  q: Quit")),
            RenderObj(2, 4, Vector(s"Scale: $renderScale ( +: up  -: down )")),
            RenderObj(cardsX, cardsY, cardRender.lines)
          ) ++ playersPanel
        )
    }
  }

  private[tui] def centeredObject(lines: Vector[String]): RenderObj = {
    val startY = math.max(0, terminal.getHeight / 2 - lines.length / 2)
    RenderObj.Centered(0, startY, lines, width = Some(terminal.getWidth))
  }

  private[tui] def playerPanelRenderObjs(game: Game): Vector[RenderObj] = {
    val panelWidth = math.max(30, math.min(56, terminal.getWidth / 2))
    val panelX = math.max(0, terminal.getWidth - panelWidth - 2)
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
            else cards.map(card => s"[${card.toString}]").mkString(" ")
          val playerName = s"Player ${id.toString.take(8)}"

          Vector(
            RenderObj.Left(panelX, y, Vector(cardsText), width = Some(cardsWidth)),
            RenderObj.Right(panelX + cardsWidth + 1, y, Vector(playerName), width = Some(namesWidth))
          )
        }

    header ++ rows
  }

  private[tui] def activeCards(game: Game): Vector[Card] =
    if game.playedCards.nonEmpty then game.playedCards else fallbackActiveCards

  private[tui] def buildCanvas(renderObjs: Seq[RenderObj]): Vector[String] = {
    ConsoleCanvas.renderFrame(terminal.getWidth, terminal.getHeight, renderObjs)
  }

  private[tui] def currentRenderScale: Int = renderScale

  private[tui] def setRenderScale(scale: Int): Unit = renderScale = scale

  private[tui] def closeForTest(): Unit = renderer.close()

}
