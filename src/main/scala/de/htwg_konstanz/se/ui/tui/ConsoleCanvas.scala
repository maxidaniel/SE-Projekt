package de.htwg_konstanz.se.ui.tui

enum RenderAlignment:
  case Left, Centered, Right

case class RenderObj(
    x: Int,
    y: Int,
    lines: Vector[String],
    alignment: RenderAlignment = RenderAlignment.Left,
    width: Option[Int] = None
)

object RenderObj:
  def Left(x: Int, y: Int, lines: Vector[String], width: Option[Int] = None): RenderObj =
    RenderObj(x, y, lines, RenderAlignment.Left, width)

  def Centered(x: Int, y: Int, lines: Vector[String], width: Option[Int] = None): RenderObj =
    RenderObj(x, y, lines, RenderAlignment.Centered, width)

  def Right(x: Int, y: Int, lines: Vector[String], width: Option[Int] = None): RenderObj =
    RenderObj(x, y, lines, RenderAlignment.Right, width)

object ConsoleCanvas:
  def renderFrame(width: Int, height: Int, renderObjs: Seq[RenderObj]): Vector[String] =
    val canvas = ConsoleCanvas(width, height)
    canvas.drawRenderObjs(renderObjs)
    canvas.render()

case class ConsoleCanvas(width: Int, height: Int, backgroundChar: Char = ' '):
  private val safeWidth = math.max(0, width)
  private val safeHeight = math.max(0, height)
  private val buffer: Array[Array[Char]] = Array.fill(safeHeight, safeWidth)(backgroundChar)

  def clear(char: Char = backgroundChar): Unit =
    for
      y <- 0 until safeHeight
      x <- 0 until safeWidth
    do buffer(y)(x) = char

  def drawText(x: Int, y: Int, text: String): Unit =
    if y < 0 || y >= safeHeight then return
    text.zipWithIndex.foreach { (char, offset) =>
      val drawX = x + offset
      if drawX >= 0 && drawX < safeWidth then buffer(y)(drawX) = char
    }

  def drawHorizontalLine(x: Int, y: Int, length: Int, char: Char = '─'): Unit =
    if length <= 0 || y < 0 || y >= safeHeight then return
    for offset <- 0 until length do drawPoint(x + offset, y, char)

  def drawVerticalLine(x: Int, y: Int, length: Int, char: Char = '│'): Unit =
    if length <= 0 || x < 0 || x >= safeWidth then return
    for offset <- 0 until length do drawPoint(x, y + offset, char)

  def drawBox(
      x: Int,
      y: Int,
      width: Int,
      height: Int,
      horizontal: Char = '─',
      vertical: Char = '│',
      topLeft: Char = '┌',
      topRight: Char = '┐',
      bottomLeft: Char = '└',
      bottomRight: Char = '┘'
  ): Unit =
    if width <= 0 || height <= 0 then return
    if width == 1 && height == 1 then
      drawPoint(x, y, topLeft)
      return

    drawPoint(x, y, topLeft)
    drawPoint(x + width - 1, y, topRight)
    drawPoint(x, y + height - 1, bottomLeft)
    drawPoint(x + width - 1, y + height - 1, bottomRight)
    if width > 2 then
      drawHorizontalLine(x + 1, y, width - 2, horizontal)
      drawHorizontalLine(x + 1, y + height - 1, width - 2, horizontal)
    if height > 2 then
      drawVerticalLine(x, y + 1, height - 2, vertical)
      drawVerticalLine(x + width - 1, y + 1, height - 2, vertical)

  def fillRect(x: Int, y: Int, width: Int, height: Int, char: Char = ' '): Unit =
    if width <= 0 || height <= 0 then return
    for
      row <- 0 until height
      col <- 0 until width
    do drawPoint(x + col, y + row, char)

  def drawRenderObj(renderObj: RenderObj): Unit =
    val alignWidth = renderObj.width.getOrElse(renderObj.lines.map(_.length).maxOption.getOrElse(0))
    renderObj.lines.zipWithIndex.foreach { (line, lineIndex) =>
      val alignedLine = alignLine(line, renderObj.alignment, alignWidth)
      drawText(renderObj.x, renderObj.y + lineIndex, alignedLine)
    }

  def drawRenderObjs(renderObjs: Seq[RenderObj]): Unit =
    renderObjs.foreach(drawRenderObj)

  def render(): Vector[String] =
    buffer.iterator.map(_.mkString).toVector

  private def drawPoint(x: Int, y: Int, char: Char): Unit =
    if x < 0 || y < 0 || x >= safeWidth || y >= safeHeight then return
    buffer(y)(x) = char

  private def alignLine(line: String, alignment: RenderAlignment, width: Int): String =
    if width <= line.length then return line
    val missing = width - line.length

    alignment match
      case RenderAlignment.Left     => line + (" " * missing)
      case RenderAlignment.Right    => (" " * missing) + line
      case RenderAlignment.Centered =>
        val leftPad = missing / 2
        val rightPad = missing - leftPad
        (" " * leftPad) + line + (" " * rightPad)
