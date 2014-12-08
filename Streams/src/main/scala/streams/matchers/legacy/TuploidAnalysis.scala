package scalaxy.streams

import scala.collection.mutable.ArrayBuffer

/**
 * Refugee from Scalaxy/Components
 * TODO: modernize (quasiquotes...) / make it go away.
 */
trait TupleAnalysis
    extends TuploidValues
    with TreeBuilders
    with WithLocalContext {
    // extends MiscMatchers
    // with TreeBuilders {
  val global: reflect.api.Universe

  import global._
  import definitions._

  case class TupleInfo(tpe: Type, components: Seq[TupleInfo]) {
    assert(tpe != null, "null type in TupleInfo")
    lazy val flattenTypes: Seq[Type] = {
      components match {
        case Seq() =>
          Seq(tpe)
        case _ =>
          components.flatMap(_.flattenTypes)
      }
    }
    lazy val flattenPaths: Seq[List[Int]] = {
      components match {
        case Seq() =>
          Seq(Nil)
        case _ =>
          components.zipWithIndex.flatMap { case (c, i) => c.flattenPaths.map(p => i :: p) }
      }
    }
    lazy val componentSize: Int = {
      components match {
        case Seq() =>
          1
        case _ =>
          components.map(_.componentSize).sum
      }
    }
  }
  private val tupleInfos = new scala.collection.mutable.HashMap[Type, TupleInfo]

  def getTupleInfo(tpe: Type): TupleInfo = {
    assert(tpe != null, "null type in getTupleInfo")
    val actualTpe = normalize(tpe)
    tupleInfos.getOrElseUpdate(
      actualTpe,
      if (actualTpe <:< typeOf[Unit])
        TupleInfo(UnitTpe, Seq())
      else {
        actualTpe match {
          case t: TypeRef =>
            if (isTupleSymbol(t.sym))
              TupleInfo(t, t.args.map(getTupleInfo))
            else
              TupleInfo(t, Seq())
          case NoType =>
            TupleInfo(NoType, Seq())
          case _ =>
            throw new RuntimeException("Unhandled type : " + tpe + " (" + actualTpe + ": " + Option(actualTpe).map(_.getClass.getName) + ")")
            //System.exit(0)
            null
        }
      }
    )
  }
  def flattenTypes(tpe: Type): Seq[Type] =
    getTupleInfo(tpe).flattenTypes

  def flattenFiberPaths(tpe: Type): Seq[List[Int]] =
    flattenFiberPaths(getTupleInfo(tpe))

  def flattenFiberPaths(info: TupleInfo): Seq[List[Int]] = {
    val TupleInfo(_, components) = info
    if (components.isEmpty)
      Seq(Nil)
    else
      components.map(flattenFiberPaths).zipWithIndex flatMap {
        case (paths, i) => paths.map(path => i :: path)
      }
  }
  def getType(tree: Tree) = {
    if (tree.tpe == null || tree.tpe == NoType) {
      if (tree.symbol == null || tree.symbol == NoSymbol)
        NoType
      else
        tree.symbol.typeSignature
    } else {
      tree.tpe
    }
  }
  def applyFiberPath(rootGen: TreeGen, path: List[Int]): (Tree, Type) = {
    applyFiberPath(rootGen, rootGen().tpe, path)
  }

  def applyFiberPath(rootGen: TreeGen, rootTpe: Type, path: List[Int]): (Tree, Type) = {
    def sub(invertedPath: List[Int]): (Tree, Type) = invertedPath match {
      case Nil =>
        val root = typecheck { rootGen() }
        (root, rootTpe)
      case i :: rest =>
        val (inner, innerTpe) = applyFiberPath(rootGen, rootTpe, rest)
        val name = N("_" + (i + 1))

        //println("Getting member " + i + " of (" + inner + ": " + inner.tpe + ") ; invertedPath = " + invertedPath)
        assert(innerTpe != NoType, "Cannot apply tuple path on untyped tree")
        val info = getTupleInfo(innerTpe)
        assert(i < info.components.size, "bad path : i = " + i + ", type = " + innerTpe + ", path = " + path + ", root = " + rootGen())
        //val sym = innerTpe member name
        //println(s"innerTpe($innerTpe).member(name($name)) = sym($sym: ${sym.typeSignature})")
        // TODO typeCheck?
        //typeCheck

        (
          Select(inner, name),
          info.components(i).tpe
        )

    }
    sub(path.reverse)
  }

  def getComponentOffsetAndSizeOfIthMember(tpe: Type, i: Int) = {
    val TupleInfo(_, components) = getTupleInfo(tpe)
    (
      components.take(i).map(_.componentSize).sum,
      components(i).componentSize
    )
  }

  /**
   * Phases :
   * - unique renaming
   * - tuple cartography (map symbols and treeId to TupleSlices : x._2 will be checked against x ; if is x's symbol is mapped, the resulting slice will be composed and flattened
   * - tuple + block flattening (gives (Seq[Tree], Seq[Tree]) result)
   */
  // separate pass should return symbolsDefined, symbolsUsed
  // DefTree vs. RefTree
  // def typed(tree: Tree): Tree
  // def typeCheck(tree: Tree, pt: Type): Tree

  def ident(sym: Symbol, tpe: Type): Ident = {
    assert(sym != NoSymbol)
    val i = Ident(sym)
    try {
      typecheck(i, pt = sym.typeSignature).asInstanceOf[Ident]
    } catch {
      case _: Throwable =>
        i
    }
  }

  case class TupleSlice(baseSymbol: Symbol, sliceOffset: Int, sliceLength: Int) {
    def subSlice(offset: Int, length: Int) =
      TupleSlice(baseSymbol, sliceOffset + offset, length)

    def toTreeGen(analyzer: TupleAnalyzer): TreeGen = () => {
      val info = getTupleInfo(baseSymbol.typeSignature)
      val rootTpe = baseSymbol.typeSignature
      val root: TreeGen = () => ident(baseSymbol, rootTpe)
      assert(sliceLength == 1)
      //TupleCreation((0 until sliceLength).map(i => applyFiberPath(root, info.flattenPaths(sliceOffset + i))):_*)
      val flatPaths = info.flattenPaths
      assert(sliceOffset < flatPaths.size, "slice offset = " + sliceOffset + ", flat paths = " + flatPaths)
      //println(s"baseSymbol = $baseSymbol, ${baseSymbol.typeSignature}, ${root().symbol.typeSignature}")
      var (res, resTpe) = applyFiberPath(root, rootTpe, flatPaths(sliceOffset))
      //analyzer.setSlice(res, this)
      //res = replace(res)
      analyzer.setSlice(res, this)
      res
    }
  }
  class BoundTuple(rootSlice: TupleSlice) {
    def unapply(tree: Tree): Option[Seq[(Symbol, TupleSlice)]] = tree match {
      case Bind(name, what) =>
        val sub = this
        what match {
          case Ident(_) =>
            Some(Seq(tree.symbol -> rootSlice))
          case sub(m) =>
            Some(m :+ (tree.symbol -> rootSlice))
          case _ =>
            throw new RuntimeException("Not a bound tuple : " + tree + " (" + tree.getClass.getName + ")")
            None
        }
      case TupleCreation(components) =>
        //println("Found tuple creation with components " + components)
        var currentOffset = 0
        val ret = ArrayBuffer[(Symbol, TupleSlice)]()
        for ((component, i) <- components.zipWithIndex) {
          val compTpes = flattenTypes(component.tpe)
          val compSize = compTpes.size
          val subMatcher = new BoundTuple(rootSlice.subSlice(currentOffset, compSize))
          component match {
            case subMatcher(m) =>
              ret ++= m
            case _ =>
              //println("Cancelling BoundTuple because of component " + component + " of type " + component.tpe + " (length " + compSize + ") at offset " + currentOffset)
              return None // strict binding
          }
          currentOffset += compSize
        }
        Some(ret.toList)
      case _ =>
        throw new RuntimeException("Not a bound tuple : " + tree + " (" + tree.getClass.getName + ")") //\n\tnodes = " + nodeToString(tree))
        //System.exit(1)
        None
    }
  }
  class TupleAnalyzer(tree: Tree) {

    var treeTupleSlices = new scala.collection.mutable.HashMap[( /*Int,*/ Tree), TupleSlice]()
    //private var symbolTupleSlices = new scala.collection.mutable.HashMap[Symbol, TupleSlice]()
    var symbolTupleSlices = new scala.collection.mutable.HashMap[Symbol, TupleSlice]()

    def getSymbolSlice(sym: Symbol, recursive: Boolean = false): Option[TupleSlice] = {
      val direct = symbolTupleSlices.get(sym)
      direct match {
        case Some(directSlice) if recursive && directSlice.sliceLength > 1 && directSlice.baseSymbol != sym =>
          getSymbolSlice(directSlice.baseSymbol, recursive).orElse(direct)
        case _ =>
          direct
      }
    }
    def createTupleSlice(sym: Symbol, tpe: Type) = {
      val info = getTupleInfo(tpe)
      //assert(info.componentSize == 1, "Invalid multi-fibers slice for symbol " + sym + " (" + info.componentSize + " fibers)")
      TupleSlice(sym, 0, info.componentSize)
    }

    def getTreeSlice(tree: Tree, recursive: Boolean = false): Option[TupleSlice] = {
      val direct = symbolTupleSlices.get(tree.symbol).orElse(treeTupleSlices.get(( /*tree.id, */ tree)))
      if (recursive && direct != None)
        getSymbolSlice(direct.get.baseSymbol, recursive).orElse(direct)
      else
        direct.orElse(
          if (tree.symbol == null ||
            !tree.symbol.isTerm || //.getClass != classOf[TermSymbol] || // not isInstanceOf ! We don't want ModuleSymbol nor MethodSymbol here, which are both TermSymbol subclasses 
            tree.tpe == null || tree.tpe == NoType)
            None
          else {
            //println("Created slice for symbol " + tree.symbol + " (tree = " + tree + ", symbol.class = " + tree.symbol.getClass.getName + ")")
            Some(createTupleSlice(tree.symbol, tree.tpe))
            //None
          }
        )
    }

    def setSlice(sym: Symbol, slice: TupleSlice) = {
      assert(sym != slice.baseSymbol, "Invalid self-slice for symbol " + sym)
      //println("Setting slice " + slice + " for symbol " + sym)
      symbolTupleSlices(sym) = slice
    }

    def setSlice(tree: Tree, slice: TupleSlice) = {
      //println("Setting slice " + slice + " for tree " + tree)
      val info = getTupleInfo(slice.baseSymbol.typeSignature)
      val n = info.flattenPaths.size
      assert(slice.sliceOffset + slice.sliceLength <= n, "invalid slice for type " + tree.tpe + " : " + slice + ", flat types = " + info.flattenTypes)
      treeTupleSlices(( /*tree.id, */ tree)) = slice
      tree match {
        case vd: ValDef =>
          symbolTupleSlices(tree.symbol) = slice
        case _ =>
      }
    }

    // Identify trees and symbols that represent tuple slices
    new Traverser {
      override def traverse(tree: Tree): Unit = {
        tree match {
          case ValDef(mods, name, tpt, rhs) if !mods.hasFlag(Flag.MUTABLE) =>
            super.traverse(tree)
            //println("Got valdef " + name)
            val tupleInfo = getTupleInfo(rhs.tpe)
            if (tupleInfo == null) {
              throw new RuntimeException("No tuple info for type " + rhs.tpe + " !")
            }
            //setSlice(tree.symbol, TupleSlice(tree.symbol, 0, tupleInfo.componentSize))
            for (slice <- getTreeSlice(rhs, true)) {
              //println("\tvaldef " + tree.symbol + " linked to rhs slice " + slice)
              setSlice(tree.symbol, slice)
            }
          case Match(selector, cases) =>
            traverse(selector)
            //println("Found match") 
            for (slice <- getTreeSlice(selector)) {
              //println("\tMatch has slice " + slice)
              val subMatcher = new BoundTuple(slice)
              for (CaseDef(pat, guard, body) <- cases) {
                pat match {
                  case subMatcher(m) =>
                    //println("CaseDef has guard " + guard + " (cases = " + cases + ")")
                    assert(guard == EmptyTree, guard)
                    for ((sym, subSlice) <- m) {
                      //println("Binding " + sym + " to " + subSlice)
                      setSlice(sym, subSlice)
                    }
                    for (bodySlice <- getTreeSlice(body)) {
                      //println("Set body slice " + bodySlice + " for body " + body)
                      setSlice(tree, bodySlice)
                    }
                  case _ =>
                    assert(false, "Case matching only supports tuples for now (TODO: add (CL)Array(...) case).")
                }
              }
            }
            cases.foreach(traverse(_))
          case TupleComponent(target, i) if target != null =>
            super.traverse(tree)
            val (componentsOffset, componentCount) = getComponentOffsetAndSizeOfIthMember(target.tpe, i)

            //println("Identified tuple component " + i + " of " + target)
            getTreeSlice(target) match {
              case Some(slice) =>
                //println("\ttarget got slice " + slice)
                setSlice(tree, TupleSlice(slice.baseSymbol, componentsOffset, componentCount))
              case _ =>
              //println("No tuple slice symbol info for tuple component i = " + i + " : " + target + "\n\t-> " + nodeToStringNoComment(target))
              //println("\ttree : " + nodeToStringNoComment(tree))
            }
          case Typed(expr, tpt) =>
            super.traverse(tree)
            propagateSlice(expr, tree)
          case Annotated(annot, arg) =>
            super.traverse(tree)
            propagateSlice(arg, tree)
          case _ =>
            super.traverse(tree)
        }
      }
      // TODO: Understand why we can't just use Tree param types: getting error:
      // "Parameter type in structural refinement may not refer to an abstract type defined outside that refinement"
      def propagateSlice(aSource: AnyRef, aDestination: AnyRef) = { //source: Tree, destination: Tree) = {
        val source = aSource.asInstanceOf[Tree]
        val destination = aDestination.asInstanceOf[Tree]
        getTreeSlice(source) match {
          case Some(slice) =>
            setSlice(destination, slice)
          //println("Propagated slice " + slice + " from " + source + " to " + destination)
          case _ =>
        }
      }
    }.traverse(tree)

    //println("treeTupleSlices = \n\t" + treeTupleSlices.mkString("\n\t"))
    //println("symbolTupleSlices = \n\t" + symbolTupleSlices.mkString("\n\t"))

    // 1) Create unique names for unique symbols !
    // 2) Detect external references, lift them up in arguments.
    // 3) Annotate code with usage :
    //    - symbol to Array and CLArray val : read, written, both ?
    //    - extern vars : forbidden
    //    -
    // 4) Flatten tuples and blocks, function definitions arg lists, function calls args
    //
    // Symbol => TupleSlice
    // Tree => TupleSlice
    // e.g. x: ((Double, Float), Int) ; x._1._2 => TupleSlice(x, 1, 1)
    //
    // Tuples flattening :
    // - list tuple definitions
    // - explode each definition unless it's an OpenCL intrinsic :
    //    -> create a new symbol for each split component,
    //    -> map resulting TupleSlice => componentSymbol
    //    -> splitSymbolsTable = Map[Symbol, Seq[(TupleSlice, componentSymbol, componentName)]]
    // - given a Tree, we get an exploded Seq[Tree] + pre-definitions
    //    e.g.:
    //      val a: (Int, Int) = (1, 3)
    //        -> val a1 = 1
    //           val a2 = 3
    //      val a: (Int, Int) = f(x) // special case for int2 : no change
    // We need to propagate slices that are of length > 1 :
    // - arr1.zip(arr2).zipWithIndex.map { case r @ (p @ (a1, a2), i) => p } map { p => p._1 }
    //    -> p => TupleSlice(mapArg, 0, 2)
    // - val (a, b) = p // p is mapped
    //    -> val a = p1 // using splitSymbolsTable
    //       val b = p2
    // Jump over blocks :
    // val p = {
    //  val x = 10
    //  (x, x + 2)
    // }
    // ->
    // val x = 10
    // val p1 = x
    // val p2 = x + 2
    //
    // Each Tree gives a list of statements + a list of split value components :
    // convertTree(tree: Tree): (Seq[Tree], Seq[Tree])
    //
    //
  }
}
