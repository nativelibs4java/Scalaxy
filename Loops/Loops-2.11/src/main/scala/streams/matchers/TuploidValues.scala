package scalaxy.loops

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

  /** A tuploid value is either a scalar or a tuple of tuploid values. */
  sealed trait TuploidValue {
    def collectTupleAliases: Set[Symbol]
    def collectSymbols: Set[Symbol]
    def find(symbol: Symbol): Option[TuploidPath]
    def get(path: TuploidPath): TuploidValue
  }
  case class ScalarValue(alias: Symbol, value: Tree = EmptyTree)
      extends TuploidValue
  {
    override def collectTupleAliases = Set()

    override def collectSymbols =
      if (alias != NoSymbol)
        Set(alias)
      else
        Set()

    override def find(symbol: Symbol) =
      Option(Nil).filter(_ => symbol == alias)

    override def get(path: TuploidPath) = {
      val Nil = path
      this
    }
  }
  case class TupleValue(values: List[TuploidValue], alias: Symbol = NoSymbol)
      extends TuploidValue
  {
    private def subSymbolsPlusAlias(getSubs: TuploidValue => Set[Symbol]): Set[Symbol] = {
      val subAliases = values.map(getSubs).reduce(_ ++ _)
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
        Some(Nil)
      else
        values.toIterator.zipWithIndex.map {
          case (v, i) =>
            v.find(symbol).map(i :: _)
        } collectFirst {
          case Some(path) =>
            path
        }
    }

    override def get(path: TuploidPath) = path match {
      case Nil =>
        this

      case i :: subPath =>
        values(i).get(subPath)
    }
  }

  object TuploidValue {
    def extractValue(tree: Tree, alias: Symbol = NoSymbol): TuploidValue = {
      def sub(subs: List[Tree]): List[TuploidValue] =
        subs map {
          case b @ Bind(_, _) =>
            extractValueFromBind(b)

          case t =>
            extractValue(t)
        }

      tree match {
        case q"$tuple(..$subs)" =>
          TupleValue(values = sub(subs), alias = alias)

        case q"${Tuple()}.apply[..$_](..$subs)" =>
          TupleValue(values = sub(subs), alias = alias)

        case Ident(nme.WILDCARD) =>
          ScalarValue(alias)

        case Ident(n) if tree.symbol.name == n =>
          ScalarValue(tree.symbol)

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
  object CaseTuploidValue {
    def unapply(caseDef: CaseDef): Option[(TuploidValue, Tree)] = {
      def sub(binds: List[Tree]): List[TuploidValue] =
        binds.map({
          case bind: Bind =>
            TuploidValue.extractValueFromBind(bind)

          case ident @ Ident(n) =>
            ScalarValue(ident.symbol)
        })

      trySome {
        caseDef match {
          case cq"($tuple(..$binds)) => $body" =>
            TupleValue(alias = NoSymbol, values = sub(binds)) -> body

          case cq"($alias @ $tuple(..$binds)) => $body" =>
            TupleValue(alias = caseDef.pat.symbol, values = sub(binds)) -> body
        }
      }
    }
  }
}
