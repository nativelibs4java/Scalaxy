package scalaxy

import javafx.event._

import scala.language.dynamics
import scala.reflect.macros.Context

package object fx
{
  implicit def fxBeansExtensions[T](bean: T) = new {
    def set = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): T =
        macro internal.applyDynamicNamedImpl[T]
    }
  }
  
  implicit def handler[E <: Event](f: E => Unit): EventHandler[E] = {
    new EventHandler[E]() {
      override def handle(event: E) {
        f(event)
      }
    }
  }
  
  implicit def handler[E <: Event](block: => Unit): EventHandler[E] = {
    new EventHandler[E]() {
      override def handle(event: E) {
        block
      }
    }
  }
}

package fx {
  import scalaxy.beans.internal.BeansMacros
  package object internal extends BeansMacros
  {
    // This needs to be public and statically accessible.
    def applyDynamicNamedImpl[T : c.WeakTypeTag]
      (c: Context)
      (name: c.Expr[String])
      (args: c.Expr[(String, Any)]*) : c.Expr[T] =
    {
      rewriteNamedSetBeanApply[T](c)(name)(args: _*)
    }
      
    override def transformValue
      (c: Context)
      (
        fieldName: String, 
        beanTpe: c.universe.Type, 
        varTpe: c.universe.Type, 
        setterSymbol: c.universe.Symbol, 
        value: c.universe.Tree
      ): c.universe.Tree = 
    {
      import c.universe._
      
      // Implicits will convert functions and blocks to EventHandler.
      if (!(value.tpe weak_<:< varTpe) && !(varTpe <:< typeOf[EventHandler[_]]))
        c.error(value.pos, s"Setter $beanTpe.${setterSymbol.name}($varTpe) does not accept values of type ${value.tpe}")

      value
    }
  }
}
