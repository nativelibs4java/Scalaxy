package scalaxy.fx
package impl

import javafx.beans.value.ObservableValue
import javafx.event.EventHandler

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.NameTransformer
import scala.reflect.macros.Context

private[fx] object BeanExtensionMacros
{
  /** This needs to be public and statically accessible. */
  def applyDynamicNamedImpl
      [T : c.WeakTypeTag]
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
    }

    // Get the bean.
    val Select(Apply(_, List(bean)), _) = c.prefix.tree//c.typeCheck(c.prefix.tree)

    // Choose a non-existing name for our bean's val.
    val beanName = newTermName(c.fresh("bean"))

    // Create a declaration for our bean's val.
    val beanDef = ValDef(NoMods, beanName, TypeTree(bean.tpe), bean)

    // Try to find a setter in the bean type that can take values of the type we've got.
    def getSetter(name: String) = {
      bean.tpe.member(newTermName(name))
        .filter(s => s.isMethod && s.asMethod.paramss.flatten.size == 1)
    }

    // Try to find a setter in the bean type that can take values of the type we've got.
    def getVarTypeFromSetter(s: Symbol) = {
      val Seq(param) = s.asMethod.paramss.flatten
      param.typeSignature
    }

    val values = args.map(_.tree).map {
      // Match Tuple2.apply[String, Any](fieldName, value).
      case Apply(_, List(Literal(Constant(fieldName: String)), value)) =>
        (fieldName, value)
    }

    // Forbid duplicates.
    for ((fieldName, dupes) <- values.groupBy(_._1); if dupes.size > 1) {
      for ((_, value) <- dupes.drop(1))
        c.error(value.pos, s"Duplicate value for property '$fieldName'")
    }

    // Generate one setter call per argument.
    val setterCalls = values map
    {
      case (fieldName, value) =>

        val valueTpe = value.tpe.normalize//.widen

        // Check that all parameters are named.
        if (fieldName == null || fieldName == "")
          c.error(value.pos, "Please use named parameters.")

        //println(s"fieldName = $fieldName, valueTpe.typeSymbol = ${valueTpe.typeSymbol}; value = $value")
        //if (valueTpe weak_<:< g
        if (valueTpe <:< typeOf[ObservableValue[_]]) {
          val propertyName = newTermName(fieldName + "Property")
          val propertySymbol =
            bean.tpe.member(propertyName)
              .filter(s => s.isMethod && s.asMethod.paramss.flatten.isEmpty)

          if (propertySymbol == NoSymbol) {
            c.error(value.pos, s"Couldn't find a property getter $propertyName for property '$fieldName' in type ${bean.tpe}")
          }

          Apply(
            Select(
              Select(Ident(beanName), propertyName),
              TermName("bind")
            ),
            List(value)
          )
        } else {
          // Get beans-style setter or Scala-style var setter.
          val setterSymbol =
            getSetter("set" + fieldName.capitalize)
              .orElse(getSetter(fieldName))
                .orElse(getSetter(NameTransformer.encode(fieldName + "_=")))

          if (setterSymbol == NoSymbol) {
            c.error(value.pos, s"Couldn't find a setter for property '$fieldName' in type ${bean.tpe}")
          }
          val varTpe = getVarTypeFromSetter(setterSymbol)
          // Implicits will convert functions and blocks to EventHandler.
          if (!(valueTpe weak_<:< varTpe) && !(varTpe <:< typeOf[EventHandler[_]]))
            c.error(value.pos, s"Setter ${bean.tpe}.${setterSymbol.name}($varTpe) does not accept values of type ${valueTpe}")

          Apply(Select(Ident(beanName), setterSymbol), List(value))
        }
    }
    // Build a block with the bean declaration, the setter calls and return the bean.
    c.Expr[T](
      Block(List(beanDef) ++ setterCalls, Ident(beanName))
    )
  }
}
