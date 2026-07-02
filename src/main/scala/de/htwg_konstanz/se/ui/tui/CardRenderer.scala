package de.htwg_konstanz.se.ui.tui

import de.htwg_konstanz.se.models.Card

case class CardRendererOptions(userScale: Int = 1, overlapColumns: Int = 4)

case class CardRenderResult(
    lines: Vector[String],
    offsetX: Int,
    offsetY: Int,
    scale: Int,
    step: Int
)

object CardRenderer:
  def render(
      cards: Seq[Card],
      terminalWidth: Int,
      terminalHeight: Int,
      options: CardRendererOptions = CardRendererOptions()
  ): CardRenderResult =
    if cards.isEmpty || terminalWidth <= 0 || terminalHeight <= 0 then return CardRenderResult(Vector.empty, 0, 0, 1, 0)

    val requestedScale = math.max(1, options.userScale)

    val chosenScale =
      (requestedScale to 1 by -1)
        .find(scale => cardHeight(scale) <= terminalHeight)
        .getOrElse(1)

    val cardHeightAtScale = cardHeight(chosenScale)
    val cardWidthAtScale = cardWidth(chosenScale)

    val preferredStep = math.max(1, cardWidthAtScale - math.max(0, options.overlapColumns))
    val maxStepByTerminal =
      if cards.size <= 1 then preferredStep
      else if terminalWidth <= cardWidthAtScale then 1
      else math.max(1, (terminalWidth - cardWidthAtScale) / (cards.size - 1))

    val step = math.min(preferredStep, maxStepByTerminal)
    val totalWidth = cardWidthAtScale + (cards.size - 1) * step

    val targetWidth = math.min(terminalWidth, totalWidth)
    val targetHeight = math.min(terminalHeight, cardHeightAtScale)

    val raw = Array.fill(targetHeight, targetWidth)(' ')
    cards.zipWithIndex.foreach { (card, index) =>
      drawCard(raw, card.render(chosenScale), index * step, 0)
    }

    val lines = raw.iterator.map(chars => trimLine(chars.mkString)).toVector

    CardRenderResult(
      lines = lines,
      offsetX = math.max(0, (terminalWidth - targetWidth) / 2),
      offsetY = math.max(0, (terminalHeight - targetHeight) / 2),
      scale = chosenScale,
      step = step
    )

  private def drawCard(canvas: Array[Array[Char]], cardLines: Vector[String], xOffset: Int, yOffset: Int): Unit =
    cardLines.zipWithIndex.foreach { (line, y) =>
      val canvasY = yOffset + y
      if canvasY >= 0 && canvasY < canvas.length then
        line.zipWithIndex.foreach { (char, x) =>
          val canvasX = xOffset + x
          if canvasX >= 0 && canvasX < canvas(canvasY).length then canvas(canvasY)(canvasX) = char
        }
    }

  private def trimLine(line: String): String =
    line.reverse.dropWhile(_ == ' ').reverse

  private def cardWidth(scale: Int): Int =
    Card.AceOfSpades.render(scale).headOption.map(_.length).getOrElse(0)

  private def cardHeight(scale: Int): Int =
    Card.AceOfSpades.render(scale).length
