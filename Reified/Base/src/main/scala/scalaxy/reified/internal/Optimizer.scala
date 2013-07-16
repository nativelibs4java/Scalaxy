package scalaxy.reified.internal

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.NameTransformer.encode
import scala.tools.reflect.ToolBox

import scalaxy.reified.internal.Utils._

/**
 * Small AST optimizer that performs the following rewrites:
 * - transform function values into methods when they're only used as methods (frequent pattern with Scalaxy/Reified's function composition and capture of reified functions)
 * - TODO: add Range foreach loops optimizations from Scalaxy
 */
object Optimizer {

  def reset(tree: Tree, toolbox: ToolBox[universe.type]): Tree = {
    typeCheckTree(toolbox.resetAllAttrs(resolveModulePaths(universe)(tree)))
  }

  def optimize(rawTree: Tree, toolbox: ToolBox[universe.type]): Tree = {
    val result = optimizeLoops(optimizeFunctionVals(rawTree, toolbox), toolbox)
    //val result = optimizeFunctionVals(rawTree, toolbox)
    //val result = reset(rawTree, toolbox)
    //println("Raw tree:\n" + rawTree)
    //println("Optimized tree:\n" + result)
    result
  }

  private def newInlineAnnotation = {
    Apply(
      Select(
        New(Ident(typeOf[scala.inline].typeSymbol)),
        nme.CONSTRUCTOR),
      Nil)
  }

  def optimizeFunctionVals(rawTree: Tree, toolbox: ToolBox[universe.type]): Tree = {
    val tree = reset(rawTree, toolbox)

    val functionSymbols = tree collect {
      case vd @ ValDef(mods, name, tpt, Function(vparams, body)) =>
        vd.symbol
    }

    assert(functionSymbols.forall(s => s != null && s != NoSymbol), "Some ValDefs have no symbol")

    val functionsUsedAsObjects = tree.collect {
      case Select(t, m) if functionSymbols.contains(t.symbol) && m.toString != "apply" =>
        t.symbol
      case ValDef(_, _, _, rhs) if functionSymbols.contains(rhs.symbol) =>
        rhs.symbol
    }

    val optimizableFunctions = functionSymbols.toSet -- functionsUsedAsObjects.toSet

    //println(s"functionSymbols = $functionSymbols")
    //println(s"functionsUsedAsObjects = $functionsUsedAsObjects")
    //println(s"optimizableFunctions = $optimizableFunctions")
    val functionsPromoter = new Transformer {
      override def transform(tree: Tree) = tree match {

        case ValDef(mods, name, tpt, Function(vparams, body)) if optimizableFunctions(tree.symbol) =>
          //println(s"optimizing " + tree.symbol + " = " + tree)
          DefDef(
            mods.mapAnnotations(list => newInlineAnnotation :: list),
            name,
            Nil,
            List(vparams),
            TypeTree(NoType),
            transform(body))

        case Apply(Select(t, m), args) if optimizableFunctions.contains(t.symbol) && m.toString == "apply" =>
          Apply(t, args.map(transform(_)))

        case _ =>
          super.transform(tree)
      }
    }
    val optimized = functionsPromoter.transform(tree)
    //println("Original tree:\n" + tree)
    //println("Optimized tree:\n" + optimized)

    optimized
  }

  object CommonScalaNames {
    import definitions._

    class N(val s: String) {
      def unapply(n: Name): Boolean = n.toString == s
      def apply(): TermName = s
    }
    object N {
      def apply(s: String) = new N(s)
    }
    implicit def N2TermName(n: N) = n()

