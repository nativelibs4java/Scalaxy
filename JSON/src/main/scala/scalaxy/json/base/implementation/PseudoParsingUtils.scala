package scalaxy.json.base

import scala.util.matching._
import scala.collection.immutable

object PseudoParsingUtils {
  private val stringsPattern = """
    "([^\\"]+|\\"|\\\\|\\\w)*"|'([^\\']+|\\'|\\\\|\\\w)*'
  """.trim
  private val commentsPattern = """//[^\n]*|/\*(\*[^/]|[^*])*?\*/"""
  private val stringsAndCommentsRx = (stringsPattern + "|" + commentsPattern).r

  implicit class RegexExt(val r: Regex) extends AnyVal {
    def findAllRangesIn(s: String) =
      immutable.TreeMap[Int, Int]() ++ r.findAllMatchIn(s).map(m => m.start -> m.end)

    def findAllMatchOutsidePatternIn(s: String, outsidePattern: Regex): Iterator[Regex.Match] = {
      val bannedRanges = outsidePattern.findAllRangesIn(s)
      r.findAllMatchIn(s).filter(m => {
        !bannedRanges.to(m.start).lastOption.exists(_._2 >= m.start) &&
        !bannedRanges.from(m.start).headOption.exists(_._1 < m.end)
      })
    }
  }

  implicit class RegexJSONExt(val r: Regex) extends AnyVal {
    def findAllMatchOutsideJSONCommentsAndStringsIn(s: String) =
      r.findAllMatchOutsidePatternIn(s, outsidePattern = stringsAndCommentsRx)
  }
}
