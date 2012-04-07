package scalaxy; package components

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.transform.TypingTransformers
import Function.tupled

trait Replacements
extends TypingTransformers
//   with MirrorConversions
{
  this: PluginComponent =>

  //import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._

  //import scala.reflect._
  
  //import scala.reflect.api._
  //import scala.reflect.runtime._
  //import scala.reflect.runtime.Mirror._
  //import scala.reflect.mirror._
  
  case class Bindings(
    nameBindings: Map[global.Name, global.Tree] = Map(), 
    typeBindings: Map[global.Type, global.Type] = Map()
  ) {
    val typeStrBindings = typeBindings map {
      case (from, to) =>
        (from.toString, to)
    }
    def getType(t: global.Type) = {
      if (t == null)
        None
      else
        typeStrBindings.get(t.toString)
    }
    def bindName(n: global.Name, v: global.Tree) =
      copy(nameBindings = nameBindings + (n -> v))
     
    def bindType(t: global.Type, t2: global.Type) =
      copy(typeBindings = typeBindings + (t -> t2))
      
    def ++(b: Bindings) =
      Bindings(nameBindings ++ b.nameBindings, typeBindings ++ b.typeBindings)
  }
  val EmptyBindings = Bindings()
  
  /*def combine(x: Option[Bindings], y: Option[Bindings]): Option[Bindings] = (x, y) match {
    case (None, _) => None
    case (_, None) => None
    case (Some(b1), Some(b2)) => 
      Some(b1 ++ b2)
  }
  */
  def combine(a: Bindings, b: Bindings) = a ++ b
    
  def resolveType(tpe: global.Type): global.Type = 
      /*.map(_.deconst.dealias.normalize)*/
      Option(tpe).map({
        case global.ThisType(sym) =>
          sym.asType
        case global.SingleType(pre, sym) =>
          val t = sym.asType
          //println("Found SINGLE TYPE(" + pre + ", " + sym + ") = " + tpe + " -> " + t)
          if (t != null && t != global.NoType)
            t//normalize(t)
          else
            tpe
        case _ =>
          tpe
      }).orNull//getOrElse(global.NoType)
      
  def matchAndResolveBindings(reps: List[(global.Tree, global.Tree)], depth: Int)(implicit internalDefs: InternalDefs): Bindings = {
    reps.map({ case (a, b) => matchAndResolveBindings(a, b, depth)}).reduceLeft(_ ++ _)
  }
  def matchAndResolveBindings(reps: List[(global.Type, global.Type)])(implicit internalDefs: InternalDefs): Bindings = {
    reps.map({ case (a, b) => matchAndResolveBindings(a, b)}).reduceLeft(_ ++ _)
  }
  
  type InternalDefs = Set[global.Name]
  
  def getNamesDefinedIn(stats: List[global.Tree]): Set[global.Name] =
    stats.collect { case global.ValDef(_, name, _, _) => name: global.Name } toSet
  
  case class NoMatchException(expected: Any, found: Any, msg: String)
  extends RuntimeException(
    msg + 
    " (\n\texpected = " + expected + ": " + Option(expected).map(_.getClass.getName) + 
    ",\n\tfound = " + found + ": " + Option(found).map(_.getClass.getName) + "\n)")
  
  def matchAndResolveBindings(pattern0: global.Type, tree0: global.Type)(implicit internalDefs: InternalDefs): Bindings = {
    import global._
    
    val pattern = resolveType(pattern0)
    val tree = resolveType(tree0)
    
    
    //def normalize(t: Type) = Option(t).map(_.normalize.deconst.dealias)
    
    //def empt(t: global.Type) = 
    //  t == null || t == NoType || t.toString == "package <empty>"
    
    def typeStr(t: Any) = 
      if (t == null) "?" else t.getClass.getName + " <- " + t.getClass.getSuperclass.getName
      
    println("Matching types " + pattern + " (" + typeStr(pattern) + ") vs. " + tree + " (" + typeStr(tree) + ")")
    val ret = (pattern, tree) match {
      //case (_, _) if empt(pattern) && empt(tree) =>
      //  EmptyBindings
        
      //case ((_: UniqueThisType) | (_: UniqueSingleType), (_: UniqueThisType) | (_: UniqueSingleType)) =>
      //  // TODO UGLY HACK for "scala.collection.type" vs. "type"
      //  EmptyBindings
        
      case (RefinedType(parents, decls), RefinedType(parents2, decls2)) =>
        EmptyBindings
        
      case (TypeBounds(lo, hi), TypeBounds(lo2, hi2)) =>
        matchAndResolveBindings(List((lo, lo2), (hi, hi2)))
        
      case (MethodType(paramtypes, result), MethodType(paramtypes2, result2)) =>
        //println("MethodType")
        matchAndResolveBindings(result, result2)
        //matchAndResolveBindings((result, result2):: paramtypes.zip(paramtypes2))
        
      case (NullaryMethodType(result), NullaryMethodType(result2)) =>
        //println("NullaryMethodType")
        matchAndResolveBindings(result, result2)
        
      case (PolyType(tparams, result), PolyType(tparams2, result2)) =>
        //println("PolyType")
        matchAndResolveBindings(result, result2)
        //matchAndResolveBindings((result, result2):: tparams.zip(tparams2))
        
      case (ExistentialType(tparams, result), ExistentialType(tparams2, result2)) =>
        //println("PolyType")
        matchAndResolveBindings(result, result2)
        //matchAndResolveBindings((result, result2):: tparams.zip(tparams2))
      
      case (TypeRef(pre, sym, args), TypeRef(pre2, sym2, args2)) =>
        //matchAndResolveBindings(pre, pre2)
        //println("TypeRef")
        // TODO test sym vs. sym2
        if (args.size != args2.size) {
          throw NoMatchException(pattern, tree, "Different number of args in type ref")
        } else {
          if (args.isEmpty)
            EmptyBindings//Bindings(Map(), Map(pattern -> tree))
          else
            matchAndResolveBindings(args.zip(args2))
        }
      case _ =>
        if (Option(pattern).toString == Option(tree).toString) {
          println("Monkey type matching of " + pattern + " vs. " + tree)
          EmptyBindings
        } else {
          throw NoMatchException(pattern, tree, "Type matching failed")
        }
    }
    
    println("Successfully bound " + pattern + " vs. " + tree)
    ret.bindType(pattern, tree)
  }
  def matchAndResolveBindings(pattern: global.Tree, tree: global.Tree, depth: Int = 0)(implicit internalDefs: InternalDefs = Set()): Bindings = {
    //println("internalDefs = " + internalDefs) 
    //println("matchAndResolveBindings(" + pattern + ", " + tree + ")")
    println("M " + pattern)
    val treesMatching: Bindings = (pattern, tree) match {
      case (_, _) if pattern.isEmpty && tree.isEmpty =>
        EmptyBindings
        
      case (global.This(_), global.This(_)) =>
        EmptyBindings
        
      case (_: global.TypeTree, _: global.TypeTree) =>
        Bindings(Map(), Map(pattern.tpe -> tree.tpe))
        
      case (global.Ident(n), _) =>
        if (internalDefs.contains(n))
          EmptyBindings
        else
          Bindings(Map(n -> tree), Map())
          
      case (global.ValDef(mods, name, tpt, rhs), global.ValDef(mods2, name2, tpt2, rhs2))
      if mods.modifiers == mods2.modifiers =>
        println("TPT1 = " + tpt)
        println("TPT2 = " + tpt2)
        matchAndResolveBindings(List((rhs, rhs2), (tpt, tpt2)), depth + 1)(internalDefs + name).
        bindName(name, global.Ident(name2))
      
      case (global.Function(vparams, body), global.Function(vparams2, body2)) =>
        println("GOT FUNCTION !!!")
        matchAndResolveBindings((body, body2) :: vparams.zip(vparams2), depth + 1)(internalDefs ++ vparams.map(_.name))
        
      case (global.TypeApply(fun, args), global.TypeApply(fun2, args2)) =>
        matchAndResolveBindings((fun, fun2) :: args.zip(args2), depth + 1)
      
      case (global.Apply(a, b), global.Apply(a2, b2)) =>
        matchAndResolveBindings((a, a2) :: b.zip(b2), depth + 1)
        
      case (global.Block(l, v), global.Block(l2, v2)) =>
        matchAndResolveBindings((v, v2) :: l.zip(l2), depth + 1)(internalDefs ++ getNamesDefinedIn(l))
        
      case (global.Select(a, n), global.Select(a2, n2)) if n == n2 =>
          matchAndResolveBindings(a, a2, depth + 1)
      
      //case (ClassDef(mods, name, tparams, impl), ClassDef(mods2, name2, tparams2, impl2)) =
      //  matchAndResolveBindings(impl, impl)(internalDefs + name)
      
      case _ =>
        if (Option(pattern).toString == Option(tree).toString) {
          println("Monkey type matching of " + pattern + " vs. " + tree)
          EmptyBindings
        } else {
          throw NoMatchException(pattern, tree, "Different types")
        }
    }
    
    val tp = resolveType(pattern.tpe)
    val tt = resolveType(tree.tpe)
    
    treesMatching ++ {
      try {
        matchAndResolveBindings(tp, tt)
      } catch { case ex =>
        println("Failed type matching on " + tp + " (" + pattern.tpe + ") vs. " + tt + " (" + tree.tpe + ")")
        EmptyBindings
      }
    }
  } 
  
  def replace(replacement: global.Tree, bindings: Bindings) = {
    new global.Transformer {
      override def transform(tree: global.Tree) = tree match {
        case global.TypeTree() =>
          val opt = bindings.getType(tree.tpe)
          println("Replacement of " + tree + " = " + opt)
          super.transform(opt.map(global.TypeTree(_)).getOrElse(tree))
        case global.Ident(n) =>
          bindings.nameBindings.get(n).getOrElse(tree)
        case _ =>
          super.transform(tree)
      }
    }.transform(replacement)
  }
}