    val addAssignName = N(NameTransformer.encode("+="))
    val toArrayName = N("toArray")
    val toListName = N("toList")
    val toSeqName = N("toSeq")
    val toSetName = N("toSet")
    val toIndexedSeqName = N("toIndexedSeq")
    val toVectorName = N("toVector")
    val toMapName = N("toMap")
    val resultName = N("result")
    val scalaName = N("scala")
    val ArrayName = N("Array")
    val intWrapperName = N("intWrapper")
    val tabulateName = N("tabulate")
    val toName = N("to")
    val byName = N("by")
    val withFilterName = N("withFilter")
    val untilName = N("until")
    val isEmptyName = N("isEmpty")
    val sumName = N("sum")
    val productName = N("product")
    val minName = N("min")
    val maxName = N("max")
    val headName = N("head")
    val tailName = N("tail")
    val foreachName = N("foreach")
    val foldLeftName = N("foldLeft")
    val foldRightName = N("foldRight")
    val zipWithIndexName = N("zipWithIndex")
    val zipName = N("zip")
    val reverseName = N("reverse")
    val reduceLeftName = N("reduceLeft")
    val reduceRightName = N("reduceRight")
    val scanLeftName = N("scanLeft")
    val scanRightName = N("scanRight")
    val mapName = N("map")
    val collectName = N("collect")
    val canBuildFromName = N("canBuildFrom")
    val filterName = N("filter")
    val filterNotName = N("filterNot")
    val takeWhileName = N("takeWhile")
    val dropWhileName = N("dropWhile")
    val countName = N("count")
    val lengthName = N("length")
    val forallName = N("forall")
    val existsName = N("exists")
    val findName = N("find")
    val updateName = N("update")
    val toSizeTName = N("toSizeT")
    val toLongName = N("toLong")
    val toIntName = N("toInt")
    val toShortName = N("toShort")
    val toByteName = N("toByte")
    val toCharName = N("toChar")
    val toDoubleName = N("toDouble")
    val toFloatName = N("toFloat")
    val mathName = N("math")
    val packageName = N("package")
    val applyName = N("apply")
    val thisName = N("this")
    val superName = N("super")

    def C(name: String) = rootMirror.staticClass(name)
    def M(name: String) = rootMirror.staticModule(name)
    def P(name: String) = rootMirror.staticPackage(name)

    lazy val ScalaReflectPackage = P("scala.reflect")
    lazy val ScalaCollectionPackage = P("scala.collection")
    lazy val ScalaMathPackage = M("scala.math.package")
    lazy val ScalaMathPackageClass =
      ScalaMathPackage.moduleClass //.asModule.moduleClass
    lazy val ScalaMathCommonClass = C("scala.MathCommon")

    lazy val PredefModule = M("scala.Predef")

    lazy val SeqModule = M("scala.collection.Seq")
    lazy val SeqClass = C("scala.collection.Seq")
    lazy val SetModule = M("scala.collection.Set")
    lazy val SetClass = C("scala.collection.Set")
    lazy val VectorClass = C("scala.collection.Set")
    lazy val ListClass = C("scala.List")
    lazy val ImmutableListClass = C("scala.collection.immutable.List")
    lazy val NonEmptyListClass = C("scala.collection.immutable.$colon$colon")
    lazy val IndexedSeqModule = M("scala.collection.IndexedSeq")
    lazy val IndexedSeqClass = C("scala.collection.IndexedSeq")
    lazy val OptionModule = M("scala.Option")
    lazy val OptionClass = C("scala.Option")
    lazy val SomeModule = M("scala.Some")
    lazy val NoneModule = M("scala.None")
    lazy val StringOpsClass = C("scala.collection.immutable.StringOps")
    lazy val ArrayOpsClass = C("scala.collection.mutable.ArrayOps")

    lazy val VectorBuilderClass = C("scala.collection.immutable.VectorBuilder")
    lazy val ListBufferClass = C("scala.collection.mutable.ListBuffer")
    lazy val ArrayBufferClass = C("scala.collection.mutable.ArrayBuffer")
    lazy val WrappedArrayBuilderClass = C("scala.collection.mutable.WrappedArrayBuilder")
    lazy val RefArrayBuilderClass = C("scala.collection.mutable.ArrayBuilder.ofRef")
    lazy val RefArrayOpsClass = C("scala.collection.mutable.ArrayOps.ofRef")
    lazy val SetBuilderClass = C("scala.collection.mutable.SetBuilder")
  }

  object Predef {
    import CommonScalaNames._

    def unapply(tree: Tree): Boolean = tree.symbol == PredefModule
  }

  object IntRange {
    import CommonScalaNames._

    def apply(from: Tree, to: Tree, by: Option[Tree], isInclusive: Boolean, filters: List[Tree]) = sys.error("not implemented")

