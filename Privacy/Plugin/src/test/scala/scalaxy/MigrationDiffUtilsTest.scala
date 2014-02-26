package scalaxy.privacy.test

import scalaxy.privacy.MigrationDiffUtils
import org.junit._
import org.junit.Assert._

class MigrationDiffUtilsTest {

  import MigrationDiffUtils._

  @Test
  def twoLines {
    val file = "a.scala"

    val lineContent1 = "def f"
    //           pos:       ^
    //       columns:       5
    val lineContent3 = "def g"
    //           pos:       ^
    //       columns:       5
    val mods = List(
      PrivateModifier(SourcePos(file, line = 3, column = 5, lineContent = lineContent3)),
      PrivateModifier(SourcePos(file, line = 1, column = 5, lineContent = lineContent1))
    )

    assertEquals(
      """
        --- a.scala
        +++ a.scala
        @@ -1,1 +1,1 @@
        -def f
        +private[this] def f
        @@ -3,1 +3,1 @@
        -def g
        +private[this] def g
      """.trim.replaceAll("\n\\s+", "\n"),
      createRevertModificationsDiff(mods).trim)
  }

  @Test
  def multipleModifiersInOneLine {
    val file = "a.scala"

    val lineContent1 = "class Foo(val x: Int, y: Double)"
    //             pos:       ^       ^
    //         columns:       7       15
    val mods = List(
      PrivateModifier(SourcePos(file, line = 1, column = 7, lineContent = lineContent1)),
      PrivateModifier(SourcePos(file, line = 1, column = 15, lineContent = lineContent1))
    )

    assertEquals(
      """
        --- a.scala
        +++ a.scala
        @@ -1,1 +1,1 @@
        -class Foo(val x: Int, y: Double)
        +private[this] class Foo(private[this] val x: Int, y: Double)
      """.trim.replaceAll("\n\\s+", "\n"),
      createRevertModificationsDiff(mods).trim)
  }

  @Test
  def multipleDeclVal {
    val file = "a.scala"
    val lineContent2 = "val x = 10, y = 12"
    //     pos:             ^       ^
    // columns:             5       13
    val mods = List(
      PrivateModifier(SourcePos(file, line = 2, column = 5, lineContent = lineContent2)),
      PrivateModifier(SourcePos(file, line = 2, column = 13, lineContent = lineContent2))
    )

    assertEquals(
      """
        --- a.scala
        +++ a.scala
        @@ -2,1 +2,1 @@
        -val x = 10, y = 12
        +private[this] val x = 10, y = 12
      """.trim.replaceAll("\n\\s+", "\n"),
      createRevertModificationsDiff(mods).trim)
  }
}
