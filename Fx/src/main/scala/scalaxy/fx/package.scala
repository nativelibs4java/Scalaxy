package scalaxy

import javafx.beans._
import javafx.beans.value._
import javafx.beans.property._
import javafx.beans.binding._
import javafx.event._
import javafx.scene._

import scala.language.dynamics
import scala.reflect.macros.Context

import scalaxy.beans.internal.BeansMacros

trait JavaWrapper[T, J]
  
// TODO: rewrite all calls with macros to avoid dependency to this package object?
package object fx
{
  // This adds `obj.set(property1 = value1, property2 = value2)` to all object types.
  // Properties of type EventHandler[_] benefit from a special type-check to accommodate 
  // implicit conversions below.
  implicit def fxBeansExtensions[T](bean: T) = new {
    def set = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): T =
        macro internal.applyDynamicNamedImpl[T]
    }
  }
  
  implicit object IntWrapper extends JavaWrapper[Int, java.lang.Number]
  
  def bind[T, J](expression: T)(implicit ev: JavaWrapper[T, J]): GenericBinding[T, J] =
    macro internal.bindImpl[T, J]
  
  // Implicit conversion from an event handler function to a JavaFX EventHandler[_].
  implicit def handler[E <: Event](f: E => Unit): EventHandler[E] =
    new FunctionEventHandler(f)
  
  // Implicit conversion from an event handler block to a JavaFX EventHandler[_].
  implicit def handler[E <: Event](block: => Unit): EventHandler[E] =
    new FunctionEventHandler(_ => block)
    
  implicit def doubleProperty(p: DoubleProperty) = 
    macro internal.doublePropertyImpl
    
  implicit def integerProperty(p: IntegerProperty) = 
    macro internal.integerPropertyImpl
    
  implicit def longProperty(p: LongProperty) = 
    macro internal.longPropertyImpl
    
  implicit def floatProperty(p: FloatProperty) = 
    macro internal.floatPropertyImpl
    
  implicit def booleanProperty(p: BooleanProperty) = 
    macro internal.booleanPropertyImpl
}

package fx 
{
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
  
    def bindImpl[T : c.WeakTypeTag, J : c.WeakTypeTag]
      (c: Context)
      (expression: c.Expr[T])
      (ev: c.Expr[JavaWrapper[T, J]]): c.Expr[GenericBinding[T, J]] = 
    {
      import c.universe._
      
      val tpe = weakTypeTag[T].tpe
      val wrapperTpe = weakTypeTag[J].tpe
      val bindingTpe = typeRef(NoType, typeOf[GenericBinding[_, _]].typeSymbol, List(tpe, wrapperTpe))
      
      val bindingName = newTermName(c.fresh("binding"))
      
      val bindingDef = 
        ValDef(
          NoMods, 
          bindingName, 
          TypeTree(bindingTpe),
          Apply(
            {
              val factory = reify(GenericBinding).tree
              if (tpe <:< typeOf[Int])
                Select(factory, "ofInt")
              else if (tpe <:< typeOf[Long]) 
                Select(factory, "ofLong")
              else if (tpe <:< typeOf[Double]) 
                Select(factory, "ofDouble")
              else if (tpe <:< typeOf[Float]) 
                Select(factory, "ofFloat")
              else if (tpe <:< typeOf[Boolean]) 
                Select(factory, "ofBoolean")
              else if (tpe <:< typeOf[String]) 
                Select(factory, "ofString")
              else
                TypeApply(Select(factory, "ofObject"), List(TypeTree(bindingTpe)))
            },
            List(expression.tree)
          )
        )
       
      var observables: List[Tree] = Nil
      (
        new Traverser {
          override def traverse(tree: Tree) = {
            if (tree.tpe <:< typeOf[Observable]) {
              tree match {
                case Ident(n) =>
                  observables = tree :: observables
                case _ =>
                  c.error(tree.pos, "Unsupported observable type")
              }
            }
            super.traverse(tree)
          }
        }
      ).traverse(c.typeCheck(expression.tree))
      
      val observableIdents: List[Tree] = 
        observables.groupBy(_.symbol).map(_._2.head).toList
      println(s"Found the following observables: $observableIdents")
      
      c.Expr[GenericBinding[T, J]](
        Block(
          bindingDef,
          Apply(Select(Ident(bindingName), newTermName("bindObservables")), observableIdents), 
          Ident(bindingName)
        )
      )
    }
    
    def doublePropertyImpl(c: Context)(p: c.Expr[DoubleProperty]): c.Expr[Double] =
      c.universe.reify(p.splice.get)
    
    def floatPropertyImpl(c: Context)(p: c.Expr[FloatProperty]): c.Expr[Float] =
      c.universe.reify(p.splice.get)
    
    def integerPropertyImpl(c: Context)(p: c.Expr[IntegerProperty]): c.Expr[Int] =
      c.universe.reify(p.splice.get)
    
    def longPropertyImpl(c: Context)(p: c.Expr[LongProperty]): c.Expr[Long] =
      c.universe.reify(p.splice.get)
    
    def booleanPropertyImpl(c: Context)(p: c.Expr[BooleanProperty]): c.Expr[Boolean] =
      c.universe.reify(p.splice.get)
  }
}
