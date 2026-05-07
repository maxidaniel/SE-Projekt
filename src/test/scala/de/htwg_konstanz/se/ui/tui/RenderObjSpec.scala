package de.htwg_konstanz.se.ui.tui

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class RenderObjSpec extends AnyWordSpec {
  "A RenderObj" should {
    "default to left alignment and no fixed width" in {
      val obj = RenderObj(x = 2, y = 3, lines = Vector("hello"))

      obj.alignment should be(RenderAlignment.Left)
      obj.width should be(None)
    }

    "support explicit width on direct construction" in {
      val obj = RenderObj(x = 4, y = 5, lines = Vector("abc"), width = Some(20))

      obj.width should be(Some(20))
      obj.alignment should be(RenderAlignment.Left)
    }
  }

  "RenderObj.Left" should {
    "create a left-aligned object" in {
      val obj = RenderObj.Left(1, 2, Vector("line"), width = Some(10))

      obj should be(RenderObj(1, 2, Vector("line"), RenderAlignment.Left, Some(10)))
    }

    "default optional width to none" in {
      val obj = RenderObj.Left(1, 2, Vector("line"))

      obj.width should be(None)
      obj.alignment should be(RenderAlignment.Left)
    }
  }

  "RenderObj.Centered" should {
    "create a centered object" in {
      val obj = RenderObj.Centered(3, 4, Vector("line"))

      obj should be(RenderObj(3, 4, Vector("line"), RenderAlignment.Centered, None))
    }
  }

  "RenderObj.Right" should {
    "create a right-aligned object" in {
      val obj = RenderObj.Right(5, 6, Vector("line"), width = Some(12))

      obj should be(RenderObj(5, 6, Vector("line"), RenderAlignment.Right, Some(12)))
    }

    "default optional width to none" in {
      val obj = RenderObj.Right(5, 6, Vector("line"))

      obj.width should be(None)
      obj.alignment should be(RenderAlignment.Right)
    }
  }
}
