package scalaxy.fx

import javafx.beans._
import javafx.beans.binding._

trait GenericBinding[T, J] extends Binding[J] {
  def apply(): T
  def bindObservables(observables: Observable*): Unit
}

object GenericBinding {
  def ofInt(value: => Int) = new IntegerBinding with GenericBinding[Int, java.lang.Number] {
    override def apply() = get
    override def computeValue = value 
    override def bindObservables(observables: Observable*) = super.bind(observables: _*)
  }
  def ofLong(value: => Long) = new LongBinding with GenericBinding[Long, java.lang.Number] {
    override def apply() = get
    override def computeValue = value 
    override def bindObservables(observables: Observable*) = super.bind(observables: _*)
  }
  def ofDouble(value: => Double) = new DoubleBinding with GenericBinding[Double, java.lang.Number] {
    override def apply() = get
    override def computeValue = value 
    override def bindObservables(observables: Observable*) = super.bind(observables: _*)
  }
  def ofFloat(value: => Float) = new FloatBinding with GenericBinding[Float, java.lang.Number] {
    override def apply() = get
    override def computeValue = value 
    override def bindObservables(observables: Observable*) = super.bind(observables: _*)
  }
  def ofBoolean(value: => Boolean) = new BooleanBinding with GenericBinding[Boolean, java.lang.Boolean] {
    override def apply() = get
    override def computeValue = value 
    override def bindObservables(observables: Observable*) = super.bind(observables: _*)
  }
  def ofString(value: => String) = new StringBinding with GenericBinding[String, java.lang.String] {
    override def apply() = get
    override def computeValue = value 
    override def bindObservables(observables: Observable*) = super.bind(observables: _*)
  }
  def ofObject[T](value: => T) = new ObjectBinding[T] with GenericBinding[T, T] {
    override def apply() = get
    override def computeValue = value 
    override def bindObservables(observables: Observable*) = super.bind(observables: _*)
  }
}
