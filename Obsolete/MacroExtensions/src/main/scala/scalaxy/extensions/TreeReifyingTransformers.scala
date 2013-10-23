// Author: Olivier Chafik (http://ochafik.com)
// Feel free to modify and reuse this for any purpose ("public domain / I don't care").
package scalaxy.extensions

import scala.collection.mutable

//import scala.reflect.api.Universe
import scala.tools.nsc.Global
import scala.reflect.internal.Flags
import scala.reflect.NameTransformer

trait TreeReifyingTransformers extends Extensions
{
  val global: Global
  import global._
  import definitions._
  
  def newConstant(v: Any) = 
    Literal(Constant(v))
    
  def newApplyList(args: List[Tree]): Tree =
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
  
  class TreeReifyingTransformer(
    exprSplicer: TermName => Option[Tree], 
    typeGetter: Tree => Tree,
    typeTreeGetter: Tree => Tree)
      extends Transformer 
  {
    def newTermIdent(n: String): Tree =
      newApply("Ident", transform(newTermName(n)))
      
    def transform(n: Name): Tree = {
      if (n.toString == "T")
        new RuntimeException("TTTT: " + n).printStackTrace()
      newApply(if (n.isTermName) "newTermName" else "newTypeName", newConstant(n.toString))
    }
    
    def transform(constant: Constant): Tree = {
      val Constant(value) = constant
      newApply(
        "Constant",
        newConstant(value))
    }
    
    def transform(mods: Modifiers): Tree = {
      val Modifiers(flags, privateWithin, annotations) = mods
      newApply(
        "Modifiers", 
        transform(flags), 
        transform(privateWithin), 
        transform(annotations))
    }
    
    def transform(trees: List[Tree]): Tree =
      newApplyList(trees.map(transform(_)))
    
    def transforms(treess: List[List[Tree]]): Tree = 
      newApplyList(treess.map(transform(_)))
          
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
        Ident("NoFlags": TermName)
      else
        flagTrees.reduceLeft[Tree]((a, b) => Apply(Select(a, encode("|")), List(b)))
    }
    
    /*
    class Extractors(extractors: (String, Tree => Option[_ <: Product])*) 
    {
      private val extractorsList = extractors.toList
      
      private def unapply(tree: Tree, extractors: List[(String, Tree => Option[_ <: Product])]): Option[Product] = extractors match {
        case Nil => None
        case x :: xs =>
          x.unapplySeq(tree).getOrElse(unapply(tree, xs))
      }
      
      def unapply(tree: Tree): Option[Tree] =
        unapply(tree, extractorsList)
    }
    */
    override def transform(tree: Tree): Tree = {
      /*val xs = new Extractors(
        "Try" -> ((t: Tree) => Try.unapply(t))
      )*/
      tree match {
        case Ident(n: TypeName) =>
          typeTreeGetter(tree)
        case AppliedTypeTree(target, args) =>
          val ttarget = 
            Apply(
              Ident("TypeTree": TermName), 
              List(
                termPath(typeGetter(tree), "typeConstructor")))
          
          newApply(
            "AppliedTypeTree", 
            ttarget, 
            transform(args))
        case ExistentialTypeTree(target, args) =>
          newApply(
            "ExistentialTypeTree",
            transform(target),
            transform(args))
        case _: TypeTree if !tree.isEmpty =>
          typeTreeGetter(super.transform(tree))
        case Ident(n: TermName) =>
          exprSplicer(n).getOrElse {
            newApply(
              "Ident", 
              transform(n))
          }
        case Select(target, n) =>
          newApply(
            "Select", 
            transform(target), 
            newConstant(n.toString))
        case Block(statements, value) =>
          newApply(
            "Block", 
            (statements :+ value).map(transform(_)): _*)
        case _: TypeTree if tree.isEmpty =>
          newApply(
            "TypeTree",
            newConstant(null))
        case _ if tree.isEmpty =>
          Ident("EmptyTree": TermName)
        
          
        case Apply(target, args) =>
          newApply(
            "Apply", 
            transform(target),
            transform(args))
        case TypeApply(target, args) =>
          newApply(
            "TypeApply", 
            transform(target),
            transform(args))
        case New(tpt) =>
          newApply(
            "New",
            transform(tpt))
        case Typed(a, b) =>
          newApply(
            "Typed", 
            transform(a), 
            transform(b))
        case If(cond, a, b) =>
          newApply(
            "If", 
            transform(cond), 
            transform(a), 
            transform(b))
        case ValDef(mods, name, tpt, rhs) =>
          newApply(
            "ValDef", 
            transform(mods), 
            transform(name), 
            transform(tpt), 
            transform(rhs))
        case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
          newApply(
            "DefDef", 
            transform(mods), 
            transform(name), 
            transform(tparams),
            transforms(vparamss),
            transform(tpt), 
            transform(rhs))
        case ClassDef(mods, name, tparams, impl) =>
          newApply(
            "ClassDef", 
            transform(mods), 
            transform(name), 
            transform(tparams),
            transform(impl))
        case ModuleDef(mods, name, impl) =>
          newApply(
            "ModuleDef", 
            transform(mods), 
            transform(name), 
            transform(impl))
        case Template(parents, self, body) =>
          newApply(
            "Template", 
            transform(parents),
            transform(self),
            transform(body))
        case Function(params, body) =>
          newApply(
            "Function", 
            transform(params), 
            transform(body))
        case LabelDef(name, params, rhs) =>
          newApply(
            "LabelDef", 
            transform(name), 
            transform(params), 
            transform(rhs))
        case Literal(value) =>
          newApply(
            "Literal",
            transform(value))
        case Try(block, catches, finalizer) =>
          newApply(
            "Try", 
            transform(block), 
            transform(catches), 
            transform(finalizer))
        case CaseDef(pat, guard, body) =>
          newApply(
            "CaseDef",
            transform(pat),
            transform(guard),
            transform(body))
        case Bind(name, body) =>
          newApply(
            "Bind",
            transform(name),
            transform(body))
        case Match(selector, cases) =>
          newApply(
            "Match",
            transform(selector),
            transform(cases))
        case null => 
          null
        case _ =>
          // TODO ValDef, DefDef, ClassDef, Template, Function...
          println("TODO reify properly " + tree.getClass.getName + " <- " + tree.getClass.getSuperclass.getName + ": " + tree)
          newSelect(newApply("reify", super.transform(tree)), "tree")
      }
    }
  }
}
