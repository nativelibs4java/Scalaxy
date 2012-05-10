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
  
  // TODO turn to false once macro type is fixed !
  val workAroundMissingTypeApply = true 
  
  val patternUniv: api.Universe 
  val candidateUniv: api.Universe with scala.reflect.internal.Importers
  
  case class Bindings(
    nameBindings: Map[String, candidateUniv.Tree] = Map(), 
    typeBindings: Map[patternUniv.Type, candidateUniv.Type] = Map()
  ) {
    def getType(t: patternUniv.Type): Option[candidateUniv.Type] =
      Option(t).flatMap(typeBindings.get(_))
    
    def bindName(n: patternUniv.Name, v: candidateUniv.Tree) =
      copy(nameBindings = nameBindings + (n.toString -> v))
     
    def bindType(t: patternUniv.Type, t2: candidateUniv.Type) =
      copy(typeBindings = typeBindings + (t -> t2))
      
    def ++(b: Bindings) =
      Bindings(
        nameBindings ++ b.nameBindings, typeBindings ++ b.typeBindings
      )
      
    def apply(replacement: patternUniv.Tree): candidateUniv.Tree = 
    {
      //val toto = candidateUniv.asInstanceOf[scala.reflect.internal.Importers]
      val importer = new candidateUniv.StandardImporter {
        val from = patternUniv.asInstanceOf[scala.reflect.internal.SymbolTable]
        override def importTree(tree: from.Tree): candidateUniv.Tree = {
          tree match {
            case from.Ident(n) =>
              nameBindings.get(n.toString).
              getOrElse(super.importTree(tree)).
              asInstanceOf[candidateUniv.Tree]
              //{ val imp = candidateUniv.Ident(in) ; imp.tpe = importType(tree.tpe) ; imp })
            case _ =>
              super.importTree(tree)
          }
        }
        /*
        override def importType(tpe: from.Type): candidateUniv.Type = {
          if (tpe == null) {
            null
          } else {
            //val it = resolveType(candidateUniv)(super.importType(tpe)).asInstanceOf[candidateUniv.Type]
            //getType(it.asInstanceOf[patternUniv.Type]).getOrElse(it).asInstanceOf[candidateUniv.Type]
            
            //var it = super.importType(tpe)
            //it = resolveType(candidateUniv)(it)
            getType(resolveType(patternUniv)(tpe.asInstanceOf[patternUniv.Type])).
            getOrElse(super.importType(tpe))
            //.asInstanceOf[candidateUniv.Type]
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
  
  def normalize(u: api.Universe)(tpe: u.Type) = {
    //tpe.dealias.deconst.normalize
    tpe.
    deconst.
    dealias.
    normalize.
    asInstanceOf[u.Type]
  }
    
  def resolveType(u: api.Universe)(tpe: u.Type): u.Type = 
      Option(tpe).map(normalize(u)(_)).map({
        case u.ThisType(sym) =>
          sym.asType
        case tt @ u.SingleType(pre, sym) =>
          val t = sym.asType
          if (t != null && t != candidateUniv.NoType)
            t
          else
            tt
        case tt =>
          tt
      }).orNull
      
  def matchAndResolveTreeBindings(reps: List[(patternUniv.Tree, candidateUniv.Tree)], depth: Int)(implicit internalDefs: InternalDefs): Bindings = 
  {
    if (reps.isEmpty)
      EmptyBindings
    else
      reps.map({ case (a, b) => 
        matchAndResolveTreeBindings(a, b, depth)
      }).reduceLeft(_ ++ _)
  }
  def matchAndResolveTypeBindings(reps: List[(patternUniv.Type, candidateUniv.Type)], depth: Int)(implicit internalDefs: InternalDefs): Bindings = 
  {
    if (reps.isEmpty)
      EmptyBindings
    else
      reps.map({ case (a, b) => 
        matchAndResolveTypeBindings(a, b, depth)
      }).reduceLeft(_ ++ _)
  }
  
  type InternalDefs = Set[patternUniv.Name]
  
  def getNamesDefinedIn(u: api.Universe)(stats: List[u.Tree]): Set[u.Name] =
    stats.collect { case u.ValDef(_, name, _, _) => name: u.Name } toSet
  
  case class NoTreeMatchException(expected: Any, found: Any, msg: String, depth: Int)
  extends RuntimeException(msg)
    
  case class NoTypeMatchException(expected: Any, found: Any, msg: String, depth: Int, insideExpected: AnyRef = null, insideFound: AnyRef = null)
  extends RuntimeException(msg)
    
  def isNoType(u: api.Universe)(t: u.Type) =
    t == null || 
    t == u.NoType || 
    t == u.NoPrefix ||
    t == u.definitions.UnitClass.asType || {
      val s = t.toString
      s == "<notype>" || s == "scala.this.Unit"
    }
    
  def clstr(v: AnyRef) = 
    v.getClass.getName + " <- " + v.getClass.getSuperclass.getName
    
  def types(u: api.Universe)(syms: List[u.Symbol]) = 
    syms.map(_.asType)
          
  def zipTypes(syms1: List[patternUniv.Symbol], syms2: List[candidateUniv.Symbol]) = 
    types(patternUniv)(syms1).zip(types(candidateUniv)(syms2))
          
  def isTypeParameter(t: patternUniv.Type) = {
    t != null && {
      type PlasticSymbol = {
        def isTypeParameter: Boolean
      }
      
      val s = t.typeSymbol
      s != null && s.asInstanceOf[PlasticSymbol].isTypeParameter ||
      TypeVars.isTypeVar(t.asInstanceOf[mirror.Type])
    }
  } 
    
  def matchAndResolveTypeBindings(pattern0: patternUniv.Type, tree0: candidateUniv.Type, depth: Int = 0)(implicit internalDefs: InternalDefs = Set()): Bindings = 
  {
    import candidateUniv._
    
    lazy val EmptyBindings = Bindings()
  
    val pattern = resolveType(patternUniv)(pattern0)
    val tree = resolveType(candidateUniv)(tree0)
    
    if (pattern.toString.contains(".api")) {
      println("Going down in types (depth " + depth + "):")
      println("\ttype pattern = " + pattern + ": " + clstr(pattern))
      println("\ttype found = " + tree + ": " + clstr(tree))
    }
    
    //if (pattern != null && pattern == tree) {
    //  EmptyBindings
    //} else 
    {
      /*
      if (pattern != null && tree != null)
        if (pattern.typeSymbol.isPackage != tree.typeSymbol.isPackage)
          throw new NoTypeMatchException(pattern0, tree0, "Package vs. non-package types", depth)
      */
      val patNoType = isNoType(patternUniv)(pattern)
      val candNoType = isNoType(candidateUniv)(tree)
      
      val ret = (pattern, tree) match {
        // TODO remove null acceptance once macro typechecker is fixed !
        case (_, _) if pattern == null || patNoType && candNoType => 
          EmptyBindings
          
        case (_, _) if isTypeParameter(pattern) =>
          Bindings(Map(), Map(pattern -> tree))
          
        case (_, _) if candNoType && !patNoType =>
          throw NoTypeMatchException(pattern0, tree0, "Type matching failed", depth)
        
        case (patternUniv.RefinedType(parents, decls), RefinedType(parents2, decls2)) =>
          EmptyBindings
          
        case (patternUniv.TypeBounds(lo, hi), TypeBounds(lo2, hi2)) =>
          matchAndResolveTypeBindings(List((lo, lo2), (hi, hi2)), depth + 1)
          
        case (patternUniv.MethodType(paramtypes, result), MethodType(paramtypes2, result2)) =>
          matchAndResolveTypeBindings((result, result2) :: zipTypes(paramtypes, paramtypes2), depth + 1)
          
        case (patternUniv.NullaryMethodType(result), NullaryMethodType(result2)) =>
          matchAndResolveTypeBindings(result, result2, depth + 1)
          
        case (patternUniv.PolyType(tparams, result), PolyType(tparams2, result2)) =>
          matchAndResolveTypeBindings((result, result2):: zipTypes(tparams, tparams2), depth + 1)
          
        case (patternUniv.ExistentialType(tparams, result), ExistentialType(tparams2, result2)) =>
          matchAndResolveTypeBindings((result, result2) :: zipTypes(tparams, tparams2), depth + 1)
        
        case (patternUniv.TypeRef(pre, sym, args), TypeRef(pre2, sym2, args2)) 
        if args.size == args2.size //&&
           //pattern.kind == tree.kind && 
           //sym.kind == sym2.kind //&& 
           //sym.toString == sym2.toString // TODO remove this ugly hack !
        =>
          //if (pattern.kind != tree.kind)
          //  throw NoTypeMatchException(pattern0, tree0, "Different type kinds : " + pattern.kind + " vs. " + tree.kind, depth)
          if (pattern.toString.contains(".api")) {
            println("pattern.sym = " + sym + " (" + sym.kind + "), tree.sym = " + sym2 + " (" + sym2.kind + ")")
            println("pattern.pre = " + pre + " (" + pre.kind + "), tree.pre = " + pre2 + " (" + pre2.kind + ")")
          }
          //matchAndResolveTypeBindings(sym.asType, sym2.asType, depth + 1) ++ 
          matchAndResolveTypeBindings(pre, pre2, depth + 1) ++
          matchAndResolveTypeBindings(args.zip(args2), depth + 1)
        
        case _ =>
          if (Option(pattern).toString == Option(tree).toString) {
            println("WARNING: Monkey type matching of " + pattern + " vs. " + tree)
            EmptyBindings
          } else {
            if (depth > 0)
              println("TYPE MISMATCH \n\texpected = " + pattern0 + "\n\t\t" + Option(pattern0).map(_.getClass.getName) + "\n\tfound = " + tree0 + "\n\t\t" + Option(tree0).map(_.getClass.getName))
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
  
  def matchAndResolveTreeBindings(pattern: patternUniv.Tree, tree: candidateUniv.Tree, depth: Int = 0)(implicit internalDefs: InternalDefs = Set()): Bindings = 
  {
    val patternType = getOrFixType(patternUniv)(pattern)
    val candidateType = getOrFixType(candidateUniv)(tree)
    
    //if (depth > 0)
    //
    val typeBindings = try {
      matchAndResolveTypeBindings(patternType, candidateType, depth)
    } catch { 
      case ex: NoTypeMatchException =>
        throw ex.copy(insideExpected = pattern, insideFound = tree)
    }
    //println("Going down in trees (depth " + depth + "):")
    //println("\tpattern = " + pattern + ": " + patternType + " (" + pattern.getClass.getName + ", " + clstr(patternType) + ")")
    //println("\tfound = " + tree + ": " + candidateType + " (" + tree.getClass.getName + ", " + clstr(candidateType) + ")")
    //println("\ttypeBindings = " + typeBindings)
    
    typeBindings ++ ( 
      (pattern, tree) match {
        case (_, _) if pattern.isEmpty && tree.isEmpty =>
          EmptyBindings
          
        case (patternUniv.This(_), candidateUniv.This(_)) =>
          EmptyBindings
          
        case (patternUniv.Literal(patternUniv.Constant(a)), candidateUniv.Literal(candidateUniv.Constant(a2))) 
        if a == a2 =>
          EmptyBindings
          
        case (_: patternUniv.TypeTree, _: candidateUniv.TypeTree) =>
          //println("# Type Trees :")
          //println("# \texpected = " + pattern.tpe + ": " + pattern.tpe.getClass.getName)
          //println("# \tfound = " + tree.tpe + ": " + tree.tpe.getClass.getName)
          EmptyBindings // already handled by matchAndResolveTypeBindings
          //Bindings(Map(), Map(pattern.tpe -> tree.tpe))
          
        case (patternUniv.Ident(n), _) =>
          if (internalDefs.contains(n))
            EmptyBindings
          else /*tree match {
            case candidateUniv.Ident(nn) if n.toString == nn.toString =>
              EmptyBindings
            case _ =>*/
              //println("GOT BINDING " + pattern + " -> " + tree + " (tree is " + tree.getClass.getName + ")")
              Bindings(Map(n.toString -> tree), Map())
          //}
            
        case (patternUniv.ValDef(mods, name, tpt, rhs), candidateUniv.ValDef(mods2, name2, tpt2, rhs2))
        if mods.modifiers == mods2.modifiers =>
          val r = matchAndResolveTreeBindings(
            List((rhs, rhs2), (tpt, tpt2)), depth + 1
          )(
            internalDefs + name
          )
            
          if (name == name2)
            r
          else
            r.bindName(name, candidateUniv.Ident(name2))
        
        case (patternUniv.Function(vparams, body), candidateUniv.Function(vparams2, body2)) =>
          matchAndResolveTreeBindings(
            (body, body2) :: vparams.zip(vparams2), depth + 1
          )(
            internalDefs ++ vparams.map(_.name)
          )
          
        case (patternUniv.TypeApply(fun, args), candidateUniv.TypeApply(fun2, args2)) =>
          matchAndResolveTreeBindings(
            (fun, fun2) :: args.zip(args2), depth + 1
          )
        
        case (patternUniv.Apply(a, b), candidateUniv.Apply(a2, b2)) =>
          matchAndResolveTreeBindings(
            (a, a2) :: b.zip(b2), depth + 1
          )
          
        case (patternUniv.Block(l, v), candidateUniv.Block(l2, v2)) =>
          matchAndResolveTreeBindings(
            (v, v2) :: l.zip(l2), depth + 1
          )(
            internalDefs ++ getNamesDefinedIn(patternUniv)(l)
          )
          
        case (patternUniv.Select(a, n), candidateUniv.Select(a2, n2)) if n.toString == n2.toString =>
          //println("Matched select " + a + " vs. " + a2)
            matchAndResolveTreeBindings(
              a, a2, depth + 1
            )
        
        // TODO
        //case (ClassDef(mods, name, tparams, impl), ClassDef(mods2, name2, tparams2, impl2)) =
        //  matchAndResolveTreeBindings(impl, impl)(internalDefs + name)
        
        case (_, candidateUniv.TypeApply(target, typeArgs)) if workAroundMissingTypeApply =>
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
  def matchAndResolveBindings(pattern: patternUniv.Tree, tree: candidateUniv.Tree): Bindings = 
  {
    /*val typeBindings = matchAndResolveTypeBindings(
      getOrFixType(patternUniv)(pattern), getOrFixType(candidateUniv)(tree)
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
