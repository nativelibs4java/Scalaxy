package scalaxy

import scala.language.dynamics
import scala.language.experimental.macros

import scala.reflect.ClassTag
import scala.reflect.NameTransformer.encode
import scala.reflect.macros.Context

package object debug {
  def assert(condition: Boolean): Unit =
    macro impl.assertImpl
}

package debug {
  object impl {
    def assertImpl(c: Context)(condition: c.Expr[Boolean]): c.Expr[Unit] = 
    {
      import c.universe._
      
      def newValDef(name: String, rhs: Tree, tpe: Type = null) =
        ValDef(NoMods, newTermName(c.fresh(name)), TypeTree(Option(tpe).getOrElse(rhs.tpe.normalize)), rhs)

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
      
      val typedCondition = c.typeCheck(condition.tree, typeOf[Boolean])
      c.Expr[Unit](
        typedCondition match 
        {
          case Apply(Select(left, op @ EqualityOpName(isEqual)), List(right)) =>
            val leftDef = newValDef("left", left)
            val rightDef = newValDef("right", right)

            val leftExpr = c.Expr[Any](Ident(leftDef.name))
            val rightExpr = c.Expr[Any](Ident(rightDef.name))
            
            val actualRel = if (isEqual) "!=" else "=="
            val actualRelExpr = c.literal(actualRel)
            val str = c.literal(s"$left $actualRel $right")
            val condExpr = c.Expr[Boolean](
              Apply(Select(Ident(leftDef.name), op), List(Ident(rightDef.name)))
            )
            Block(
              leftDef,
              rightDef,
              if (isEqual)
                reify(
                  Predef.assert(
                    condExpr.splice, 
                    s"${str.splice} (${leftExpr.splice} ${actualRelExpr.splice} ${rightExpr.splice})"
                  )
                ).tree
              else if (isConstant(left) || isConstant(right))
                reify(
                  Predef.assert(condExpr.splice, str.splice)
                ).tree
              else
                reify(
                  Predef.assert(condExpr.splice, s"${str.splice} (== ${leftExpr.splice})")
                ).tree
            )
          case _ =>
            val condDef = newValDef("cond", typedCondition)
            val condExpr = c.Expr[Boolean](Ident(condDef.name))
            val str = 
              if (isConstant(typedCondition))
                c.literal("Always false!")
              else
                c.literal(typedCondition + " == false")
            Block(
              condDef,
              reify(
                Predef.assert(
                  condExpr.splice,
                  str.splice
                )
              ).tree
            )
        }
      )
    }
  }
}
