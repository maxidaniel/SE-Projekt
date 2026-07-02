package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.models.{Card, CardSuit}
import org.jline.utils.{AttributedStringBuilder, AttributedStyle}

/** Opaque type wrapper for ANSI escape sequences */
opaque type AnsiCode = String

object AnsiCode:
  inline def apply(code: String): AnsiCode = code
  extension (a: AnsiCode)
    inline def value: String = a
    inline def +(other: AnsiCode): AnsiCode = a + other

  /** Reset all styles */
  val Reset: AnsiCode = "\u001b[0m"

/** Rich terminal styling via extension methods on String and Card */
extension (text: String)
  def red: String = TuiColors.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED), text)
  def green: String = TuiColors.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN), text)
  def blue: String = TuiColors.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE), text)
  def cyan: String = TuiColors.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN), text)
  def magenta: String = TuiColors.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA), text)
  def yellow: String = TuiColors.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW), text)
  def white: String = TuiColors.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE), text)
  def bold: String = TuiColors.styled(AttributedStyle.DEFAULT.bold, text)
  def dim: String = TuiColors.styled(AttributedStyle.DEFAULT.faint, text)
  def italic: String = TuiColors.styled(AttributedStyle.DEFAULT.italic, text)
  def underline: String = TuiColors.styled(AttributedStyle.DEFAULT.underline, text)
  def boldGreen: String = TuiColors.styled(AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.GREEN), text)
  def boldYellow: String = TuiColors.styled(AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.YELLOW), text)
  def boldRed: String = TuiColors.styled(AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.RED), text)
  def boldBlue: String = TuiColors.styled(AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.BLUE), text)
  def boldCyan: String = TuiColors.styled(AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.CYAN), text)
  def boldMagenta: String = TuiColors.styled(AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.MAGENTA), text)
  def bgRed: String = TuiColors.styled(AttributedStyle.DEFAULT.background(AttributedStyle.RED), text)
  def bgGreen: String = TuiColors.styled(AttributedStyle.DEFAULT.background(AttributedStyle.GREEN), text)
  def bgBlue: String = TuiColors.styled(AttributedStyle.DEFAULT.background(AttributedStyle.BLUE), text)
  def bgYellow: String = TuiColors.styled(AttributedStyle.DEFAULT.background(AttributedStyle.YELLOW), text)
  def bgCyan: String = TuiColors.styled(AttributedStyle.DEFAULT.background(AttributedStyle.CYAN), text)
  def onBlack: String = TuiColors.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK), text)

extension (card: Card)
  def cardText: String =
    val isRed = card.suit == CardSuit.Hearts || card.suit == CardSuit.Diamonds
    val rankStr = if card.rank == null then "?" else card.rank.symbol
    val suitStr = if card.suit == null then "?" else card.suit.symbol
    if isRed then s"$rankStr $suitStr".red else s"$rankStr $suitStr"

  def cardRankText: String =
    val rankStr = if card.rank == null then "?" else card.rank.name
    if card.suit == CardSuit.Hearts || card.suit == CardSuit.Diamonds then rankStr.red else rankStr

object TuiColors:
  def styled(style: AttributedStyle, text: String): String =
    new AttributedStringBuilder()
      .style(style)
      .append(text)
      .style(AttributedStyle.DEFAULT)
      .toAttributedString
      .toAnsi()

  /** Build an AttributedString with multiple style segments */
  def buildStyled(segments: (AttributedStyle, String)*): String =
    val builder = new AttributedStringBuilder()
    segments.foreach { case (style, text) =>
      builder.style(style).append(text)
    }
    builder.style(AttributedStyle.DEFAULT).toAttributedString.toAnsi()

  /** Commonly used styles */
  val SuccessStyle: AttributedStyle = AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.GREEN)
  val ErrorStyle: AttributedStyle = AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.RED)
  val WarningStyle: AttributedStyle = AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.YELLOW)
  val InfoStyle: AttributedStyle = AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.CYAN)
  val DimStyle: AttributedStyle = AttributedStyle.DEFAULT.faint
  val HighlightStyle: AttributedStyle = AttributedStyle.DEFAULT.bold.foreground(AttributedStyle.MAGENTA)

  def success(text: String): String = styled(SuccessStyle, text)
  def error(text: String): String = styled(ErrorStyle, text)
  def warning(text: String): String = styled(WarningStyle, text)
  def info(text: String): String = styled(InfoStyle, text)
  def dim(text: String): String = styled(DimStyle, text)
  def highlight(text: String): String = styled(HighlightStyle, text)
