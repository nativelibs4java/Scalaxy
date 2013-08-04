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

private[reified] object CommonExtractors {

  object ProductAndClassName {
    val rx = """scala\.(Tuple(\d+))(?:\$.*)?""".r
    def unapply(v: Any): Option[(AnyRef with Product, String, Int)] = v match {
      case p: AnyRef with Product =>
        Option(p.getClass.getName) collect {
          case rx(name, arity) => (p, name, arity.toInt)
        }
      case _ =>
        None
    }
  }

  object HasReifiedValueWrapperTree {
    import scalaxy.reified._

    private def isReifiedValue(tpe: Type) = tpe != null && tpe <:< typeOf[ReifiedValue[_]]
    private def isHasReifiedValue(tpe: Type) = tpe != null && tpe <:< typeOf[HasReifiedValue[_]]
    def unapply(tree: Tree): Option[(Name, Tree)] = {
      val tpe = tree.tpe
      if (isHasReifiedValue(tpe) && !isReifiedValue(tpe)) {
        Option(tree) collect {
          case Apply(Apply(TypeApply(builder, targs), List(value)), implicits) =>
            builder.symbol.name -> value
        }
      } else {
        None
      }

    }
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

  object Step {
    def unapply(treeOpt: Option[Tree]): Option[Int] = Option(treeOpt) collect {
      case Some(Literal(Constant(step: Int))) =>
        step
      case None =>
        1
    }
  }
}
