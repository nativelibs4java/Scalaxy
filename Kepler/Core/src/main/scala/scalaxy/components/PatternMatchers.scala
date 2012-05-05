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
  //import global.definitions._
  import scala.tools.nsc.symtab.Flags._

  import scala.reflect._
  
  //import scala.reflect.api._
  //import scala.reflect.runtime._
  //import scala.reflect.runtime.Mirror._
  //import scala.reflect.mirror._
  
  val patternUniverse: api.Universe 
  val candidateUniverse: api.Universe with scala.reflect.internal.Importers
  
  case class Bindings(
    nameBindings: Map[String, candidateUniverse.Tree] = Map(), 
    typeBindings: Map[patternUniverse.Type, candidateUniverse.Type] = Map()
  ) {
    def getType(t: patternUniverse.Type): Option[candidateUniverse.Type] =
      Option(t).flatMap(typeBindings.get(_))
    
    def bindName(n: patternUniverse.Name, v: candidateUniverse.Tree) =
      copy(nameBindings = nameBindings + (n.toString -> v))
     
    def bindType(t: patternUniverse.Type, t2: candidateUniverse.Type) =
      copy(typeBindings = typeBindings + (t -> t2))
      
    def ++(b: Bindings) =
      Bindings(
        nameBindings ++ b.nameBindings, typeBindings ++ b.typeBindings
      )
      
    def apply(replacement: patternUniverse.Tree): candidateUniverse.Tree = 
    {
      //val toto = candidateUniverse.asInstanceOf[scala.reflect.internal.Importers]
      val importer = new candidateUniverse.StandardImporter {
        val from = patternUniverse.asInstanceOf[scala.reflect.internal.SymbolTable]
        override def importTree(tree: from.Tree): candidateUniverse.Tree = {
          tree match {
            case from.Ident(n) =>
              nameBindings.get(n.toString).
              getOrElse(super.importTree(tree)).
              asInstanceOf[candidateUniverse.Tree]
              //{ val imp = candidateUniverse.Ident(in) ; imp.tpe = importType(tree.tpe) ; imp })
            case _ =>
              super.importTree(tree)
          }
        }
        /*
        override def importType(tpe: from.Type): candidateUniverse.Type = {
          if (tpe == null) {
            null
          } else {
            //val it = resolveType(candidateUniverse)(super.importType(tpe)).asInstanceOf[candidateUniverse.Type]
            //getType(it.asInstanceOf[patternUniverse.Type]).getOrElse(it).asInstanceOf[candidateUniverse.Type]
            
            //var it = super.importType(tpe)
            //it = resolveType(candidateUniverse)(it)
            getType(resolveType(patternUniverse)(tpe.asInstanceOf[patternUniverse.Type])).
            getOrElse(super.importType(tpe))
            //.asInstanceOf[candidateUniverse.Type]
          }
        }*/
      }
      importer.importTree(replacement.asInstanceOf[importer.from.Tree])
    }
  }
  
  def combine(a: Bindings, b: Bindings) = a ++ b
    
  def normalize(u: api.Universe)(tpe: u.Type) = 
    //tpe.dealias.deconst.normalize
    tpe
    
  def resolveType(u: api.Universe)(tpe: u.Type): u.Type = 
      Option(tpe).map(normalize(u)(_)).map({
        case u.ThisType(sym) =>
          sym.asType
        case tt @ u.SingleType(pre, sym) =>
          val t = sym.asType
          if (t != null && t != candidateUniverse.NoType)
            t
          else
            tt
        case tt =>
          tt
      }).orNull
      
  def matchAndResolveTreeBindings(reps: List[(patternUniverse.Tree, candidateUniverse.Tree)], depth: Int)(implicit internalDefs: InternalDefs): Bindings = 
  {
    reps.map({ case (a, b) => matchAndResolveTreeBindings(a, b, depth)}).reduceLeft(_ ++ _)
  }
  def matchAndResolveTypeBindings(reps: List[(patternUniverse.Type, candidateUniverse.Type)])(implicit internalDefs: InternalDefs): Bindings = 
  {
    reps.map({ case (a, b) => matchAndResolveTypeBindings(a, b)}).reduceLeft(_ ++ _)
  }
  
  type InternalDefs = Set[patternUniverse.Name]
  
  def getNamesDefinedIn(u: api.Universe)(stats: List[u.Tree]): Set[u.Name] =
    stats.collect { case u.ValDef(_, name, _, _) => name: u.Name } toSet
  
  case class NoTreeMatchException(expected: Any, found: Any, msg: String, depth: Int)
  extends RuntimeException(msg)
    
  case class NoTypeMatchException(expected: Any, found: Any, msg: String)
  extends RuntimeException(msg)
    
  def isNoType(u: api.Universe)(t: u.Type) =
    t == null || t == u.NoType || t == u.definitions.UnitClass.asType || {
      val s = t.toString
      s == "<notype>" || s == "scala.this.Unit"
    }
    
  
  def matchAndResolveTypeBindings(pattern0: patternUniverse.Type, tree0: candidateUniverse.Type)(implicit internalDefs: InternalDefs = Set()): Bindings = 
  {
    import candidateUniverse._
    
    lazy val EmptyBindings = Bindings()
  
    val pattern = resolveType(patternUniverse)(pattern0)
    val tree = resolveType(candidateUniverse)(tree0)
    
    if (pattern != null && pattern == tree) {
      EmptyBindings
    } else {
      if (pattern != null && tree != null)
        if (pattern.typeSymbol.isPackage || tree.typeSymbol.isPackage)
          if (!pattern.typeSymbol.isPackage || !tree.typeSymbol.isPackage)
            throw new NoTypeMatchException(pattern0, tree0, "Package vs. non-package types")
      
      val ret = (pattern, tree) match {
        case (_, _) if isNoType(patternUniverse)(pattern) && isNoType(candidateUniverse)(tree) =>
          EmptyBindings
          
        case (patternUniverse.RefinedType(parents, decls), RefinedType(parents2, decls2)) =>
          EmptyBindings
          
        case (patternUniverse.TypeBounds(lo, hi), TypeBounds(lo2, hi2)) =>
          matchAndResolveTypeBindings(List((lo, lo2), (hi, hi2)))
          
        case (patternUniverse.MethodType(paramtypes, result), MethodType(paramtypes2, result2)) =>
          matchAndResolveTypeBindings(result, result2)
          // TODO matchAndResolveTypeBindings((result, result2):: paramtypes.zip(paramtypes2))
          
        case (patternUniverse.NullaryMethodType(result), NullaryMethodType(result2)) =>
          matchAndResolveTypeBindings(result, result2)
          
        case (patternUniverse.PolyType(tparams, result), PolyType(tparams2, result2)) =>
          matchAndResolveTypeBindings(result, result2)
          // TODO matchAndResolveTypeBindings((result, result2):: tparams.zip(tparams2))
          
        case (patternUniverse.ExistentialType(tparams, result), ExistentialType(tparams2, result2)) =>
          matchAndResolveTypeBindings(result, result2)
          // TODO matchAndResolveTypeBindings((result, result2):: tparams.zip(tparams2))
        
        case (patternUniverse.TypeRef(pre, sym, args), TypeRef(pre2, sym2, args2)) =>
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
  def matchAndResolveTreeBindings(pattern: patternUniverse.Tree, tree: candidateUniverse.Tree, depth: Int = 0)(implicit internalDefs: InternalDefs = Set()): Bindings = 
  {
    //if (depth > 0)
    //println("Going down : " + candidateUniverse.nodeToString(pattern) + " vs. " + candidateUniverse.nodeToString(tree))
    lazy val EmptyBindings = Bindings()
  
    (pattern, tree) match {
      case (_, _) if pattern.isEmpty && tree.isEmpty =>
        EmptyBindings
        
      case (patternUniverse.This(_), candidateUniverse.This(_)) =>
        EmptyBindings
        
      case (patternUniverse.Literal(patternUniverse.Constant(a)), candidateUniverse.Literal(candidateUniverse.Constant(a2))) if a == a2 =>
      //case (candidateUniverse.Literal(c), candidateUniverse.Literal(c2)) =>
        // if a == a2 =>
        //println("FOUND LITERALS c = " + c + ", c2 = " + c2 + ": " + c2.tpe) 
        EmptyBindings
        
      case (_: patternUniverse.TypeTree, _: candidateUniverse.TypeTree) =>
        Bindings(Map(), Map(pattern.tpe -> tree.tpe))
        
      case (patternUniverse.Ident(n), _) =>
        if (internalDefs.contains(n))
          EmptyBindings
        else /*tree match {
          case candidateUniverse.Ident(nn) if n.toString == nn.toString =>
            EmptyBindings
          case _ =>*/
            //println("GOT BINDING " + pattern + " -> " + tree + " (tree is " + tree.getClass.getName + ")")
            Bindings(Map(n.toString -> tree), Map())
        //}
          
      case (patternUniverse.ValDef(mods, name, tpt, rhs), candidateUniverse.ValDef(mods2, name2, tpt2, rhs2))
      if mods.modifiers == mods2.modifiers =>
        val r = matchAndResolveTreeBindings(
          List((rhs, rhs2), (tpt, tpt2)), depth + 1
        )(
          internalDefs + name
        )
          
        if (name == name2)
          r
        else
          r.bindName(name, candidateUniverse.Ident(name2))
      
      case (patternUniverse.Function(vparams, body), candidateUniverse.Function(vparams2, body2)) =>
        matchAndResolveTreeBindings(
          (body, body2) :: vparams.zip(vparams2), depth + 1
        )(
          internalDefs ++ vparams.map(_.name)
        )
        
      case (patternUniverse.TypeApply(fun, args), candidateUniverse.TypeApply(fun2, args2)) =>
        matchAndResolveTreeBindings(
          (fun, fun2) :: args.zip(args2), depth + 1
        )
      
      case (patternUniverse.Apply(a, b), candidateUniverse.Apply(a2, b2)) =>
        matchAndResolveTreeBindings(
          (a, a2) :: b.zip(b2), depth + 1
        )
        
      case (patternUniverse.Block(l, v), candidateUniverse.Block(l2, v2)) =>
        matchAndResolveTreeBindings(
          (v, v2) :: l.zip(l2), depth + 1
        )(
          internalDefs ++ getNamesDefinedIn(patternUniverse)(l)
        )
        
      case (patternUniverse.Select(a, n), candidateUniverse.Select(a2, n2)) if n == n2 =>
        //println("Matched select " + a + " vs. " + a2)
          matchAndResolveTreeBindings(
            a, a2, depth + 1
          )
      
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
  
  def getOrFixType(u: api.Universe)(tree: u.Tree) = {
    import u.definitions._
    val t = tree.tpe
    if (t == null)
      tree match {
        case u.Literal(u.Constant(v)) =>
          v match {
            case _: Int => IntClass.asType
            case _: Short => ShortClass.asType
            case _: Long => LongClass.asType
            case _: Byte => ByteClass.asType
            case _: Double => DoubleClass.asType
            case _: Float => FloatClass.asType
            case _: Char => CharClass.asType
            case _: Boolean => BooleanClass.asType
            case _: String => StringClass.asType
            case _: Unit => UnitClass.asType
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
  def matchAndResolveBindings(pattern: patternUniverse.Tree, tree: candidateUniverse.Tree): Bindings = 
  {
    val typeBindings = matchAndResolveTypeBindings(
      getOrFixType(patternUniverse)(pattern), getOrFixType(candidateUniverse)(tree)
    )
      
    val treeBindings = matchAndResolveTreeBindings(
      pattern, tree
    )
    
    typeBindings ++ treeBindings
  } 
}
