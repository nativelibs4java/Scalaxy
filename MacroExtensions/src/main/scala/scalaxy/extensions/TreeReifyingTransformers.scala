// Author: Olivier Chafik (http://ochafik.com)
// Feel free to modify and reuse this for any purpose ("public domain / I don't care").
package scalaxy.extensions

import scala.collection.mutable

//import scala.reflect.api.Universe
import scala.tools.nsc.Global
import scala.reflect.internal.Flags
import scala.reflect.NameTransformer

trait TreeReifyingTransformers
{
  val global: Global
  import global._
  import definitions._
  
  def newConstant(v: Any) = 
    Literal(Constant(v))
    
  def newApplyList(args: Tree*): Tree =
    if (args.isEmpty)
      Ident("Nil": TermName)
    else
      newApply("List", args: _*)
    
  def newApply(f: String, args: Tree*): Tree =
    newApply(Ident(f: TermName), args: _*)
    
  def newApply(target: Tree, args: Tree*): Tree =
    Apply(target, args.toList)
    
  def newSelect(target: Tree, name: String): Tree =
    newApply("Select", target, newConstant(name))
  
  class TreeReifyingTransformer extends Transformer {
    def newTermIdent(n: String): Tree =
      newApply("Ident", transform(newTermName(n)))
      
    def transform(n: Name): Tree =
      newApply(if (n.isTermName) "newTermName" else "newTypeName", newConstant(n.toString))
    
    def transformApplyLike(f: String, target: Tree, args: List[Tree]): Tree =
      newApply(f, transform(target), newApplyList(args.map(transform(_)): _*))

    def transform(mods: Modifiers): Tree = {
      val Modifiers(flags, privateWithin, annotations) = mods
      newApply("Modifiers", transform(flags), transform(privateWithin), newApplyList(annotations.map(transform(_)): _*))
    }
    
    def transform(flags: FlagSet): Tree = {
      var v = flags: Long
      var names = Set[String]()
      def remove(f: Long): Boolean = {
        if ((v & f) != 0) {
          v = v & ~f
          true
        } else
          false
      }
      if (remove(Flag.LOCAL)) names += "LOCAL"
      if (remove(Flag.PARAM)) names += "PARAM"
      if (remove(Flag.IMPLICIT)) names += "IMPLICIT"
      if (remove(Flag.MUTABLE)) names += "MUTABLE"
      assert(v == 0, "Flag not handled yet: " + v)
      val flagTrees = names.map(n => Select(Ident("Flag": TermName), n))
      if (flagTrees.isEmpty)
        newTermIdent("NoMods")
      else
        flagTrees.reduceLeft[Tree]((a, b) => Apply(Select(a, encode("|")), List(b)))
    }
    
    override def transform(tree: Tree): Tree = tree match {
      case Apply(target, args) =>
        transformApplyLike("Apply", target, args)
      case TypeApply(target, args) =>
        transformApplyLike("TypeApply", target, args)
      case AppliedTypeTree(target, args) =>
        transformApplyLike("AppliedTypeTree", target, args)
      case ExistentialTypeTree(target, args) =>
        transformApplyLike("ExistentialTypeTree", target, args)
      case Ident(n) =>
        // TODO pass spliceable names in, and handle them here!
        newApply("Ident", transform(n))
      case Select(target, n) =>
        newApply("Select", transform(target), newConstant(n.toString))
      case Block(statements, value) =>
        newApply("Block", (statements :+ value).map(transform(_)): _*)
      case If(cond, a, b) =>
        newApply("If", transform(cond), transform(a), transform(b))
      case ValDef(mods, name, tpt, rhs) =>
        newApply("ValDef", transform(mods), transform(name), transform(tpt), transform(rhs))
      case _ if tree.isEmpty =>
        Ident("EmptyTree": TermName)
      case LabelDef(name, params, rhs) =>
        newApply("LabelDef", transform(name), newApplyList(params.map(transform(_)): _*), transform(rhs))
      case Literal(Constant(v)) =>
        newApply("Literal", newApply("Constant", newConstant(v)))
      case null => 
        null
      case _ =>
        // TODO ValDef, DefDef, ClassDef, Template, Function...
        println("TODO reify properly " + tree.getClass.getName + " <- " + tree.getClass.getSuperclass.getName + ": " + tree)
        newSelect(newApply("reify", super.transform(tree)), "tree")
    }
  }
}
