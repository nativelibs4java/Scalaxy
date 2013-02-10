package scalaxy.fx

import scala.reflect.NameTransformer
import scala.reflect.macros.Context

import scala.reflect.ClassTag
import javafx.beans._
import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._

trait GenericType[@specialized(Int, Long, Float, Double) T, J, B <: Binding[J], P <: Property[J]] {
  def newBinding(value: () => T, observables: Observable*): B
}

object GenericType {
  def newPropertyImpl
      [T : c.WeakTypeTag, P : c.WeakTypeTag]
      (c: Context)
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[P] = 
  {
    import c.universe._
    c.Expr[P](New(TypeTree(weakTypeTag[P].tpe), Nil))
  }
  
  def propertyValueImpl
      [T : c.WeakTypeTag, P : c.WeakTypeTag]
      (c: Context)
      (p: c.Expr[P])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[T] = 
  {
    import c.universe._
    c.Expr[T](Select(c.typeCheck(p.tree), "get"))
  }
    
  def bindingValueImpl
      [T : c.WeakTypeTag, B : c.WeakTypeTag]
      (c: Context)
      (b: c.Expr[B])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[T] = 
  {
    import c.universe._
    c.Expr[T](Select(c.typeCheck(b.tree), "get"))
  }
}

trait GenericTypes {
  implicit object GenericIntType extends GenericType[Int, Number, IntegerBinding, SimpleIntegerProperty] {
    override def newBinding(value: () => Int, observables: Observable*) = new IntegerBinding {
      super.bind(observables: _*)
      override def computeValue = value()
    }
  }
  implicit object GenericLongType extends GenericType[Long, Number, LongBinding, SimpleLongProperty] {
    override def newBinding(value: () => Long, observables: Observable*) = new LongBinding {
      super.bind(observables: _*)
      override def computeValue = value()
    }
  }
  implicit object GenericFloatType extends GenericType[Float, Number, FloatBinding, SimpleFloatProperty] {
    override def newBinding(value: () => Float, observables: Observable*) = new FloatBinding {
      super.bind(observables: _*)
      override def computeValue = value()
    }
  }
  implicit object GenericDoubleType extends GenericType[Double, Number, DoubleBinding, SimpleDoubleProperty] {
    override def newBinding(value: () => Double, observables: Observable*) = new DoubleBinding {
      super.bind(observables: _*)
      override def computeValue = value()
    }
  }
  implicit object GenericBooleanType extends GenericType[Boolean, java.lang.Boolean, BooleanBinding, SimpleBooleanProperty] {
    override def newBinding(value: () => Boolean, observables: Observable*) = new BooleanBinding {
      super.bind(observables: _*)
      override def computeValue = value()
    }
  }
  implicit object GenericStringType extends GenericType[String, String, StringBinding, SimpleStringProperty] {
    override def newBinding(value: () => String, observables: Observable*) = new StringBinding {
      super.bind(observables: _*)
      override def computeValue = value()
    }
  }
  implicit def GenericObjectType[T <: AnyRef] = new GenericType[T, T, Binding[T], SimpleObjectProperty[T]] {
    override def newBinding(value: () => T, observables: Observable*) = new ObjectBinding[T] {
      super.bind(observables: _*)
      override def computeValue = value()
    }
  }
  
  implicit def newProperty
      [T, J, B <: Binding[J], P <: Property[J]]
      (implicit ev: GenericType[T, J, B, P]): P = 
    macro GenericType.newPropertyImpl[T, P]
  
  implicit def propertyValue
      [T, J, B <: Binding[J], P <: Property[J]]
      (p: P)
      (implicit ev: GenericType[T, J, B, P]): T = 
    macro GenericType.propertyValueImpl[T, P]
    
  implicit def bindingValue
      [T, J, B <: Binding[J], P <: Property[J]]
      (b: B)
      (implicit ev: GenericType[T, J, B, P]): T =  
    macro GenericType.bindingValueImpl[T, B]
}
