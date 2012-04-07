package scalaxy

import scala.reflect._
class Replacement(val pattern: mirror.Tree, val replacement: mirror.Tree) {
  override def toString =
    "Replacement(" + pattern + ", " + replacement + ")"
}
  
object Macros {
  
  //mirror.{ Tree => mirror.Tree, Name => mirror.Name, Ident => MIdent }
  
  
  /*
  def selectTerms(terms: mirror.Name*): mirror.Tree = {
    val x :: xs = terms.toList
    terms.foldLeft[mirror.Tree](mirror.Ident(mirror.newTermTree(x)))(
      (t, n) => mirror.Select(t, mirror.n)
    )
  }*/
  
  def macro Replacement[T](pattern: T, replacement: T): Replacement = {
    val rpattern = reify(pattern)
    val rreplacement = reify(replacement)
    
    //println("pattern = " + rpattern)
    New(
      Select(Ident(newTermName("scalaxy")), newTypeName("Replacement")), 
      List(List(rpattern, rreplacement))
    )
  }
  
  def macro tree(v: Any): mirror.Tree = 
    reify(v)
}