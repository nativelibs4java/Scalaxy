package scalaxy.loops
import scala.collection.breakOut

private[loops] trait TuploidValues extends Utils
{
  val global: scala.reflect.api.Universe
  import global._

  object Tuple {
    def unapply(tree: Tree): Boolean = tree match {
      case q"scala.$n" if n.toString.matches("Tuple\\d+") =>
        true

      case _ =>
        false
    }
  }

  type TuploidPath = List[Int]
  val RootTuploidPath = Nil

  // def tupleTypeComponents(tpe: Type): List[Type] = {
  //   ???
  // }
  def createTuploidPathsExtractionDecls(targetName: TermName, paths: Set[TuploidPath], fresh: String => TermName): (List[Tree], TuploidValue) = {

    // val targetName: TermName = fresh("tup")
    // val targetDecl = q"val $n = $target"
    val headToSubs = for ((head, subPaths) <- paths.filter(_.nonEmpty).groupBy(_.head)) yield {
      val selector = "_" + (head + 1)
      val subName: TermName = fresh(selector)
      val (subExtraction, subValue) = createTuploidPathsExtractionDecls(subName, subPaths, fresh)
      val subDecl: Tree = q"val $subName = $targetName.${selector: TermName}"
      (subDecl :: subExtraction, head -> subValue)
    }

    // val arity = tupleArity(targetType)
    (headToSubs.flatMap(_._1).toList, TupleValue(headToSubs.map(_._2).toMap))
  }

  /** A tuploid value is either a scalar or a tuple of tuploid values. */
  sealed trait TuploidValue {
    def collectTupleAliases: Set[Symbol]
    def collectSymbols: Set[Symbol]
    def find(symbol: Symbol): Option[TuploidPath]
    def get(path: TuploidPath): TuploidValue

    def alias: Symbol
    def aliasName: TermName
  }

  case class ScalarValue(value: Tree = EmptyTree, alias: Symbol = NoSymbol, aliasName: TermName = "")
      extends TuploidValue
  {
    override def collectTupleAliases = Set()

    override def collectSymbols =
      if (alias != NoSymbol)
        Set(alias)
      else
        Set()

    override def find(symbol: Symbol) =
      Option(RootTuploidPath).filter(_ => symbol == alias)

    override def get(path: TuploidPath) = {
      val RootTuploidPath = path
      this
    }
  }

  case class TupleValue(values: Map[Int, TuploidValue], alias: Symbol = NoSymbol, aliasName: TermName = "")
      extends TuploidValue
  {
    private def subSymbolsPlusAlias(getSubs: TuploidValue => Set[Symbol]): Set[Symbol] = {
      val subAliases = values.values.map(getSubs).reduce(_ ++ _)
      if (alias != NoSymbol)
        subAliases + alias
      else
        subAliases
    }

    override def collectTupleAliases: Set[Symbol] =
      subSymbolsPlusAlias(_.collectTupleAliases)

    override def collectSymbols =
      subSymbolsPlusAlias(_.collectSymbols)

    override def find(symbol: Symbol) = {
      if (symbol == alias)
        Some(RootTuploidPath)
      else
        values.toIterator.map {
          case (i, v) =>
            v.find(symbol).map(i :: _)
        } collectFirst {
          case Some(path) =>
            path
        }
    }

    override def get(path: TuploidPath) = path match {
      case RootTuploidPath =>
        this

      case i :: subPath =>
        values(i).get(subPath)
    }
  }

  object TuploidValue {
    def extractValue(tree: Tree, alias: Symbol = NoSymbol): TuploidValue = {
      def sub(subs: List[Tree]): Map[Int, TuploidValue] =
        (subs.zipWithIndex.map {
          case (b @ Bind(_, _), i) =>
            i -> extractValueFromBind(b)

          case (t, i) =>
            i -> extractValue(t)
        })(breakOut)

      tree match {
        case q"$tuple(..$subs)" =>
          TupleValue(values = sub(subs), alias = alias)

        case q"${Tuple()}.apply[..$_](..$subs)" =>
          TupleValue(values = sub(subs), alias = alias)

        case Ident(nme.WILDCARD) =>
          ScalarValue(alias = alias)

        case Ident(n) if tree.symbol.name == n =>
          ScalarValue(alias = tree.symbol)

        case _ =>
          ScalarValue(alias = NoSymbol, value = tree)
      }
    }
    def extractValueFromBind(bind: Bind): TuploidValue = {
      extractValue(bind.body, bind.symbol)
    }

    def unapply(tree: Tree): Option[TuploidValue] =
      trySome(extractValue(tree))
  }

  /** Extract TuploidValue from a CaseDef */
  object CaseTuploidValue {
    def unapply(caseDef: CaseDef): Option[(TuploidValue, Tree)] = {
      def sub(binds: List[Tree]): Map[Int, TuploidValue] =
        binds.zipWithIndex.map({
          case (bind: Bind, i) =>
            i -> TuploidValue.extractValueFromBind(bind)

          case (ident @ Ident(n), i) =>
            i -> ScalarValue(alias = ident.symbol)
        })(breakOut)

      trySome {
        caseDef match {
          case cq"($tuple(..$binds)) => $body" =>
            TupleValue(values = sub(binds), alias = NoSymbol) -> body

          case cq"($alias @ $tuple(..$binds)) => $body" =>
            TupleValue(values = sub(binds), alias = caseDef.pat.symbol) -> body
        }
      }
    }
  }
}
