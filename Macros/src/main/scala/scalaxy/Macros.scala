package scalaxy

import scala.reflect._
class Replacement(val pattern: mirror.Tree, val replacement: mirror.Tree) {
  override def toString =
    "Replacement(" + pattern + ", " + replacement + ")"
}

//sealed trait AnalysisResult
//case class Warning(pos: mirror.Position, msg: String) extends AnalysisResult
//case class Error(pos: mirror.Position, msg: String) extends AnalysisResult

//class Analysis(val pattern: mirror.Tree, f: )
  
object Macros {
  implicit def tree2pos(tree: mirror.Tree) = 
    tree.pos
    
  def macro Replacement[T](pattern: T, replacement: T): Replacement = {
    New(
      Select(Ident(newTermName("scalaxy")), newTypeName("Replacement")), 
      List(List(reify(pattern), reify(replacement)))
    )
  }
  
  def macro tree(v: Any): mirror.Tree = 
    reify(v)
}