package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.controller.IController
import de.htwg_konstanz.se.controller.strategies.*
import de.htwg_konstanz.se.models.GameState.*
import de.htwg_konstanz.se.models.PlayerType.{Computer, Human, Unknown}
import de.htwg_konstanz.se.models.{Card, Game, GameEvent, GameExitEvent}
import de.htwg_konstanz.se.ui.gui.View
import de.htwg_konstanz.se.util.Listener
import org.jline.keymap.{BindingReader, KeyMap}
import org.jline.reader.*
import org.jline.reader.impl.DefaultHighlighter
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.{Terminal, TerminalBuilder}
import org.jline.utils.AttributedString
import com.google.inject.Inject

enum GameTab:
  case Players, Scores, Hands

  def next: GameTab = this match
    case Players => Scores
    case Scores  => Hands
    case Hands   => Players

  def prev: GameTab = this match
    case Players => Hands
    case Scores  => Players
    case Hands   => Scores

enum KeyAction:
  case Quit, Plus, Minus, Undo, Redo, Abort
  case TabNext, TabPrev, Pass, NextRound
  case Save, Load, Back
  case NavigateMenu, NavigateLobby, NavigateGame, NavigateResult
  case ComputerTurn, AddPlayer
  case CardSelectPrev, CardSelectNext, CardConfirm

object KeyAction:
  def fromString(s: String): Option[KeyAction] = s match
    case "quit"            => Some(Quit)
    case "plus"            => Some(Plus)
    case "minus"           => Some(Minus)
    case "undo"            => Some(Undo)
    case "redo"            => Some(Redo)
    case "abort"           => Some(Abort)
    case "tab-next"        => Some(TabNext)
    case "tab-prev"        => Some(TabPrev)
    case "pass"            => Some(Pass)
    case "next-round"      => Some(NextRound)
    case "save"            => Some(Save)
    case "load"            => Some(Load)
    case "back"            => Some(Back)
    case "navigate-menu"   => Some(NavigateMenu)
    case "navigate-lobby"  => Some(NavigateLobby)
    case "navigate-game"   => Some(NavigateGame)
    case "navigate-result" => Some(NavigateResult)
    case "computer-turn"   => Some(ComputerTurn)
    case "add-player"      => Some(AddPlayer)
    case "card-prev"       => Some(CardSelectPrev)
    case "card-next"       => Some(CardSelectNext)
    case "card-confirm"    => Some(CardConfirm)
    case _                 => None

/** Tab completer for file paths in save/load prompts */
class PathCompleter extends Completer:
  private val javaCompleter =
    org.jline.builtins.Completers.FilesCompleter(java.nio.file.Paths.get("."))
  override def complete(
      reader: LineReader,
      line: ParsedLine,
      candidates: java.util.List[Candidate]
  ): Unit =
    javaCompleter.complete(reader, line, candidates)

/** Syntax highlighter for LineReader prompts */
class PresidentHighlighter extends DefaultHighlighter:
  private val keywordStyle =
    org.jline.utils.AttributedStyle.DEFAULT.bold.foreground(org.jline.utils.AttributedStyle.CYAN)
  private val pathStyle =
    org.jline.utils.AttributedStyle.DEFAULT.foreground(org.jline.utils.AttributedStyle.YELLOW)

  override def highlight(reader: LineReader, buffer: String): AttributedString =
    val builder = new org.jline.utils.AttributedStringBuilder()
    val parts = buffer.split("\\s+", 2)
    if parts.nonEmpty then
      val cmd = parts(0).toLowerCase
      val cmdStyle = cmd match
        case "save" | "load" => keywordStyle
        case _               => org.jline.utils.AttributedStyle.DEFAULT
      builder.style(cmdStyle).append(parts(0))
      if parts.length > 1 then builder.style(pathStyle).append(" ").append(parts(1))
    builder.toAttributedString

