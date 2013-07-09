package scalaxy.compilets
package components

import scala.language.reflectiveCalls
import scala.language.postfixOps

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.transform.TypingTransformers
import Function.tupled

sealed trait PatternMatchingFailure

case class NoTreeMatchFailure(expected: Any, found: Any, msg: String, depth: Int)
extends PatternMatchingFailure

case class NoTypeMatchFailure(expected: Any, found: Any, msg: String, depth: Int, insideExpected: AnyRef = null, insideFound: AnyRef = null)
extends PatternMatchingFailure

trait PatternMatchers
extends TypingTransformers
//   with MirrorConversions
{
  this: PluginComponent =>

  import scala.tools.nsc.symtab.Flags._

  import scala.reflect._

  private def ultraLogPattern(txt: => String) {
    //println(txt)
  }

  val patternUniv: api.Universe
  val candidateUniv: api.Universe with scala.reflect.internal.Importers

  lazy val applyNamePattern = patternUniv.newTermName("apply")
  
  var printed = Set[String]()
  
  case class Bindings(
    nameBindings: Map[String, candidateUniv.Tree] = Map(),
    typeBindings: Map[patternUniv.Type, candidateUniv.Type] = Map(),
    functionBindings: Map[String, (List[String], patternUniv.Tree, candidateUniv.Tree)] = Map(),
    failure: PatternMatchingFailure = null
  ) {
    lazy val stringIndexedTypeBindings =
      typeBindings.map { case (k, v) => (k.toString, (k, v)) }

    def isEmpty =
      failure == null &&
      nameBindings.isEmpty &&
      typeBindings.isEmpty &&
      functionBindings.isEmpty
      
    def getType(t: patternUniv.Type): Option[candidateUniv.Type] = {
      Option(t).
        flatMap(tt => {
          var typeBinding = typeBindings.get(tt)
          if (typeBinding == None &&
              HacksAndWorkarounds.useStringBasedTypeEqualityInBindings)
          {
            for ((origKey, value) <- stringIndexedTypeBindings.get(tt.toString))
            {
              typeBinding = Some(value)

              ultraLogPattern("WARNING: type " + t + " (" + clstr(t) + ") was deemed to be equal to " + origKey + "(" + clstr(origKey) + "), but they're not equal!")

              ultraLogPattern("t.hashCode = " + t.hashCode + ", origKey.hashCode = " + origKey.hashCode)
            }
          }
          typeBinding
        })
    }

    def bindName(n: patternUniv.Name, v: candidateUniv.Tree) =
      copy(nameBindings = nameBindings + (n.toString -> v))

    def bindType(t: patternUniv.Type, t2: candidateUniv.Type) =
      copy(typeBindings = typeBindings + (t -> t2))

    def markFailure(insideExpected: => patternUniv.Tree, insideFound: => candidateUniv.Tree) = {
      if (failure != null) {
        failure match {
          case ex: NoTypeMatchFailure =>
            copy(failure = ex.copy(insideExpected = insideExpected, insideFound = insideFound))
          case _ =>
            this
        }
      } else {
        this
      }
    }
    
    def ++(b: => Bindings) = {
      if (failure != null || b.isEmpty)
        this
      else if (isEmpty || b.failure != null)
        b
      else
        Bindings(
          nameBindings ++ b.nameBindings,
          typeBindings ++ b.typeBindings,
          functionBindings ++ b.functionBindings
        )
    }
    
    override def toString = 
      "Bindings {\n\t" + (if (failure != null) "failure = " + failure else (
      Seq[Seq[Any]](nameBindings.toSeq, typeBindings.toSeq, functionBindings.toSeq).flatMap(s => Option(s)).flatten.mkString("\n\t")
      )) + "\n}\n"

    def convertToExpected[T](v: Any) = v.asInstanceOf[T]

    def apply(replacement: patternUniv.Tree): candidateUniv.Tree =
    {
      val importer = new candidateUniv.StandardImporter {
        val from = patternUniv.asInstanceOf[scala.reflect.internal.SymbolTable]
        
        override def importTree(tree: from.Tree) = convertToExpected {//: candidateUniv.Tree = {
          //println("importTree(" + tree + ")")
          //for ((n, params) <- functionReplacements.get(tree.asInstanceOf[patternUniv.Tree])) {
          //  println("FOUND FUNCTION REPLACEMENT with " + n + "(" + params.mkString(", ") + ")") 
          //}
          tree match {
            case from.Ident(n) =>
              nameBindings.get(n.toString).
              getOrElse(super.importTree(tree))
            case _ =>
              super.importTree(tree)
          }
        }
        override def importType(tpe: from.Type) = convertToExpected {
          if (tpe == null) {
            null
          } else {
            getType(resolveType(patternUniv)(tpe.asInstanceOf[patternUniv.Type])).
            getOrElse(super.importType(tpe))
          }
        }
      }
      importer.importTree(replacement.asInstanceOf[importer.from.Tree])
    }
  }
  
  def bindingFailed(failure: PatternMatchingFailure) =
    Bindings(nameBindings = null, typeBindings = null, functionBindings = null, failure = failure)

  def combine(a: Bindings, b: Bindings) = a ++ b

  implicit def t2pt(tp: api.Types#Type) = 
    tp.asInstanceOf[PlasticType]
    
  type PlasticType = {
    def dealias: api.Types#Type
    def deconst: api.Types#Type
    def normalize: api.Types#Type
  }

  def normalize(u: api.Universe)(tpe: u.Type) = {
    tpe.deconst.dealias.normalize.asInstanceOf[u.Type]
  }

  def resolveType(u: api.Universe)(tpe: u.Type): u.Type = {
    Option(tpe).map(normalize(u)(_)).map({
      case tt @ u.ThisType(sym) =>
        if (tpe.toString == "<root>")
          u.NoPrefix // Should be fine TODO ask gurus about possible ambiguity here between root and NoPrefix
        else if (sym.isType)
          resolveType(u)(sym.asType.toType)
        else 
          tt
        
      case tt: u.NullaryMethodTypeApi =>
      //case tt @ u.NullaryMethodType(restpe) =>
        resolveType(u)(tt.resultType)
        
      case tt: u.SingleTypeApi =>
      //case tt @ u.SingleType(pre, sym) =>
        resolveType(u)(tt.sym.typeSignature)
      case tt =>
        tt
    }).orNull
  }

  def matchAndResolveTreeBindings(reps: List[(patternUniv.Tree, candidateUniv.Tree)], depth: Int)(implicit internalDefs: InternalDefs): Bindings =
  {
    if (reps.isEmpty)
      EmptyBindings
    else
      reps.foldLeft(EmptyBindings) {
        case (bindings, (a, b)) =>
          bindings ++ matchAndResolveTreeBindings(a, b, depth)
      }
  }

  def matchAndResolveTypeBindings(reps: List[(patternUniv.Type, candidateUniv.Type)], depth: Int)(implicit internalDefs: InternalDefs): Bindings =
  {
    if (reps.isEmpty)
      EmptyBindings
    else
      reps.foldLeft(EmptyBindings) {
        case (bindings, (a, b)) =>
          bindings ++ matchAndResolveTypeBindings(a, b, depth)
      }
  }

  type InternalDefs = Set[patternUniv.Name]

  def getNamesDefinedIn(u: api.Universe)(stats: List[u.Tree]): Set[u.Name] =
    stats.collect { case u.ValDef(_, name, _, _) => name: u.Name } toSet

  def isNoType(u: api.Universe)(t: u.Type) =
    t == null ||
    t == u.NoType ||
    t == u.NoPrefix ||
    t == u.definitions.UnitClass.asType.toType || {
      val s = t.toString
      s == "<notype>" || s == "scala.this.Unit"
    }

  def clstr(v: AnyRef) =
    if (v == null)
      "<null>"
    else
      v.getClass.getName + " <- " + v.getClass.getSuperclass.getName

  def types(u: api.Universe)(syms: List[u.Symbol]) =
    syms.flatMap {
      case t =>
        try
          Some(t.asType.toType)
        catch { case _: Throwable =>
          None 
        }
    }

  def zipTypes(syms1: List[patternUniv.Symbol], syms2: List[candidateUniv.Symbol]) =
    types(patternUniv)(syms1).zip(types(candidateUniv)(syms2))

  def isParameter(t: patternUniv.Type) = {
    t != null && {
      val s = t.typeSymbol
      s != null &&
      s.isParameter
    }
  }

  lazy val candidateUnitTpe =
    candidateUniv.definitions.UnitClass.asType.toType
  lazy val patternUnitTpe =
    patternUniv.definitions.UnitClass.asType.toType
  
  val EmptyBindings = Bindings()

  def matchAndResolveTypeBindings(
    pattern0: patternUniv.Type,
    tree0: candidateUniv.Type,
    depth: Int = 0,
    strict: Boolean = false
  )(
    implicit internalDefs: InternalDefs = Set()
  ): Bindings =
  {
    import candidateUniv._

    val pattern = resolveType(patternUniv)(pattern0)
    val tree = resolveType(candidateUniv)(tree0)
        
    if (false)//if (depth > 0)
    {
      println("Going down in types (depth " + depth + "):")
      println("\tpattern = " + pattern + " (" + Option(pattern).map(_.getClass.getName).orNull + ")")
      println("\tfound = " + tree + " (" + Option(tree).map(_.getClass.getName).orNull + ")")
    }
    
    //lazy val desc = "(" + pattern + ": " + clstr(pattern) + " vs. " + tree + ": " + clstr(tree) + ")"

    if (isParameter(pattern)) {
      Bindings(typeBindings = Map(pattern -> tree))
    }
    else
    //if (pattern != null && pattern.toString.matches(".*\\.T\\d+")) {
    //  ultraLogPattern("TYPE MATCHING KINDA FAILED ON isParameter(" + pattern + ")")
    //  Bindings(Map(), Map(pattern -> tree))
    //}
    //else
    if (HacksAndWorkarounds.workAroundNullPatternTypes &&
        tree == null && pattern != null) {
      bindingFailed(NoTypeMatchFailure(pattern0, tree0, "Type kind matching failed (" + pattern + " vs. " + tree + ")", depth))
    }
    else
    {
      val ret = (pattern, tree) match {
        // TODO remove null acceptance once macro typechecker is fixed !
        //case (_, _)
        //if pattern == null && HacksAndWorkarounds.workAroundNullPatternTypes =>
        //  EmptyBindings

        case (patternUniv.NoType, candidateUniv.NoType) =>
          EmptyBindings

        case (patternUniv.NoPrefix, candidateUniv.NoPrefix) =>
          EmptyBindings

        case (`patternUnitTpe`, `candidateUnitTpe`) =>
          EmptyBindings

        // TODO support refined types again:
        //case (patternUniv.RefinedType(parents, decls), RefinedType(parents2, decls2)) =>
        //  println("TYPE MATCH refined type")
        //  EmptyBindings

        case (patternUniv.ClassInfoType(_, _, sym), ClassInfoType(_, _, sym2))
        if sym.fullName == sym2.fullName =>
          EmptyBindings
        
        case (patternUniv.SingleType(pre, sym), SingleType(pre2, sym2))
        if sym.fullName == sym2.fullName =>
          val subs = matchAndResolveTypeBindings(List((pre, pre2)), depth + 1)
          //println("Matched single types, skipping bindings: " + subs)
          if (subs.failure != null)
            subs
          else
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
        if args.size == args2.size &&
           sym != null && sym2 != null &&
           sym.fullName == sym2.fullName
        =>
          matchAndResolveTypeBindings(pre, pre2, depth + 1) ++
          matchAndResolveTypeBindings(args.zip(args2), depth + 1)
        
        //case (patternUniv.ClassInfoType(_, _, _), _) 
        //if tree.typeSymbol.isPackage =>
        ////if //tree.typeSymbol.isPackage && 
        ////  Option(pattern0).toString == Option(tree0).toString =>
        //  EmptyBindings // TODO remove this hack
          
        
        case _ =>
          if (HacksAndWorkarounds.useStringBasedTypePatternMatching &&
              Option(pattern).toString == Option(tree).toString)
          {
            println("WARNING: Dumb string type matching of " + pattern + " (" + clstr(pattern) + ") " + " vs. " + tree + " (" + clstr(tree) + ") " )
            EmptyBindings
          } else {
            //if (depth > 0)
            //  println("TYPE MISMATCH \n\texpected = " + pattern0 + "\n\t\t" + Option(pattern0).map(_.getClass.getName) + "\n\tfound = " + tree0 + "\n\t\t" + Option(tree0).map(_.getClass.getName))
            bindingFailed(NoTypeMatchFailure(pattern0, tree0, "Type matching failed between " + pattern + "(: " + pattern.getClass.getName + " <- " + pattern0.getClass.getName + ") and " + tree + "(: " + tree.getClass.getName + " <- " + tree0.getClass.getName + ")", depth))
          }
      }

      if (false)//ret.failure == null)
      {
        println("Successfully bound types (depth " + depth + "):")
        println("\ttype pattern = " + pattern + " (" + pattern0 + "): " + clstr(pattern))// + "; kind = " + Option(pattern).map(_.typeSymbol.kind))
        println("\ttype found = " + tree + " (" + tree0 + "): " + clstr(tree))
        println("\tbindings:\n\t\t" + (Option(ret.nameBindings).getOrElse(Seq()) ++ Option(ret.typeBindings).getOrElse(Seq()) ++ Option(ret.functionBindings).getOrElse(Seq())).mkString("\n\t\t"))
      }
      ret
    }
  }

  def matchAndResolveTreeBindings(pattern: patternUniv.Tree, tree: candidateUniv.Tree, depth: Int = 0)(implicit internalDefs: InternalDefs = Set()): Bindings =
  {
    val patternType = getOrFixType(patternUniv)(pattern)
    val candidateType = getOrFixType(candidateUniv)(tree)

    val typeBindings = if (patternType == null && HacksAndWorkarounds.workAroundNullPatternTypes) {
      EmptyBindings
    } else {
      val b = matchAndResolveTypeBindings(patternType, candidateType, depth)
      if (HacksAndWorkarounds.debugFailedMatches)
        b.markFailure(insideExpected = pattern, insideFound = tree)
      else
        b
    }
        
    if (false)//if (depth > 0)
    {
      println("Going down in trees (depth " + depth + "):")
      println("\tpattern = " + pattern + ": " + patternType + " (" + pattern.getClass.getName + ", " + clstr(patternType) + ")")
      println("\tfound = " + tree + ": " + candidateType + " (" + tree.getClass.getName + ", " + clstr(candidateType) + ")")
      println("\ttypeBindings = " + typeBindings)
    }
    
    if (typeBindings.failure != null) typeBindings
    else typeBindings ++ {
      lazy val desc = "(" + pattern + ": " + clstr(pattern) + " vs. " + tree + ": " + clstr(tree) + ")"

      val ret = (pattern, tree) match 
      {
        case (_, _) if pattern.isEmpty && tree.isEmpty =>
          EmptyBindings

        case (patternUniv.Apply(patternUniv.Select(i @ patternUniv.Ident(n), `applyNamePattern`), params), _)
        if i.symbol != null && i.symbol.isParameter  =>
          //println("### Found function param: \n\t" + pattern + "\n\t-> " + tree + "\n\t(i = " + i + ": " + i.getClass.getName + ", tpe = " + i.tpe + ", i.symbol.isParameter = " + i.symbol.isParameter + ")")
          // TODO: recurse on pattern to bind params to actual values?
          //println("FUNCTION MAP RETURN " + patternType + " (sym.typeSignature = " + tree.symbol.typeSignature + ") -> " + candidateType) 
          Bindings(functionBindings = Map(
            n.toString -> ((
              params.map({ case patternUniv.Ident(p) => p.toString }),
              pattern,
              tree
            ))
          ))
          
        //case (_, _) if isParameter(patternType) =>
        //  EmptyBindings

        case (patternUniv.This(_), candidateUniv.This(_)) =>
          EmptyBindings

        case (patternUniv.Literal(patternUniv.Constant(a)), candidateUniv.Literal(candidateUniv.Constant(a2)))
        if a == a2 =>
          EmptyBindings

        //case (patternUniv.Ident(n), candidateUniv.Ident(n2))
        //if n.toString == n2.toString =>
        //  EmptyBindings
          
        case (patternUniv.Ident(n), _) =>
          //println("Ident TYPE IS " + patternType + ": " + Option(patternType).map(_.getClass.getName))
          //println("Ident SYMBOL IS " + pattern.symbol + ": " + pattern.symbol.typeSignature)
          if (internalDefs.contains(n) ||
              pattern.symbol.isPackage ||
              pattern.symbol.isType && !isParameter(pattern.symbol.asType.toType))
          {
            EmptyBindings
          } else {
            Bindings(nameBindings = Map(n.toString -> tree))
          }

        case (patternUniv.ValDef(mods, name, tpt, rhs), candidateUniv.ValDef(mods2, name2, tpt2, rhs2))
        if mods.flags == mods2.flags =>
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

        case (patternUniv.Select(a, n), candidateUniv.Select(a2, n2))
        if n.toString == n2.toString =>
          matchAndResolveTreeBindings(
            a, a2, depth + 1
          )

        // TODO
        //case (ClassDef(mods, name, tparams, impl), ClassDef(mods2, name2, tparams2, impl2)) =
        //  matchAndResolveTreeBindings(impl, impl)(internalDefs + name)

        case (_, candidateUniv.TypeApply(target, typeArgs))
        if HacksAndWorkarounds.workAroundMissingTypeApply =>
          //println("Workaround for missing TypeApply in pattern... (loosing types " + typeArgs + ")")
          matchAndResolveTreeBindings(pattern, target, depth + 1)

        case (patternUniv.AppliedTypeTree(tpt, args), candidateUniv.AppliedTypeTree(tpt2, args2))
        if args.size == args2.size =>
          matchAndResolveTreeBindings(tpt, tpt2, depth + 1) ++
          matchAndResolveTreeBindings(args.zip(args2), depth + 1)

        case (patternUniv.SelectFromTypeTree(qualifier, name), candidateUniv.SelectFromTypeTree(qualifier2, name2))
        if name.toString == name2.toString =>
          matchAndResolveTreeBindings(qualifier, qualifier2, depth + 1)

        case (patternUniv.SingletonTypeTree(ref), candidateUniv.SingletonTypeTree(ref2)) =>
          matchAndResolveTreeBindings(ref, ref2, depth + 1)

        case (patternUniv.TypeTree(), candidateUniv.TypeTree())
        if pattern.toString == "<type ?>" =>
          EmptyBindings

        case _ =>
          if (HacksAndWorkarounds.useStringBasedTreePatternMatching &&
              Option(pattern).toString == Option(tree).toString)
          {
            println("WARNING: Monkey matching of " + pattern + " vs. " + tree)
            EmptyBindings
          } else {
            //if (depth > 0)
            //    println("TREE MISMATCH \n\texpected = " + toTypedString(pattern) + "\n\t\t" + pattern.getClass.getName + "\n\tfound = " + toTypedString(tree) + "\n\t\t" + tree.getClass.getName)
            bindingFailed(NoTreeMatchFailure(pattern, tree, "Different trees", depth))
          }
      }

      if (false)//ret.failure == null) 
      {
        println("Successfully bound trees (depth " + depth + "):")
        println("\ttree pattern = " + pattern + ": " + clstr(pattern))// + "; kind = " + Option(pattern).map(_.typeSymbol.kind))
        println("\ttree found = " + tree + ": " + clstr(tree))
        println("\tbindings:\n\t\t" + (ret.nameBindings ++ ret.typeBindings ++ ret.functionBindings).mkString("\n\t\t"))
      }
      ret
    }
  }
  private def toTypedString(v: Any) =
    v + Option(v).map(_ => ": " + v.getClass.getName + " <- " + v.getClass.getSuperclass.getSimpleName).getOrElse("")


  def getOrFixType(u: api.Universe)(tree: u.Tree): u.Type = {
    import u._
    import u.definitions._
    var t = tree.tpe
    val s = tree.symbol
    
    if (t == null && s != null && s != u.NoSymbol) 
      t = if (s.isType) s.asType.toType else s.typeSignature
    
    if (t == null)
      tree match {
        case Literal(Constant(v)) =>
          v match {
            case _: Int => IntTpe
            case _: Short => ShortTpe
            case _: Long => LongTpe
            case _: Byte => ByteTpe
            case _: Double => DoubleTpe
            case _: Float => FloatTpe
            case _: Char => CharTpe
            case _: Boolean => BooleanTpe
            case _: String => StringClass.asType.toType
            case _: Unit => UnitClass.asType.toType
            case _ =>
              null
          }
        case _ =>
          val st = try {
            tree.symbol.asType.toType
          } catch { case ex: Throwable => null }

          // TODO
          //println("Cannot fix type for " + tree + ": " + clstr(tree) + " (symbol = " + st + ")")
          st
          //null
      }
    else
      t
  }
}