    def unapply(tree: Tree): Option[(Tree, Tree, Option[Tree], Boolean, List[Tree])] = {
      if (tree.tpe <:< typeOf[Range]) {
        tree match {
          case Apply(
            Select(
              Apply(
                Select(Predef(), intWrapperName()),
                List(from)),
              funToName @ (toName() | untilName())),
            List(to)) =>

            Option(funToName) collect {
              case toName() =>
                (from, to, None, true, Nil)
              case untilName() =>
                (from, to, None, false, Nil)
            }
          case Apply(
            Select(
              IntRange(from, to, by, isInclusive, filters),
              n @ (byName() | withFilterName() | filterName())),
            List(arg)) =>

            Option(n) collect {
              case byName() if by == None =>
                (from, to, Some(arg), isInclusive, filters)
              case withFilterName() | filterName() /* if !options.stream */ =>
                (from, to, by, isInclusive, filters :+ arg)
            }
          case _ =>
            None
        }
      } else {
        None
      }
    }
  }

  private def getFreshNameGenerator(tree: Tree): String => TermName = {
    val names = collection.mutable.HashSet[String]()
    names ++= tree.collect {
      case t if t.symbol != null && t.symbol.isTerm =>
        t.symbol.name.toString
    }

    (base: String) => {
      var i = 1;
      var name: String = null
      while ({ name = syntheticVariableNamePrefix + base + "$" + i; names.contains(name) }) {
        i += 1
      }
      names.add(name)
      name
    }
  }

  object Step {
    def unapply(treeOpt: Option[Tree]): Option[Int] = Option(treeOpt) collect {
      case Some(Literal(Constant(step: Int))) =>
        step
      case None =>
        1
    }
  }

  def optimizeLoops(rawTree: Tree, toolbox: ToolBox[universe.type]): Tree = {
    import toolbox.resetAllAttrs
    import CommonScalaNames._

    val tree = reset(rawTree, toolbox)
    def freshName = getFreshNameGenerator(tree)

    val transformer = new Transformer {
      override def transform(tree: Tree) = tree match {
        case Apply(
          TypeApply(
            Select(
              IntRange(start, end, Step(step), isInclusive, filters),
              foreachName()),
            List(u)),
          List(Function(List(param), body))) =>

          def newIntVal(name: TermName, rhs: Tree) =
            ValDef(NoMods, name, TypeTree(typeOf[Int]), rhs)

          def newIntVar(name: TermName, rhs: Tree) =
            ValDef(Modifiers(Flag.MUTABLE), name, TypeTree(typeOf[Int]), rhs)

          // Body expects a local constant: create a var outside the loop + a val inside it.
          val iVar = newIntVar(freshName("i"), start)
          val iVal = newIntVal(param.name, Ident(iVar.name))
          val stepVal = newIntVal(freshName("step"), Literal(Constant(step)))
          val endVal = newIntVal(freshName("end"), end)
          val condition =
            Apply(
              Select(
                Ident(iVar.name),
                encode(
                  if (step > 0) {
                    if (isInclusive) "<=" else "<"
                  } else {
                    if (isInclusive) ">=" else ">"
                  }
                ): TermName
              ),
              List(Ident(endVal.name))
            )

          val iVarExpr = newExpr[Unit](iVar)
          val iValExpr = newExpr[Unit](iVal)
          val endValExpr = newExpr[Unit](endVal)
          val stepValExpr = newExpr[Unit](stepVal)
          val conditionExpr = newExpr[Boolean](condition)
          // Body still refers to old function param symbol (which has same name as iVal).
          // We must wipe it out (alas, it's not local, so we must reset all symbols).
          // TODO: be less extreme, replacing only the param symbol (see branch replaceParamSymbols).
          val bodyExpr = newExpr[Unit](resetAllAttrs(transform(body)))

          val incrExpr = newExpr[Unit](
            Assign(
              Ident(iVar.name),
              Apply(
                Select(
                  Ident(iVar.name),
                  encode("+"): TermName
                ),
                List(Ident(stepVal.name))
              )
            )
          )
          val iVarRef = newExpr[Int](Ident(iVar.name))
          val stepValRef = newExpr[Int](Ident(stepVal.name))

          universe.reify({
            iVarExpr.splice
            endValExpr.splice
            stepValExpr.splice
            while (conditionExpr.splice) {
              iValExpr.splice
              bodyExpr.splice
              incrExpr.splice
            }
          }).tree
        case _ =>
          super.transform(tree)
      }
    }
    transformer.transform(tree)
  }
}
