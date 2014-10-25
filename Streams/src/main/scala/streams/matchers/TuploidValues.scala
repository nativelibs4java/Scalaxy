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
    def unapply(tpe: Type): Boolean =
      Option(tpe).exists(
        _.typeSymbol.fullName.toString.matches("scala\\.Tuple\\d+"))
  }

  object TupleCreation {
    def unapply(tree: Tree): Option[List[Tree]] =
      Option(tree).filter(tree => TupleType.unapply(tree.tpe)) collect {
        case q"${Tuple()}[..${_}](..$subs)" =>
          subs

        case q"${Tuple()}.apply[..${_}](..$subs)" =>
          subs
      }
  }

  type TuploidPath = List[Int]
  val RootTuploidPath = Nil

  def createTuploidPathsExtractionDecls(
    target: Tree,
    paths: Set[TuploidPath],
    fresh: String => TermName,
    typed: Tree => Tree,
    coercionSuccessVarDefRef: (Option[Tree], Option[Tree]) = (None, None))
      : (List[Tree], TuploidValue[Tree]) =
  {
    def aux(tpe: Type, target: Tree, paths: Set[TuploidPath])
        : (List[Tree], List[Tree], TuploidValue[Tree]) = {
      val headToSubs = for ((head, pathsWithSameHead) <- paths.filter(_.nonEmpty).groupBy(_.head)) yield {
        val subPaths = pathsWithSameHead.map(_.tail)
        val selector = "_" + (head + 1)
        val name = fresh(selector)

        val rhs = typed(q"$target.${TermName(selector)}")
        val subTpe = rhs.tpe
        val Block(List(decl, assign), ref) = typed(q"""
          ${newVar(name, subTpe)};
          $name = $rhs;
          $name
        """)

        val (subDecls, subAssigns, subValue) =
          aux(rhs.tpe, ref, subPaths)

        (decl :: subDecls, assign :: subAssigns, head -> subValue)
      }

      val subDecls: List[Tree] = headToSubs.flatMap(_._1).toList
      val subAssigns: List[Tree] = headToSubs.flatMap(_._2).toList
      val assigns: List[Tree] = coercionSuccessVarDefRef match {
        case (Some(successVarDef), Some(successVarRef))
            if subAssigns != Nil =>
          val Block(statements, _) = typed(q"""
            $successVarDef;
            if ((${target.duplicate} ne null) &&
                ${successVarRef.duplicate}) {
              ..$subAssigns
            } else {
              ${successVarRef.duplicate} = false;
            };
            null
          """)

          statements

        case _ =>
          subAssigns
      }

      (
        subDecls,
        assigns,
        TupleValue[Tree](
          tpe = tpe,//target.tpe,
          values = headToSubs.map(_._3).toMap,
          alias = target.asOption,
          couldBeNull = false)
      )
    }

    val (defs, assigns, value) = aux(target.tpe, target, paths)

    val all =
      if (defs.isEmpty && assigns.isEmpty)
        Nil
      else {
        val Block(all, _) = typed(q"""
          ..${defs ++ assigns};
          ""
        """)
        all
      }
    (all, value)
  }

  /** A tuploid value is either a scalar or a tuple of tuploid values. */
  sealed trait TuploidValue[A]
  {
    def collectSet[B](pf: PartialFunction[(TuploidPath, TuploidValue[A]), B]): Set[B] =
      collect(pf).toSet

    def collectMap[B, C](pf: PartialFunction[(TuploidPath, TuploidValue[A]), (B, C)]): Map[B, C] =
      collect(pf).toMap

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

    def collectAliases: Map[TuploidPath, A] =
      collectMap {
        case (path, t) if t.alias.nonEmpty =>
          path -> t.alias.get
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
        case TupleCreation(subs) =>
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
