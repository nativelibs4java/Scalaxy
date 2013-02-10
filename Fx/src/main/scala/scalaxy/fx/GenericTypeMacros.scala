package scalaxy.fx

import scala.language.experimental.macros
import scala.reflect.macros.Context

import javafx.beans._
import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._

// Implementation of macros from GenericTypes.
object GenericTypeMacros
{
  def newProperty
      [T : c.WeakTypeTag, P : c.WeakTypeTag]
      (c: Context)
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[P] = 
  {
    import c.universe._
    c.Expr[P](New(TypeTree(weakTypeTag[P].tpe), Nil))
  }
  
  def newBinding
      [T : c.WeakTypeTag, B : c.WeakTypeTag]
      (c: Context)
      (value: c.Expr[T], observables: c.Expr[Observable]*)
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[B] = 
  {
    import c.universe._
    
    val valueTpe = weakTypeTag[T].tpe
    val superBindCall = c.Expr[Unit](
      Apply(
        Select(
          Super(This(tpnme.EMPTY), tpnme.EMPTY), 
          newTermName("bind")
        ), 
        observables.toList.map(_.tree)
      )
    )
      
    (
      if (valueTpe =:= typeOf[Int])
        reify(new IntegerBinding {
           superBindCall.splice
           override def computeValue = value.asInstanceOf[c.Expr[Int]].splice
        })
      else if (valueTpe =:= typeOf[Double])
        reify(new DoubleBinding {
           superBindCall.splice
           override def computeValue = value.asInstanceOf[c.Expr[Double]].splice
        })
      else {
        println(s"c.prefix = ${c.prefix}")
        sys.error(s"Not implemented for type $valueTpe")
      }
    ).asInstanceOf[c.Expr[B]]
  }
  
  def propertyValue
      [T : c.WeakTypeTag, P : c.WeakTypeTag]
      (c: Context)
      (p: c.Expr[P])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[T] = 
  {
    import c.universe._
    c.Expr[T](Select(c.typeCheck(p.tree), "get"))
  }
    
  def bindingValue
      [T : c.WeakTypeTag, B : c.WeakTypeTag]
      (c: Context)
      (b: c.Expr[B])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[T] = 
  {
    import c.universe._
    c.Expr[T](Select(c.typeCheck(b.tree), "get"))
  }
}
