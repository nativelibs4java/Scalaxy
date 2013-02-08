package scalaxy

import scala.language.dynamics
import scala.reflect.ClassTag
import scala.reflect.NameTransformer
import scala.reflect.macros.Context

/**
  Syntactic sugar to instantiate Java beans with a very Scala-friendly syntax.

  The following expression:

    import scalaxy.beans

    beans.create[MyBean](
      foo = 10,
      bar = 12
    )

  Gets replaced (and type-checked) at compile time by:

    {
      val bean = new MyBean
      bean.setFoo(10)
      bean.setBar(12)
      bean
    }

  Doesn't bring any runtime dependency (macro is self-erasing).
  Don't expect code completion from your IDE as of yet.
*/
package object beans
{
  implicit def beansExtensions[T](bean: T) = new {
    def set = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): T =
        macro _beans_internal_.applyDynamicNamedImpl[T]
    }
  }
}

// This needs to be public and statically accessible.
object _beans_internal_
{
  def applyDynamicNamedImpl[T : c.WeakTypeTag]
    (c: Context)
    (name: c.Expr[String])
    (args: c.Expr[(String, Any)]*) : c.Expr[T] =
  {
    import c.universe._

    // Check that the method name is "create".
    name.tree match {
      case Literal(Constant(n)) =>
        if (n != "apply")
          c.error(name.tree.pos, s"Expected 'apply', got '$n'")
      case _ =>
        c.error(name.tree.pos, "Unexpected name structure error")
    }

    // Get the bean.
    val bean = (
      c.typeCheck(c.prefix.tree) match {
        case Select(Apply(_, List(bean)), _) =>
          Some(bean)
        case _ =>
          c.error(c.prefix.tree.pos, "Unexpected prefix structure error")
          None
      }
    ).get

    // Choose a non-existing name for our bean's val.
    val beanName =
      newTermName(c.fresh("bean"))

    // Create a declaration for our bean's val.
    val beanDef =
      ValDef(Modifiers(), beanName, TypeTree(bean.tpe), bean)

    // Try to find a setter in the bean type that can take values of the type we've got.
    def getSetter(name: String, valueTpe: Type, valuePos: Position) = {
      bean.tpe.member(newTermName(name)).filter {
        case s =>
          s.isMethod && (
            s.asMethod.paramss.flatten match {
              case Seq(param) =>
                // Check that the parameter can be assigned a convertible type.
                // (typeOf[Int] weak_<:< typeOf[Double]) == true.
                if (!(valueTpe weak_<:< param.typeSignature))
                  c.error(valuePos, s"Value of type $valueTpe cannot be set with ${bean.tpe}.$name(${param.typeSignature})")
                true
              case _ =>
                false
            }
          )
      }
    }

    // Generate one setter call per argument.
    val setterCalls = args.map(_.tree).map 
    {
      // Match Tuple2.apply[String, Any](fieldName, value).
      case Apply(_, List(n @ Literal(Constant(fieldName: String)), v @ value)) =>

        // Check that all parameters are named.
        if (fieldName == null || fieldName == "")
          c.error(v.pos, "Please use named parameters.")

        // Get beans-style setter or Scala-style var setter.
        val setterSymbol =
          getSetter("set" + fieldName.capitalize, value.tpe, v.pos)
            .orElse(getSetter(NameTransformer.encode(fieldName + "_="), value.tpe, v.pos))

        if (setterSymbol == NoSymbol)
          c.error(n.pos, s"Couldn't find a setter for field '$fieldName' in type ${bean.tpe}")

        Apply(Select(Ident(beanName), setterSymbol), List(value))
    }
    // Build a block with the bean declaration, the setter calls and return the bean.
    c.Expr[T](Block(Seq(beanDef) ++ setterCalls :+ Ident(beanName): _*))
  }

  
}
