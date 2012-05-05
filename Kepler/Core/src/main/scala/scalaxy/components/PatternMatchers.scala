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

  import scala.tools.nsc.symtab.Flags._

  import scala.reflect._
  
  // TODO turn to false
  val workAroundMissingTypeApply = false 
  
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
    
  implicit def t2pt(tp: api.Types#Type) = tp.asInstanceOf[PlasticType]
  type PlasticType = {
    def dealias: api.Types#Type
    def deconst: api.Types#Type
    def normalize: api.Types#Type
    //def widen: WidenableType
  }
  
  implicit def s2ps(tp: api.Symbols#Symbol) = tp.asInstanceOf[PlasticSymbol]
  type PlasticSymbol = {
    def isTypeParameter: Boolean
  }
  
  def normalize(u: api.Universe)(tpe: u.Type) = 
    
    tpe.asInstanceOf[PlasticType].deconst.dealias.normalize.asInstanceOf[u.Type]
    //tpe.dealias.deconst.normalize
    //tpe
    
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
    if (reps.isEmpty)
      EmptyBindings
    else
      reps.map({ case (a, b) => 
        matchAndResolveTreeBindings(a, b, depth)
      }).reduceLeft(_ ++ _)
  }
  def matchAndResolveTypeBindings(reps: List[(patternUniverse.Type, candidateUniverse.Type)], depth: Int)(implicit internalDefs: InternalDefs): Bindings = 
  {
    if (reps.isEmpty)
      EmptyBindings
    else
      reps.map({ case (a, b) => 
        matchAndResolveTypeBindings(a, b, depth)
      }).reduceLeft(_ ++ _)
  }
  
  type InternalDefs = Set[patternUniverse.Name]
  
  def getNamesDefinedIn(u: api.Universe)(stats: List[u.Tree]): Set[u.Name] =
    stats.collect { case u.ValDef(_, name, _, _) => name: u.Name } toSet
  
  case class NoTreeMatchException(expected: Any, found: Any, msg: String, depth: Int)
  extends RuntimeException(msg)
    
  case class NoTypeMatchException(expected: Any, found: Any, msg: String, depth: Int)
  extends RuntimeException(msg)
    
  def isNoType(u: api.Universe)(t: u.Type) =
    t == null || t == u.NoType || t == u.definitions.UnitClass.asType || {
      val s = t.toString
      s == "<notype>" || s == "scala.this.Unit"
    }
    
  def clstr(v: AnyRef) = 
    v.getClass.getName + " <- " + v.getClass.getSuperclass.getName
    
  def types(u: api.Universe)(syms: List[u.Symbol]) = 
    syms.map(_.asType)
          
  def zipTypes(syms1: List[patternUniverse.Symbol], syms2: List[candidateUniverse.Symbol]) = 
    types(patternUniverse)(syms1).zip(types(candidateUniverse)(syms2))
          
  def matchAndResolveTypeBindings(pattern0: patternUniverse.Type, tree0: candidateUniverse.Type, depth: Int = 0)(implicit internalDefs: InternalDefs = Set()): Bindings = 
  {
    import candidateUniverse._
    
    lazy val EmptyBindings = Bindings()
  
    val pattern = resolveType(patternUniverse)(pattern0)
    val tree = resolveType(candidateUniverse)(tree0)
    
    //println("Going down in types (depth " + depth + "):")
    //println("\ttype pattern = " + pattern + ": " + clstr(pattern))
    //println("\ttype found = " + tree + ": " + clstr(tree))
    
    if (pattern != null && pattern == tree) {
      EmptyBindings
    } else {
      if (pattern != null && tree != null)
        if (pattern.typeSymbol.isPackage || tree.typeSymbol.isPackage)
          if (!pattern.typeSymbol.isPackage || !tree.typeSymbol.isPackage)
            throw new NoTypeMatchException(pattern0, tree0, "Package vs. non-package types", depth)
      
      val patNoType = isNoType(patternUniverse)(pattern)
      val candNoType = isNoType(candidateUniverse)(tree)
      
      if (patNoType != candNoType)
        throw NoTypeMatchException(pattern0, tree0, "Type matching failed", depth)
      
      val ret = (pattern, tree) match {
        case (_, _) if patNoType && candNoType =>
          EmptyBindings
          
        case (patternUniverse.RefinedType(parents, decls), RefinedType(parents2, decls2)) =>
          EmptyBindings
          
        case (patternUniverse.TypeBounds(lo, hi), TypeBounds(lo2, hi2)) =>
          matchAndResolveTypeBindings(List((lo, lo2), (hi, hi2)), depth + 1)
          
        case (patternUniverse.MethodType(paramtypes, result), MethodType(paramtypes2, result2)) =>
          matchAndResolveTypeBindings((result, result2) :: zipTypes(paramtypes, paramtypes2), depth + 1)
          
        case (patternUniverse.NullaryMethodType(result), NullaryMethodType(result2)) =>
          matchAndResolveTypeBindings(result, result2, depth + 1)
          
        case (patternUniverse.PolyType(tparams, result), PolyType(tparams2, result2)) =>
          matchAndResolveTypeBindings((result, result2):: zipTypes(tparams, tparams2), depth + 1)
          
        case (patternUniverse.ExistentialType(tparams, result), ExistentialType(tparams2, result2)) =>
          matchAndResolveTypeBindings((result, result2) :: zipTypes(tparams, tparams2), depth + 1)
        
        /*
        case (patternUniverse.TypeRef(pre, sym, args), TypeRef(pre2, sym2, args2)) 
        if args.size == args2.size &&
           pattern.kind == tree.kind && 
           sym.kind == sym2.kind
        =>
          //if (pattern.kind != tree.kind)
          //  throw NoTypeMatchException(pattern0, tree0, "Different type kinds : " + pattern.kind + " vs. " + tree.kind, depth)
          println("pattern.sym = " + sym + " (" + sym.kind + "), tree.sym = " + sym2 + " (" + sym2.kind + ")")
          println("pattern.pre = " + pre + " (" + pre.kind + "), tree.pre = " + pre2 + " (" + pre2.kind + ")")
          //matchAndResolveTypeBindings(sym.asType, sym2.asType, depth + 1) ++ 
          matchAndResolveTypeBindings(pre, pre2, depth + 1) ++
          matchAndResolveTypeBindings(args.zip(args2), depth + 1)
        */
        
        case (_, _) if pattern.typeSymbol.isTypeParameter =>
          Bindings(Map(), Map(pattern -> tree))
          
        case _ =>
          if (Option(pattern).toString == Option(tree).toString) {
            println("WARNING: Monkey type matching of " + pattern + " vs. " + tree)
            EmptyBindings
          } else {
            if (depth > 0)
              println("TYPE MISMATCH \n\texpected = " + pattern0 + "\n\t\t" + pattern0.getClass.getName + "\n\tfound = " + tree0 + "\n\t\t" + tree0.getClass.getName)
            throw NoTypeMatchException(pattern0, tree0, "Type matching failed", depth)
          }
      }
      
      //println("Successfully bound " + pattern + " vs. " + tree)
      //if (pattern != null && tree != null)
      //  ret.bindType(pattern, tree)
      //else
        ret
    }
  }
  
  val EmptyBindings = Bindings()
  
  def matchAndResolveTreeBindings(pattern: patternUniverse.Tree, tree: candidateUniverse.Tree, depth: Int = 0)(implicit internalDefs: InternalDefs = Set()): Bindings = 
  {
    val patternType = getOrFixType(patternUniverse)(pattern)
    val candidateType = getOrFixType(candidateUniverse)(tree)
    
    //if (depth > 0)
    //
    val typeBindings = matchAndResolveTypeBindings(patternType, candidateType)
    //println("Going down in trees (depth " + depth + "):")
    //println("\tpattern = " + pattern + ": " + patternType + " (" + pattern.getClass.getName + ", " + clstr(patternType) + ")")
    //println("\tfound = " + tree + ": " + candidateType + " (" + tree.getClass.getName + ", " + clstr(candidateType) + ")")
    //println("\ttypeBindings = " + typeBindings)
    
    typeBindings ++ ( 
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
          //println("# Type Trees :")
          //println("# \texpected = " + pattern.tpe + ": " + pattern.tpe.getClass.getName)
          //println("# \tfound = " + tree.tpe + ": " + tree.tpe.getClass.getName)
          EmptyBindings // already handled by matchAndResolveTypeBindings
          //Bindings(Map(), Map(pattern.tpe -> tree.tpe))
          
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
          
        case (patternUniverse.Select(a, n), candidateUniverse.Select(a2, n2)) if n.toString == n2.toString =>
          //println("Matched select " + a + " vs. " + a2)
            matchAndResolveTreeBindings(
              a, a2, depth + 1
            )
        
        // TODO
        //case (ClassDef(mods, name, tparams, impl), ClassDef(mods2, name2, tparams2, impl2)) =
        //  matchAndResolveTreeBindings(impl, impl)(internalDefs + name)
        
        case (_, candidateUniverse.TypeApply(target, typeArgs)) if workAroundMissingTypeApply =>
          println("Workaround for missing TypeApply in pattern... (loosing types " + typeArgs + ")")
          
          matchAndResolveTreeBindings(pattern, target, depth + 1)
        case _ =>
          if (Option(pattern).toString == Option(tree).toString) {
            println("WARNING: Monkey matching of " + pattern + " vs. " + tree)
            EmptyBindings
          } else {
            if (depth > 0)
                println("TREE MISMATCH \n\texpected = " + pattern + "\n\t\t" + pattern.getClass.getName + "\n\tfound = " + tree + "\n\t\t" + tree.getClass.getName)
            throw NoTreeMatchException(pattern, tree, "Different trees", depth)
          }
      }
    )
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
    /*val typeBindings = matchAndResolveTypeBindings(
      getOrFixType(patternUniverse)(pattern), getOrFixType(candidateUniverse)(tree)
    )
      
    val treeBindings = matchAndResolveTreeBindings(
      pattern, tree
    )
    
    typeBindings ++ treeBindings*/
    matchAndResolveTreeBindings(
      pattern, tree
    )
  } 
}
