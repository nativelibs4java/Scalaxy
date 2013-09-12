package scalaxy.json.base

import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.collection.immutable

object JSONPseudoParsingUtils {
  private val stringsPattern = """
    "([^\\"]+|\\"|\\\\|\\\w)*"|'([^\\']+|\\'|\\\\|\\\w)*'
  """.trim
  private val commentsPattern = """//[^\n]*|/\*(\*[^/]|[^*])*?\*/"""
  private val stringsAndCommentsRx = (stringsPattern + "|" + commentsPattern).r

  import PseudoParsingUtils._

  implicit class RegexJSONExt(val r: Regex) extends AnyVal {
    def findAllMatchOutsideJSONCommentsAndStringsIn(s: CharSequence) =
      r.findAllMatchOutsidePatternIn(s, outsidePattern = stringsAndCommentsRx)

    def replaceAllOutsideJSONCommentsAndStringsIn(target: CharSequence, replacer: Match => String) =
      r.replaceAllOutsidePatternIn(target, replacer, outsidePattern = stringsAndCommentsRx)      
  }
}
