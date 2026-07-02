package de.htwg_konstanz.se.ui.gui.components

import de.htwg_konstanz.se.models.{Card, CardRank, CardSuit}
import scalafx.scene.layout.{AnchorPane, BorderPane, StackPane, VBox}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Polygon
import scalafx.scene.text.{Font, Text}

object CardComponent {
  private val AspectRatio: Double = 5.0 / 7.0
  private val CornerSize: Double = 12.0
  private val InnerPadding: Double = 6.0
  private val CornerFontSize: Double = 14.0
  private val CenterFontSize: Double = 36.0
  private val BackPatternColor: Color = Color.web("#1a3a6b")
  private val BackBorderColor: Color = Color.web("#0f2444")
  private val BackInnerBorderColor: Color = Color.web("#2a5a9b")
}

class CardComponent(val card: Card, var faceDown: Boolean = false)
    extends StackPane {

  import CardComponent._

  minWidth = 40.0
  minHeight = 40.0 / AspectRatio
  prefWidth = 100.0
  prefHeight = 140.0

  private val frontPane: BorderPane = createFrontPane()
  private val backPane: BorderPane = createBackPane()

  width.onChange { (_, _, w) => minHeight = w.doubleValue * AspectRatio }
  height.onChange { (_, _, h) => minWidth = h.doubleValue / AspectRatio }

  children = if (faceDown) Seq(backPane) else Seq(frontPane)

  def flip(): Unit =
    if faceDown then
      children = Seq(frontPane)
      faceDown = false
    else
      children = Seq(backPane)
      faceDown = true

  def flipToFaceUp(): Unit =
    if faceDown then
      children = Seq(frontPane)
      faceDown = false

  def flipToFaceDown(): Unit =
    if !faceDown then
      children = Seq(backPane)
      faceDown = true

  private def isRed: Boolean =
    card.suit == CardSuit.Hearts || card.suit == CardSuit.Diamonds

  private def textColor: String = if isRed then "#c0392b" else "#1a1a2e"

  private def createFrontPane(): BorderPane =
    new BorderPane {
      style =
        s"-fx-background-color: white;" +
        s"-fx-background-radius: $CornerSize;" +
        s"-fx-border-color: #bdc3c7;" +
        s"-fx-border-radius: $CornerSize;" +
        s"-fx-border-width: 1;" +
        s"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 8, 0.3, 1, 2);" +
        s"-fx-padding: 0;"

      center = new AnchorPane {
        AnchorPane.setTopAnchor(this, InnerPadding)
        AnchorPane.setLeftAnchor(this, InnerPadding)
        AnchorPane.setRightAnchor(this, InnerPadding)
        AnchorPane.setBottomAnchor(this, InnerPadding)

        children = Seq(
          createCornerRank(card.rank, card.suit, top = true),
          createCenterSuit(card.suit, CenterFontSize),
          createCornerRank(card.rank, card.suit, top = false)
        )
      }
    }

  private def createCornerRank(rank: CardRank, suit: CardSuit, top: Boolean): StackPane = {
    val rankText = new Text(rank.symbol) {
      font = Font.font("Arial", CornerFontSize)
      fill = Color.web(textColor)
    }
    val suitText = new Text(suit.symbol) {
      font = Font.font("Arial", CornerFontSize * 0.85)
      fill = Color.web(textColor)
    }
    new StackPane {
      alignment = if top then scalafx.geometry.Pos.TopLeft else scalafx.geometry.Pos.BottomRight
      style = s"-fx-padding: ${InnerPadding - 1};"
      children = Seq(
        new VBox {
          alignment = scalafx.geometry.Pos.Center
          spacing = 0
          children = Seq(rankText, suitText)
        }
      )
      if top then
        AnchorPane.setTopAnchor(this, 0.0)
        AnchorPane.setLeftAnchor(this, 0.0)
      else
        AnchorPane.setBottomAnchor(this, 0.0)
        AnchorPane.setRightAnchor(this, 0.0)
    }
  }

  private def createCenterSuit(suit: CardSuit, size: Double): Text = {
    val t = new Text(suit.symbol) {
      font = Font.font("Arial", size)
      fill = Color.web(textColor)
    }
    AnchorPane.setTopAnchor(t, 0.0)
    AnchorPane.setBottomAnchor(t, 0.0)
    AnchorPane.setLeftAnchor(t, 0.0)
    AnchorPane.setRightAnchor(t, 0.0)
    t
  }

  private def createBackPane(): BorderPane =
    new BorderPane {
      style =
        s"-fx-background-color: white;" +
        s"-fx-background-radius: $CornerSize;" +
        s"-fx-border-color: $BackBorderColor;" +
        s"-fx-border-radius: $CornerSize;" +
        s"-fx-border-width: 2;" +
        s"-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.3, 1, 2);" +
        s"-fx-padding: 0;"

      center = new AnchorPane {
        AnchorPane.setTopAnchor(this, 4)
        AnchorPane.setLeftAnchor(this, 4)
        AnchorPane.setRightAnchor(this, 4)
        AnchorPane.setBottomAnchor(this, 4)

        children = Seq(
          new BorderPane {
            style =
              s"-fx-background-color: transparent;" +
              s"-fx-background-radius: ${CornerSize - 4};" +
              s"-fx-border-color: $BackInnerBorderColor;" +
              s"-fx-border-radius: ${CornerSize - 4};" +
              s"-fx-border-width: 1;"
          },
          createBackPattern()
        )
      }
    }

  private def createBackPattern(): VBox =
    new VBox {
      alignment = scalafx.geometry.Pos.Center
      spacing = 8.0
      children = createDiamondPattern(BackPatternColor)
    }

  private def createDiamondPattern(color: Color): Seq[scalafx.scene.Node] =
    for {
      row <- 0 until 7
      col <- 0 until 5
    } yield new StackPane {
      children = Seq(createMiniDiamond(color))
      layoutX = col * 16.0 + 4.0
      layoutY = row * 18.0 + (if col % 2 == 1 then 9.0 else 0.0) + 2.0
    }

  private def createMiniDiamond(color: Color): Polygon =
    new Polygon {
      points.addAll(-5.0, 0.0, 0.0, -5.0, 5.0, 0.0, 0.0, 5.0)
      fill = color
    }
}
