package scalaxy.streams
import scala.collection.breakOut

private[streams] trait TuploidValues extends Utils
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
  object TupleType {
    def unapply(tpe: Type): Boolean = tpe.typeSymbol.fullName.toString.matches("scala\\.Tuple\\d+")
  }

  type TuploidPath = List[Int]
  val RootTuploidPath = Nil

  def createTuploidPathsExtractionDecls(target: Tree, paths: Set[TuploidPath], fresh: String => TermName, typed: Tree => Tree): (List[Tree], TuploidValue[Tree]) = {

    val headToSubs = for ((head, pathsWithSameHead) <- paths.filter(_.nonEmpty).groupBy(_.head)) yield {
      val subPaths = pathsWithSameHead.map(_.tail)
      val selector = "_" + (head + 1)
      val subName = fresh(selector)
      val Block(List(subDecl, subRef), _) = typed(q"""
        private[this] val $subName = $target.${TermName(selector)};
        $subName;
        ""
      """)
      val (subExtraction, subValue) = createTuploidPathsExtractionDecls(subRef, subPaths, fresh, typed)

      (subDecl :: subExtraction, head -> subValue)
    }

    (
      headToSubs.flatMap(_._1).toList,
      TupleValue[Tree](
        tpe = target.tpe,
        values = headToSubs.map(_._2).toMap,
        alias = target.asOption,
        couldBeNull = false)
    )
  }

  /** A tuploid value is either a scalar or a tuple of tuploid values. */
  sealed trait TuploidValue[A]
  {
    def collectSet[B](pf: PartialFunction[(TuploidPath, TuploidValue[A]), B]): Set[B] =
      collect(pf).toSet

    def collect[B](pf: PartialFunction[(TuploidPath, TuploidValue[A]), B]): List[B] = {
      val res = collection.mutable.ListBuffer[B]()
      foreachDefined(pf andThen {
        case a =>
          res += a
      })
      res.result
    }

    def foreachDefined(pf: PartialFunction[(TuploidPath, TuploidValue[A]), Unit]) {
      new TuploidTraverser[A] {
        override def traverse(path: TuploidPath, t: TuploidValue[A]) {
          pf.applyOrElse((path, t), (_: (TuploidPath, TuploidValue[A])) => ())
          super.traverse(path, t)
        }
      } traverse (RootTuploidPath, this)
    }

    def collectAliases: Set[A] =
      collectSet {
        case (path, t) if t.alias.nonEmpty =>
          t.alias.get
      }

    def collectValues: Seq[Tree] =
      collect {
        case (_, ScalarValue(_, Some(t), _)) =>
          t
      }

    def find(target: A): Option[TuploidPath]
    def get(path: TuploidPath): TuploidValue[A]

    def alias: Option[A]
    def tpe: Type
  }

  case class ScalarValue[A](tpe: Type, value: Option[Tree] = None, alias: Option[A] = None)
      extends TuploidValue[A]
  {
    assert((tpe + "") != "Null" && tpe != NoType)
    override def find(target: A) =
      alias.filter(_ == target).map(_ => RootTuploidPath)

    override def get(path: TuploidPath) = {
      val RootTuploidPath = path
      this
    }
  }

  case class TupleValue[A](
    tpe: Type,
    values: Map[Int, TuploidValue[A]],
    alias: Option[A] = None,
    couldBeNull: Boolean = true)
      extends TuploidValue[A]
  {
    assert((tpe + "") != "Null" && tpe != NoType)
    override def find(target: A) = {
      if (alias.exists(_ == target))
        Some(RootTuploidPath)
      else
        values.toIterator.map {
          case (i, v) =>
            v.find(target).map(i :: _)
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

  // object BindList {
  //   def unapply(trees: List[Tree]): Option[List[Bind]] = {
  //     var success = true
  //     val result = trees map {
  //       case b @ Bind(_, _) => b
  //       case _ =>
  //         success = false
  //         null
  //     }
  //     if (success)
  //       Some(result)
  //     else
  //       None
  //   }
  // }

  object MethodTypeTree {
    def unapply(tree: Tree): Option[(List[Symbol], Type)] = tree match {
      case TypeTree() =>
        tree.tpe match {
          case MethodType(params, restpe) =>
            Some(params, restpe)

          case _ =>
            None
        }

      case _ =>
        None
    }
  }

  object TuploidValue {
    def extractSymbols(tree: Tree, alias: Option[Symbol] = None, isInsideCasePattern: Boolean = false): TuploidValue[Symbol] = {
      def sub(subs: List[Tree]): Map[Int, TuploidValue[Symbol]] =
        (subs.zipWithIndex.map {
          case (b @ Bind(_, _), i) =>
            i -> extractSymbolsFromBind(b)

          case (t, i) =>
            i -> extractSymbols(t, isInsideCasePattern = isInsideCasePattern)
        })(breakOut)

      tree match {
        case q"${Tuple()}[..${_}](..$subs)" =>
          TupleValue(tree.tpe, values = sub(subs), alias = alias, couldBeNull = isInsideCasePattern)

        case q"${Tuple()}.apply[..${_}](..$subs)" =>
          TupleValue(tree.tpe, values = sub(subs), alias = alias, couldBeNull = isInsideCasePattern)

        case Ident(termNames.WILDCARD) =>
          ScalarValue(tree.tpe, alias = alias)

        case Ident(n) if tree.symbol.name == n =>
          ScalarValue(tree.tpe, alias = tree.symbol.asOption)

        case Apply(MethodTypeTree(_, restpe @ TupleType()), binds)
            if binds.forall({ case Bind(_, _) => true case _ => false }) =>
          val values = for ((bind: Bind, i) <- binds.zipWithIndex) yield {
            i -> extractSymbolsFromBind(bind)
          }
          TupleValue(restpe, values.toMap)

        case _ =>
          ScalarValue(tree.tpe, value = Some(tree), alias = alias)
      }
    }
    def extractSymbolsFromBind(bind: Bind): TuploidValue[Symbol] = {
      extractSymbols(bind.body, bind.symbol.asOption, isInsideCasePattern = true)
    }

    def unapply(tree: Tree): Option[TuploidValue[Symbol]] =
      trySome(extractSymbols(tree))
  }

  object UnitTreeScalarValue extends ScalarValue[Tree](typeOf[Unit])

  class TuploidTraverser[A] {
    def traverse(path: TuploidPath, t: TuploidValue[A]) {
      t match {
        case TupleValue(_, values, _, _) =>
          for ((i, value) <- values) {
            traverse(path :+ i, value)
          }

        case _ =>
      }
    }
  }

  trait TuploidTransformer[A, B] {
    def transform(path: TuploidPath, t: TuploidValue[A]): TuploidValue[B]
  }

  /** Extract TuploidValue from a CaseDef */
  object CaseTuploidValue {
    def unapply(caseDef: CaseDef): Option[(TuploidValue[Symbol], Tree)] = {
      def sub(binds: List[Tree]): Map[Int, TuploidValue[Symbol]] =
        binds.zipWithIndex.map({
          case (b, i) =>
            i -> (b match {
              case bind: Bind =>
                TuploidValue.extractSymbolsFromBind(bind)

              case ident @ Ident(n) =>
                ScalarValue[Symbol](tpe = ident.tpe, alias = ident.symbol.asOption)

              case TuploidValue(v) =>
                v
            })
        })(breakOut)

      require(caseDef.tpe != null && caseDef.tpe != NoType)
      tryOrNone {
        Option(caseDef) collect {
          case cq"($tuple(..$binds)) => $body" if TupleType.unapply(caseDef.pat.tpe) =>
            TupleValue(
              tpe = caseDef.pat.tpe,
              values = sub(binds), alias = None) -> body

          case cq"($alias @ $tuple(..$binds)) => $body" if TupleType.unapply(caseDef.pat.tpe) =>
            TupleValue(
              tpe = caseDef.pat.tpe,
              values = sub(binds), alias = caseDef.pat.symbol.asOption) -> body
        }
      }
    }
  }
}
