// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.privacy

object MigrationDiffUtils {

  import PrivacyComponent.defaultVisibilityString
  import java.util.regex.Pattern.quote

  case class SourcePos(file: String, line: Int, column: Int, lineContent: String)

  sealed trait Modification {
    def pos: SourcePos
  }
  case class PublicAnnotation(pos: SourcePos) extends Modification
  case class PrivateModifier(pos: SourcePos) extends Modification

  def createRevertModificationsDiff(modifications: List[Modification]): String = {
    if (modifications.isEmpty) {
      ""
    } else {
      val file = modifications.head.pos.file
      val builder = new collection.mutable.StringBuilder
      builder ++= s"--- $file\n"
      builder ++= s"+++ $file\n"
      for ((line, mods) <- modifications.groupBy(_.pos.line)) {
        val original = mods.head.pos.lineContent.stripLineEnd
        var modified = original.reverse
        for (mod <- mods.sortBy(_.pos.column)) {
          val rcol = original.length - mod.pos.column + 1
          val start = modified.substring(0, rcol)
          val end = modified.substring(rcol)

          val spacesRx = "\\s+"
          val reverseMemberRx = s"trait|object|class|case${spacesRx.reverse})class|object(|def|val|var".reverse
          //val reverseMemberRx = s"trait|object|+?)case+s\\:?(class|def|val|var".reverse
          val commentRx = """/\*(?:[^*]|\*[^/])*\*/"""

          val pattern = "^(\\s*(:?" + commentRx + "\\s*)?(?:" + reverseMemberRx + "|(?=\\s*[,(])))(?:\\s*esac|\\b)"
          // println(s"start = '$start'")
          // println(s"end = '$end'")
          // println(s"pattern = '$pattern'")
          modified = start + end.replaceAll(
            pattern,
            "$1 " + defaultVisibilityString.reverse)
        }
        modified = modified.reverse

        builder ++= s"@@ -$line,1 +$line,1 @@\n"
        builder ++= s"-$original\n"
        builder ++= s"+$modified\n"
      }
      builder.toString
    }
  }
}
