trait TuploidValues extends Utils
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
            println("WHAT THE HELL IS A RAW IDENT DOING IN A CASEDEF? " + ident)
            ScalarValue(ident.symbol)
        })

      trySome {
        caseDef match {
          case cq"($tuple(..$binds)) => $body" =>
            TupleValue(alias = NoSymbol, values = sub(binds)) -> body

          case cq"($alias @ $tuple(..$binds)) => $body" =>
            println(s"caseDef.pat.symbol = ${caseDef.pat.symbol}")
            TupleValue(alias = caseDef.pat.symbol, values = sub(binds)) -> body
        }
      }
    }
  }

  private object Idents {
    def unapply(trees: List[Tree]): Option[List[Ident]] = {
      val idents = trees collect {
        case id @ Ident(_) => id
      }
      Option(idents).filter(_.size == trees.size)
    }
  }

  private object ValDefs {
    def unapply(trees: List[Tree]): Option[List[ValDef]] = {
      val valDefs = trees collect {
        case id @ ValDef(_) => id
      }
      Option(valDefs).filter(_.size == trees.size)
    }
  }

  case class TransformationClosure(
      inputs: TuploidValue,
      statements: List[Tree],
      outputs: TuploidValue)
  {
    def hasTupleAliasReferencesInStatements = {
      val tupleAliases = inputs.collectTupleAliases
      statements.exists(_.exists(t => tupleAliases.contains(t.symbol)))
    }
  }

  object TransformationClosure {
    def unapply(closure: Tree): Option[TransformationClosure] = {

      val inputValueAndBodyOpt = closure match {
        case q"""($param) => (${paramRef @ Ident(_)}: $_) match { case $singleCase }"""
            if param.name == paramRef.name =>
          singleCase match {
            case CaseTuploidValue(inputValue, body) =>
              Some((inputValue, body))

            case _ =>
              None
          }

        case q"""($param) => $body""" =>
          Some((ScalarValue(param.symbol), body))

        case _ =>
          println("COULDN'T RECOGNIZE closure: " + closure)
          None
      }
      inputValueAndBodyOpt match {
        case Some((inputValue, Block(statements, returnValue))) =>
          returnValue match {
            case TuploidValue(outputValue) =>
              val result = TransformationClosure(inputValue, statements, outputValue)
              Option(result).filterNot(_.hasTupleAliasReferencesInStatements)

            case _ =>
              println("COULDN'T RECOGNIZE return value of body: " + returnValue)
              None
          }

        case Some((inputValue, body)) =>
          println("COULDN'T RECOGNIZE TuploidValue case: (inputValue = " + inputValue + "): " + body)
          None

        case _ =>
          None
      }
    }
  }
  def parseTupleRewiringMapClosure(closure: Tree)
      : Option[(TuploidValue, List[Tree], TuploidValue)] = {

    val inputValueAndBodyOpt = closure match {
      case q"""($param) => (${paramRef @ Ident(_)}: $_) match { case $singleCase }"""
          if param.name == paramRef.name =>
        singleCase match {
          case CaseTuploidValue(inputValue, body) =>
            Some((inputValue, body))

          case _ =>
            None
        }

      case q"""($param) => $body""" =>
        Some((ScalarValue(param.symbol), body))

      case _ =>
        println("COULDN'T RECOGNIZE closure: " + closure)
        None
    }
    inputValueAndBodyOpt match {
      case Some((inputValue, Block(statements, returnValue))) =>
        println("returnValue = " + returnValue)
        val tupleAliases = inputValue.collectTupleAliases

        val hasTupleAliasReferencesInStatements =
          statements.exists(_.exists(t => tupleAliases.contains(t.symbol)))

        if (hasTupleAliasReferencesInStatements) {
          // println("HAS TUPLE ALIAS REFERENCED IN VALDEFS: " + valDefs)
          None
        } else {
          returnValue match {
            case TuploidValue(outputValue) =>
              Some((inputValue, statements, outputValue))

            case _ =>
              println("COULDN'T RECOGNIZE return value of body: " + returnValue)
              None
          }
        }

      case Some((inputValue, body)) =>
        println("COULDN'T RECOGNIZE TuploidValue case: (inputValue = " + inputValue + "): " + body)
        None

      case _ =>
        None
    }
  }
}
