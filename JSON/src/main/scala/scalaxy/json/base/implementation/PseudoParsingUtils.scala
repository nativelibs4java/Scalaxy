package scalaxy.json.base

import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.collection.immutable

object PseudoParsingUtils {
  implicit class RegexExt(val r: Regex) extends AnyVal {
    def findAllRangesIn(s: CharSequence) =
      immutable.TreeMap[Int, Int]() ++ r.findAllMatchIn(s).map(m => m.start -> m.end)

    def findAllMatchOutsidePatternIn(s: CharSequence, outsidePattern: Regex): Iterator[Match] =
      findAllMatchOutsideRangesIn(s, outsidePattern.findAllRangesIn(s))

    def replaceAllOutsidePatternIn(target: CharSequence, replacer: Match => String, outsidePattern: Regex) = {
      val filter = getMatchFilter(outsidePattern.findAllRangesIn(target))
      r.replaceAllIn(target, m => if (filter(m)) m.matched else replacer(m))
    }

    private def getMatchFilter(bannedRanges: immutable.TreeMap[Int, Int]): Match => Boolean =
      m => {
        !bannedRanges.to(m.start).lastOption.exists(_._2 >= m.start) &&
        !bannedRanges.from(m.start).headOption.exists(_._1 < m.end)
      }

    def findAllMatchOutsideRangesIn(s: CharSequence, bannedRanges: immutable.TreeMap[Int, Int]): Iterator[Regex.Match] = {
      r.findAllMatchIn(s).filter(getMatchFilter(bannedRanges))
    }
  }

  // implicit class StringExt(val s: String) extends AnyVal {
  //   def visitSkippingRanges(bannedRanges: immutable.TreeMap[Int, Int], replacementChar: Char, f: Char => Unit) {
      
  //   }
  // }
}
