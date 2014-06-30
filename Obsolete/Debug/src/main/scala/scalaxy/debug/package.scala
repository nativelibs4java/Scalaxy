package scalaxy

import scala.language.dynamics
import scala.language.experimental.macros

import scala.reflect.ClassTag
import scala.reflect.NameTransformer.encode
import scala.reflect.macros.blackbox.Context

package object debug
{
  def assert(condition: Boolean): Unit =
    macro impl.assertImpl

  def require(condition: Boolean): Unit =
    macro impl.requireImpl

  def assume(condition: Boolean): Unit =
    macro impl.assumeImpl
}

package debug
{
  object impl
  {
    def assertImpl(c: Context)(condition: c.Expr[Boolean]): c.Expr[Unit] =
    {
      import c.universe._

      assertLikeImpl(c)(condition, (condExpr, messageExpr) => {
        reify(Predef.assert(condExpr.splice, messageExpr.splice))
      })
    }

    def requireImpl(c: Context)(condition: c.Expr[Boolean]): c.Expr[Unit] =
    {
      import c.universe._

      assertLikeImpl(c)(condition, (condExpr, messageExpr) => {
        reify(Predef.require(condExpr.splice, messageExpr.splice))
      })
    }

    def assumeImpl(c: Context)(condition: c.Expr[Boolean]): c.Expr[Unit] =
    {
      import c.universe._

      assertLikeImpl(c)(condition, (condExpr, messageExpr) => {
        reify(Predef.assume(condExpr.splice, messageExpr.splice))
      })
    }

    def assertLikeImpl
        (c: Context)
        (
          condition: c.Expr[Boolean],
          callBuilder: (c.Expr[Boolean], c.Expr[String]) => c.Expr[Unit]
        ): c.Expr[Unit] =
    {
      import c.universe._

      def newValDef(name: String, rhs: Tree, tpe: Type = null) = {
        ValDef(
          NoMods,
          TermName(c.freshName(name)),
          TypeTree(Option(tpe).getOrElse(rhs.tpe.dealias)),
          rhs
        )
      }

      object EqualityOpName {
        def unapply(name: Name): Option[Boolean] = {
          val s = name.toString
          if (s == encode("==")) Some(true)
          else if (s == encode("!=")) Some(false)
          else None
        }
      }

      def isConstant(tree: Tree) = tree match {
        case Literal(Constant(_)) => true
        case _ => false
      }

      val typedCondition = c.typecheck(condition.tree)//, typeOf[Boolean])
      c.Expr[Unit](
        typedCondition match
        {
          case Apply(Select(left, op @ EqualityOpName(isEqual)), List(right)) =>
            val leftDef = newValDef("left", left)
            val rightDef = newValDef("right", right)

            val leftExpr = c.Expr[Any](Ident(leftDef.name))
            val rightExpr = c.Expr[Any](Ident(rightDef.name))

            val rels = ("==", "!=")
            val (expectedRel, actualRel) = if (isEqual) rels else rels.swap
            val actualRelExpr = q"$actualRel"
            val str = c.literal(s"$left $expectedRel $right")
            val condExpr = c.Expr[Boolean](
              Apply(Select(Ident(leftDef.name), op), List(Ident(rightDef.name)))
            )
            Block(
              leftDef,
              rightDef,
              if (isEqual)
                callBuilder(
                  condExpr,
                  c.Expr(q""" ${""} + $str + " (" + $leftExpr + " " + $actualRelExpr + " " + $rightExpr + ")" """)
                  // c.Expr[String](q""" "%s (%s %s %s)".format($str, $leftExpr, $actualRelExpr, $rightExpr)""")
                  // reify(s"${str.splice} (${leftExpr.splice} ${actualRelExpr.splice} ${rightExpr.splice})")
                ).tree
              else if (isConstant(left) || isConstant(right))
                callBuilder(condExpr, str).tree
              else
                callBuilder(condExpr, reify(s"${str.splice} (== ${leftExpr.splice})")).tree
            )
          case _ =>
            val condDef = newValDef("cond", typedCondition)
            val condExpr = c.Expr[Boolean](Ident(condDef.name))
            val str = c.Expr[String](
              if (isConstant(typedCondition))
                q""" "Always false!" """
              else
                q"${typedCondition.toString}"
            )
            Block(
              condDef,
              callBuilder(condExpr, str).tree
            )
        }
      )
    }
  }
}