case class TuiReisen @Inject() (controller: IController) extends Listener:
  controller.add(this)

  private val presenter = TuiPresenter(
    controller,
    onRefresh = () => refresh()
  )

  private val Esc = "\u001b"
  private val Up = "\u001b[A"
  private val Down = "\u001b[B"

  private val logo: Vector[String] =
    """|.------..------..------..------..------..------..------..------..------.
       ||P.--. ||R.--. ||E.--. ||S.--. ||I.--. ||D.--. ||E.--. ||N.--. ||T.--. |
       || :/\: || :(): || (\/) || :/\: || (\/) || :/\: || (\/) || :(): || :/\: |
       || (__) || ()() || :\/: || :\/: || :\/: || (__) || :\/: || ()() || (__) |
       || '--'P|| '--'R|| '--'E|| '--'S|| '--'I|| '--'D|| '--'E|| '--'N|| '--'T|
       |`------'`------'`------'`------'`------'`------'`------'`------'`------'""".stripMargin.linesIterator.toVector

  private var shouldClose = false
  private var renderScale = 1
  private var currentTab: GameTab = GameTab.Players
  private var selectedCardIndex = -1
  private var statusBuffer = ""

  private val terminal: Terminal = TerminalBuilder.builder().system(true).color(true).build()
  private val renderer: TerminalRenderer = TerminalRenderer(terminal)

  private val history = DefaultHistory()
  private val pathCompleter = PathCompleter()
  private val highlighter = PresidentHighlighter()

  private val lineReader = LineReaderBuilder
    .builder()
    .terminal(terminal)
    .completer(pathCompleter)
    .highlighter(highlighter)
    .history(history)
    .build()

  terminal.handle(
    Terminal.Signal.INT,
    _ =>
      terminal.writer().println("Exiting")
      terminal.flush()
      controller.exit()
  )

  terminal.handle(
    Terminal.Signal.WINCH,
    _ => renderer.windowSizeChanged()
  )

  renderer.initialize()

  def run(): Unit =
    try
      val bindingReader = BindingReader(terminal.reader())
      val keyMap = KeyMap[String]()
      bindKeys(keyMap)

      renderer.render(centeredLogoLines())
      Thread.sleep(2500)

      presenter.viewModel.currentView = View.Menu
      refresh()

      while !shouldClose do
        val operation = bindingReader.readBinding(keyMap)
        if operation != null then
          KeyAction.fromString(operation) match
            case Some(action) => processAction(action)
            case None         => ()
      end while
    finally
      renderer.clear()
      renderer.close()

  private def bindKeys(keyMap: KeyMap[String]): Unit =
    keyMap.bind("quit", "q", "Q")
    keyMap.bind("plus", "+")
    keyMap.bind("minus", "-")
    keyMap.bind("undo", "z", "Z")
    keyMap.bind("redo", "y", "Y")
    keyMap.bind("abort", Esc)
    keyMap.bind("up", Up)
    keyMap.bind("down", Down)
    keyMap.bind("tab-next", "\t")
    keyMap.bind("tab-prev", "\u0019")
    keyMap.bind("pass", "p")
    keyMap.bind("next-round", "n", "N")
    keyMap.bind("save", "s", "S")
    keyMap.bind("load", "l", "L")
    keyMap.bind("back", "b", "B")
    keyMap.bind("navigate-menu", "1")
    keyMap.bind("navigate-lobby", "2")
    keyMap.bind("navigate-game", "3")
    keyMap.bind("navigate-result", "4")
    keyMap.bind("computer-turn", "c")
    keyMap.bind("add-player", "a")
    keyMap.bind("card-prev", Up)
    keyMap.bind("card-next", Down)
    keyMap.bind("card-confirm", "\n", "\r")

  private def processAction(action: KeyAction): Unit = action match
    case KeyAction.Quit => controller.exit()

    case KeyAction.CardConfirm =>
      presenter.viewModel.currentView match
        case View.Menu   => presenter.navigateTo(View.Lobby)
        case View.Lobby  => presenter.startGame()
        case View.Result =>
          if controller.getGameState == Ended then presenter.nextRound()
        case View.Game =>
          if controller.getGameState == Playing then handleCardConfirm()

    case KeyAction.Plus  => handlePlus()
    case KeyAction.Minus => handleMinus()
    case KeyAction.Undo  => presenter.undo()
    case KeyAction.Redo  => presenter.redo()

    case KeyAction.Abort =>
      if controller.getGameState == Playing then presenter.abortGame()

    case KeyAction.TabNext => currentTab = currentTab.next; refresh()
    case KeyAction.TabPrev => currentTab = currentTab.prev; refresh()

    case KeyAction.Pass => handlePass()

    case KeyAction.NextRound =>
      if controller.getGameState == Ended then presenter.nextRound()

    case KeyAction.Save => handleSave()
    case KeyAction.Load => handleLoad()
    case KeyAction.Back => handleBack()

    case KeyAction.NavigateMenu   => presenter.navigateTo(View.Menu)
    case KeyAction.NavigateLobby  => presenter.navigateTo(View.Lobby)
    case KeyAction.NavigateGame   => presenter.navigateTo(presenter.viewForGame(controller.getGame))
    case KeyAction.NavigateResult => presenter.navigateTo(View.Result)

    case KeyAction.ComputerTurn => presenter.triggerComputerPlay()
    case KeyAction.AddPlayer    => handleAddPlayer()

    case KeyAction.CardSelectPrev => handleCardSelect(-1)
    case KeyAction.CardSelectNext => handleCardSelect(1)

  private def refresh(): Unit =
    statusBuffer = presenter.statusMessage
    val lines = renderLinesForState()
    renderer.render(lines)

  override def onEvent(event: GameEvent): Unit =
    event match
      case GameExitEvent => shouldClose = true
      case _             =>
        presenter.handleEvent(event)
        refresh()

  // ── Input handlers ─────────────────────────────────────────────────────

  private def handlePlus(): Unit =
    controller.getGame.state match
      case Playing =>
        renderScale = math.min(3, renderScale + 1); renderer.windowSizeChanged()
      case WaitingForPlayers =>
        val name = s"Bot ${controller.playerCount + 1}"
        presenter.addComputerPlayer(name, PlayRandomCardStrategy())
      case _ => ()

  private def handleMinus(): Unit =
    controller.getGame.state match
      case Playing => renderScale = math.max(1, renderScale - 1)
      case _       => ()

  private def handleCardSelect(delta: Int): Unit =
    val game = controller.getGame
    if game.state == Playing then
      game.currentPlayer.foreach { player =>
        val hand = game.playerHands.getOrElse(player, Vector.empty)
        if hand.nonEmpty then
          val size = hand.size
          selectedCardIndex =
            if selectedCardIndex < 0 then if delta > 0 then 0 else size - 1
            else (selectedCardIndex + delta + size) % size
          refresh()
      }

  private def handleCardConfirm(): Unit =
    val game = controller.getGame
    if game.state == Playing && selectedCardIndex >= 0 then
      game.currentPlayer.foreach { player =>
        val hand = game.playerHands.getOrElse(player, Vector.empty)
        if selectedCardIndex < hand.size then
          presenter.playSelectedCard(player, hand(selectedCardIndex))
          selectedCardIndex = -1
      }

  private def handlePass(): Unit =
    val game = controller.getGame
    if game.state == Playing then game.currentPlayer.foreach(presenter.passTrick)

  private def handleBack(): Unit =
    presenter.viewModel.currentView match
      case View.Lobby  => presenter.navigateTo(View.Menu)
      case View.Game   => presenter.navigateTo(View.Lobby)
      case View.Result => presenter.navigateTo(View.Menu)
      case _           => ()

  private def handleSave(): Unit =
    try
      val prompt = "Save path [save.json]: ".bold
      val path = lineReader.readLine(prompt)
      val savePath = if path.isEmpty then "save.json" else path
      presenter.save(savePath)
      statusBuffer = s"Saved to $savePath"
      refresh()
    catch
      case _: EndOfFileException     => ()
      case _: UserInterruptException => ()

  private def handleLoad(): Unit =
    try
      val prompt = "Load path [save.json]: ".bold
      val path = lineReader.readLine(prompt)
      val loadPath = if path.isEmpty then "save.json" else path
      presenter.load(loadPath)
      presenter.navigateTo(presenter.viewForGame(controller.getGame))
    catch
      case _: EndOfFileException     => ()
      case _: UserInterruptException => ()

  private def handleAddPlayer(): Unit =
    try
      val prompt = s"Player name [Player ${controller.playerCount + 1}]: ".bold
      val name = lineReader.readLine(prompt)
      val finalName = if name.isEmpty then s"Player ${controller.playerCount + 1}" else name
      presenter.addPlayer(finalName)
    catch
      case _: EndOfFileException     => ()
      case _: UserInterruptException => ()

  // ── View rendering ──────────────────────────────────────────────────────

  private def renderLinesForState(): Vector[String] =
    presenter.viewModel.currentView match
      case View.Menu   => renderMenu()
      case View.Lobby  => renderLobby()
      case View.Game   => renderGame()
      case View.Result => renderResult()

  private def renderMenu(): Vector[String] =
    Vector(
      "",
      "  President - Card Game".bold,
      "",
      "  Enter/2 Open Lobby | l Load Game    | z/y Undo/Redo  | q Quit",
      ""
    ) ++ statusLines()

  private def renderLobby(): Vector[String] =
    val game = controller.getGame
    val players = game.playerHands.keys.toVector.sortBy(_.name)
    val count = players.size

    val header = Vector(
      "",
      "  Lobby".bold,
      s"  Players: $count",
      ""
    )

    val playerLines =
      if players.isEmpty then Vector("  (no players)")
      else
        players.zipWithIndex.map { (p, i) =>
          val strategy = p.playerType.strategy
            .map(s => s" [${s.name}]")
            .getOrElse("")
          val nameStr = s"  ${i + 1}. ${p.name}$strategy"
          val cardsStr = s" (${game.playerHands(p).size} cards)"
          nameStr.bold + cardsStr
        }

    val controls = Vector(
      "",
      "  + Add bot        | a Add human    | Enter Start     | b Back          | z/y Undo/Redo  | q Quit"
    ) ++ statusLines()

    header ++ playerLines ++ controls

  private def renderGame(): Vector[String] =
    val game = controller.getGame
    if game.state != Playing then return renderGameNonPlaying(game)

    val header = Vector(
      "",
      ("  Game - Round " + game.roundNumber).bold,
      ""
    )

    val turnLine = game.currentPlayer match
      case Some(p) => s"  Turn: ${p.name.boldGreen}"
      case None    => ""

    val controls = Vector(
      turnLine,
      "  ↑↓ Select        | Enter Play      | p Pass          | c Bot turn      | Esc Abort",
      "  Tab Switch view  | +/- Scale       | z/y Undo/Redo",
      "  s Save           | l Load          | b Back          | q Quit"
    ) ++ statusLines()

    val trickAndHand = renderTrickAndHand(game)

    val content = currentTab match
      case GameTab.Players => renderGamePlayers(game)
      case GameTab.Scores  => renderGameScores(game)
      case GameTab.Hands   => renderGameHands(game)

    header ++ controls ++ Vector(tabBar(), "") ++ trickAndHand ++ content

  private def renderGameNonPlaying(game: Game): Vector[String] =
    Vector(
      "",
      s"  Game - Round ${game.roundNumber}".bold,
      s"  State: ${game.state}",
      ""
    ) ++ statusLines()

  private def tabBar(): String =
    val tabs = Vector(("Players", 0), ("Scores", 1), ("Hands", 2))
    val tabLine = tabs
      .map { (name, i) =>
        val isActive = currentTab match
          case GameTab.Players if i == 0 => true
          case GameTab.Scores if i == 1  => true
          case GameTab.Hands if i == 2   => true
          case _                         => false
        if isActive then s"[$name]".boldYellow
        else s"$name".dim
      }
      .mkString("  ")
    s"  $tabLine"

  private def renderGamePlayers(game: Game): Vector[String] =
    val players = game.playerHands.keys.toVector.sortBy(_.name)
    if players.isEmpty then return Vector("  (no players)")

    val typeNames = players.map { p =>
      p.playerType match
        case Computer(s) => s"Bot (${s.name})"
        case Human       => "Human"
        case Unknown     => "Unknown"
    }
    val maxNameLen = math.max(4, players.map(_.name.length).maxOption.getOrElse(4))
    val maxTypeLen = math.max(4, typeNames.map(_.length).maxOption.getOrElse(4))

    val header = "  " + padAnsi("Name", maxNameLen + 2) + " " + padAnsi("Type", maxTypeLen + 2) + "  Cards  ID"
    val separator = "  " + "─" * (maxNameLen + maxTypeLen + 18)
    val rows = players.zip(typeNames).map { (p, typeName) =>
      val cards = game.playerHands(p).size
      val typeStr = p.playerType match
        case Computer(_) => typeName.dim
        case _           => typeName
      val current =
        if game.currentPlayer.contains(p) then " *".boldYellow
        else "  "
      val nameStr = p.name.bold
      s"$current${padAnsi(nameStr, maxNameLen + 2)} ${padAnsi(typeStr, maxTypeLen + 2)} ${cards.toString.reverse.padTo(4, ' ').reverse}  ${p.id.toString.take(8)}"
    }
    val table = Vector(header, separator) ++ rows

    val handLines = game.currentPlayer match
      case Some(p) =>
        val hand = game.playerHands.getOrElse(p, Vector.empty)
        val cardLines = hand.zipWithIndex.map { (card, i) =>
          val marker =
            if i == selectedCardIndex then " >".yellow else "  "
          s"$marker$i  ${card.cardText}"
        }
        Vector("", s"  ${p.name.bold}'s hand (${hand.size}):") ++ cardLines
      case None => Vector.empty

    table ++ handLines

  private def renderGameScores(game: Game): Vector[String] =
    val players = game.playerHands.keys.toVector.sortBy(_.name)
    val target = 11
    if players.isEmpty then return Vector("  (no scores)")

    val header = Vector(
      "  Name                 Score  Progress".bold
    )
    val separator = Vector("  " + "─" * 50)
    val rows = players.map { p =>
      val score = game.scoredRanks.getOrElse(p, 0)
      val filled = (score * 20) / target
      val bar = ("█" * filled).green + ("░" * (20 - filled)).dim
      val nameStr = p.name.bold
      f"${padAnsi(nameStr, 20)} $score%2d / $target  $bar"
    }
    header ++ separator ++ rows

  private def renderGameHands(game: Game): Vector[String] =
    val currentPlayer = game.currentPlayer
    val entries = currentPlayer match
      case Some(p) =>
        Vector(p -> game.playerHands.getOrElse(p, Vector.empty))
      case None => Vector.empty

    if entries.isEmpty then return Vector("  (no hands to display)")

    val (player, hand) = entries.head
    val lines = Vector(
      s"  ${player.name.bold}'s hand (${hand.size} cards):",
      ""
    )

    if hand.isEmpty then lines :+ "  (empty)"
    else
      val cardLines = hand.zipWithIndex.map { (card, i) =>
        val marker =
          if i == selectedCardIndex then " >".yellow else "  "
        s"$marker$i  ${card.cardText}  (${card.rank.name})"
      }
      lines ++ cardLines

  private def renderTrickAndHand(game: Game): Vector[String] =
    val trickCards = game.playedCards
    if trickCards.isEmpty then Vector("  Cards on table: (empty)")
    else
      val cards = trickCards.map(_.cardText).mkString("  ")
      val rank = game.trickRank.map(_.name).getOrElse("")
      val leader = game.trickLeader.map(_.name).getOrElse("")
      val extra =
        if rank.nonEmpty then s"  (${rank} by ${leader})".dim
        else ""
      Vector(s"  Cards on table:$extra  $cards")

  private def renderResult(): Vector[String] =
    val game = controller.getGame
    val title = presenter.resultTitle

    val header = Vector(
      "",
      s"  $title".boldGreen,
      s"  Round ${game.roundNumber}",
      ""
    )

    val winner =
      game.finishOrder.headOption.map(_.name).getOrElse("-")
    val scoreLines = Vector(s"  Winner: ${winner.bold}", "")

    val players = game.playerHands.keys.toVector.sortBy(_.name)
    val playerLines =
      if players.isEmpty then Vector("  (no players)")
      else
        players.map { p =>
          val score = game.scoredRanks.getOrElse(p, 0)
          val finished = game.finishOrder.indexWhere(_ == p)
          val pos = if finished >= 0 then s" #${finished + 1}" else ""
          s"  ${p.name.bold}$pos - $score pts"
        }

    val isEnded = game.state == Ended
    val controls = Vector("")
      ++ (if isEnded then Vector("  Enter/n Next round               | ")
          else Vector())
      ++ Vector(
        "  b Back to lobby  | 1 Menu          | s Save          | q Quit"
      ) ++ statusLines()

    header ++ scoreLines ++ playerLines ++ controls

  private def padAnsi(text: String, width: Int): String =
    if width <= 0 then ""
    else
      val attributed = AttributedString
        .fromAnsi(text, terminal)
        .columnSubSequence(terminal, 0, width)
      val visibleCols = attributed.columnLength(terminal)
      attributed.toAnsi(terminal) + (" " * math.max(0, width - visibleCols))

  private def statusLines(): Vector[String] =
    if statusBuffer.isEmpty then Vector.empty
    else if presenter.isErrorMessage then Vector("", s"  >> ${statusBuffer.red}", "")
    else Vector("", s"  >> ${statusBuffer.green}", "")

  private def centeredLogoLines(): Vector[String] =
    val termSize = renderer.size
    logo.map { line =>
      val leftPad =
        math.max(0, (termSize.columns - line.length) / 2)
      " " * leftPad + line
    }
