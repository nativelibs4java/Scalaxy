package scalaxy; package components

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.transform.TypingTransformers
import Function.tupled

trait PatternMatchers
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
    def getType(t: global.Type) =
      Option(t).flatMap(typeBindings.get(_))
    
    def bindName(n: global.Name, v: global.Tree) =
      copy(nameBindings = nameBindings + (n -> v))
     
    def bindType(t: global.Type, t2: global.Type) =
      copy(typeBindings = typeBindings + (t -> t2))
      
    def ++(b: Bindings) =
      Bindings(nameBindings ++ b.nameBindings, typeBindings ++ b.typeBindings)
  }
  val EmptyBindings = Bindings()
  
  def combine(a: Bindings, b: Bindings) = a ++ b
    
  def resolveType(tpe: global.Type): global.Type = {
    val res =
      Option(tpe).map(_.dealias.deconst.normalize).map({
        case global.ThisType(sym) =>
          sym.asType
        case tt @ global.SingleType(pre, sym) =>
          val t = sym.asType
          if (t != null && t != global.NoType)
            t
          else
            tt
        case tt =>
          tt
      }).orNull
    //println("resolveType(" + tpe + ") = " + res)
    res
  }
      
  def matchAndResolveTreeBindings(reps: List[(global.Tree, global.Tree)], depth: Int)(implicit internalDefs: InternalDefs): Bindings = {
    reps.map({ case (a, b) => matchAndResolveTreeBindings(a, b, depth)}).reduceLeft(_ ++ _)
  }
  def matchAndResolveTypeBindings(reps: List[(global.Type, global.Type)])(implicit internalDefs: InternalDefs): Bindings = {
    reps.map({ case (a, b) => matchAndResolveTypeBindings(a, b)}).reduceLeft(_ ++ _)
  }
  
  type InternalDefs = Set[global.Name]
  
  def getNamesDefinedIn(stats: List[global.Tree]): Set[global.Name] =
    stats.collect { case global.ValDef(_, name, _, _) => name: global.Name } toSet
  
  case class NoTreeMatchException(expected: Any, found: Any, msg: String, depth: Int)
  extends RuntimeException(msg)
    
  case class NoTypeMatchException(expected: Any, found: Any, msg: String)
  extends RuntimeException(msg)
    
  def matchAndResolveTypeBindings(pattern0: global.Type, tree0: global.Type)(implicit internalDefs: InternalDefs = Set()): Bindings = {
    import global._
    
    val pattern = resolveType(pattern0)
    val tree = resolveType(tree0)
    
    if (pattern != null && pattern == tree) {
      EmptyBindings
    } else {
      if (pattern != null && tree != null)
        if (pattern.typeSymbol.isPackage || tree.typeSymbol.isPackage)
          if (!pattern.typeSymbol.isPackage || !tree.typeSymbol.isPackage)
            throw new NoTypeMatchException(pattern0, tree0, "Package vs. non-package types")
      
      def isNoType(t: Type) =
        t == null || t == NoType || t == UnitClass.tpe || {
          val s = t.toString
          s == "<notype>" || s == "scala.this.Unit"
        }
        
      val ret = (pattern, tree) match {
        case (_, _) if isNoType(pattern) && isNoType(tree) =>
          EmptyBindings
          
        case (RefinedType(parents, decls), RefinedType(parents2, decls2)) =>
          EmptyBindings
          
        case (TypeBounds(lo, hi), TypeBounds(lo2, hi2)) =>
          matchAndResolveTypeBindings(List((lo, lo2), (hi, hi2)))
          
        case (MethodType(paramtypes, result), MethodType(paramtypes2, result2)) =>
          matchAndResolveTypeBindings(result, result2)
          // TODO matchAndResolveTypeBindings((result, result2):: paramtypes.zip(paramtypes2))
          
        case (NullaryMethodType(result), NullaryMethodType(result2)) =>
          matchAndResolveTypeBindings(result, result2)
          
        case (PolyType(tparams, result), PolyType(tparams2, result2)) =>
          matchAndResolveTypeBindings(result, result2)
          // TODO matchAndResolveTypeBindings((result, result2):: tparams.zip(tparams2))
          
        case (ExistentialType(tparams, result), ExistentialType(tparams2, result2)) =>
          matchAndResolveTypeBindings(result, result2)
          // TODO matchAndResolveTypeBindings((result, result2):: tparams.zip(tparams2))
        
        case (TypeRef(pre, sym, args), TypeRef(pre2, sym2, args2)) =>
          // TODO test sym vs. sym2, pre vs. pre2
          if (args.size != args2.size) {
            throw NoTypeMatchException(pattern0, tree0, "Different number of args in type ref")
          } else {
            if (args.isEmpty)
              EmptyBindings//Bindings(Map(), Map(pattern -> tree))
            else
              matchAndResolveTypeBindings(args.zip(args2))
          }
        case _ =>
          if (Option(pattern).toString == Option(tree).toString) {
            println("WARNING: Monkey type matching of " + pattern + " vs. " + tree)
            EmptyBindings
          } else {
            throw NoTypeMatchException(pattern0, tree0, "Type matching failed")
          }
      }
      
      //println("Successfully bound " + pattern + " vs. " + tree)
      if (pattern != null && tree != null)
        ret.bindType(pattern, tree)
      else
        ret
    }
  }
  def matchAndResolveTreeBindings(pattern: global.Tree, tree: global.Tree, depth: Int = 0)(implicit internalDefs: InternalDefs = Set()): Bindings = {
    //if (depth > 1)
    //  println("Going down : " + global.nodeToString(pattern) + ": " + pattern.getClass.getName + " vs. " + global.nodeToString(tree) + ": " + tree.getClass.getName)
    
    /*
    def normalize(t: global.Tree) = t match {
      case global.TypeApply(tt, List(tpt @ global.TypeTree())) if tpt.tpe == UnitClass.tpe => tt
      case _ => t                        
    }
    
    (normalize(pattern), normalize(tree)) match {
    */
    
    (pattern, tree) match {
      case (_, _) if pattern.isEmpty && tree.isEmpty =>
        EmptyBindings
        
      case (global.This(_), global.This(_)) =>
        EmptyBindings
        
      case (global.Literal(global.Constant(a)), global.Literal(global.Constant(a2))) if a == a2 =>
      //case (global.Literal(c), global.Literal(c2)) =>
        // if a == a2 =>
        //println("FOUND LITERALS c = " + c + ", c2 = " + c2 + ": " + c2.tpe) 
        EmptyBindings
        
      case (_: global.TypeTree, _: global.TypeTree) =>
        Bindings(Map(), Map(pattern.tpe -> tree.tpe))
        
      case (global.Ident(n), _) =>
        if (internalDefs.contains(n))
          EmptyBindings
        else /*tree match {
          case global.Ident(nn) if n.toString == nn.toString =>
            EmptyBindings
          case _ =>*/
            //println("GOT BINDING " + pattern + " -> " + tree + " (tree is " + tree.getClass.getName + ")")
            Bindings(Map(n -> tree), Map())
        //}
          
      case (global.ValDef(mods, name, tpt, rhs), global.ValDef(mods2, name2, tpt2, rhs2))
      if mods.modifiers == mods2.modifiers =>
        val r = matchAndResolveTreeBindings(List((rhs, rhs2), (tpt, tpt2)), depth + 1)(internalDefs + name)
        if (name == name2)
          r
        else
          r.bindName(name, global.Ident(name2))
      
      case (global.Function(vparams, body), global.Function(vparams2, body2)) =>
        matchAndResolveTreeBindings((body, body2) :: vparams.zip(vparams2), depth + 1)(internalDefs ++ vparams.map(_.name))
        
      case (global.TypeApply(fun, args), global.TypeApply(fun2, args2)) =>
        matchAndResolveTreeBindings((fun, fun2) :: args.zip(args2), depth + 1)
      
      case (global.Apply(a, b), global.Apply(a2, b2)) =>
        matchAndResolveTreeBindings((a, a2) :: b.zip(b2), depth + 1)
        
      case (global.Block(l, v), global.Block(l2, v2)) =>
        matchAndResolveTreeBindings((v, v2) :: l.zip(l2), depth + 1)(internalDefs ++ getNamesDefinedIn(l))
        
      case (global.Select(a, n), global.Select(a2, n2)) if n == n2 =>
        //println("Matched select " + a + " vs. " + a2)
          matchAndResolveTreeBindings(a, a2, depth + 1)
      
      // TODO
      //case (ClassDef(mods, name, tparams, impl), ClassDef(mods2, name2, tparams2, impl2)) =
      //  matchAndResolveTreeBindings(impl, impl)(internalDefs + name)
      
      case _ =>
        if (Option(pattern).toString == Option(tree).toString) {
          println("WARNING: Monkey matching of " + pattern + " vs. " + tree)
          EmptyBindings
        } else {
          throw NoTreeMatchException(pattern, tree, "Different trees", depth)
        }
    }
  }
  
  def getOrFixType(tree: global.Tree) = {
    val t = tree.tpe
    if (t == null)
      tree match {
        case global.Literal(global.Constant(v)) =>
          v match {
            case _: Int => IntClass.tpe
            case _: Short => ShortClass.tpe
            case _: Long => LongClass.tpe
            case _: Byte => ByteClass.tpe
            case _: Double => DoubleClass.tpe
            case _: Float => FloatClass.tpe
            case _: Char => CharClass.tpe
            case _: Boolean => BooleanClass.tpe
            case _: String => StringClass.tpe
            case _: Unit => UnitClass.tpe
            case _ =>
              null
          }
        case _ =>
          null
      }
    else
      t
  }
  
  // Throws lots of exceptions : NoTreeMatchException and NoTypeMatchException
  def matchAndResolveBindings(pattern: global.Tree, tree: global.Tree): Bindings = {
    val typeBindings = matchAndResolveTypeBindings(getOrFixType(pattern), getOrFixType(tree))
    val treeBindings = matchAndResolveTreeBindings(pattern, tree)
    
    typeBindings ++ treeBindings
  } 
}
