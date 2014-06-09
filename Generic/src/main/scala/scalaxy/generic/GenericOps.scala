package scalaxy.generic

import scala.language.experimental.macros

import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * A dynamic wrapper for generic values that can be optimized away in reified ASTs, and can verify basic union-style static type constraints.
 */
class GenericOps[+A: Generic](val rawValue: A, implicitConversions: List[Any] = Nil) extends Dynamic with NumberOps[A] {

  val value: Any = peel_(rawValue)

  private lazy val targets_ = value :: implicitConversions ++ (value match {
    case (v: java.lang.Byte) => numOps_(v.byteValue)
    case (v: java.lang.Short) => numOps_(v.shortValue)
    case (v: java.lang.Integer) => numOps_(v.intValue)
    case (v: java.lang.Long) => numOps_(v.longValue)
    case (v: java.lang.Float) => numOps_(v.floatValue)
    case (v: java.lang.Double) => numOps_(v.doubleValue)
    case (v: String) => List(v, v: collection.immutable.StringOps)
    case v => Nil
  })

  private lazy val targetMirrors_ = targets_.map(implementation => {
    val mirror = runtimeMirror(Thread.currentThread.getContextClassLoader)
    mirror.reflect(implementation)(ClassTag(implementation.getClass))
  })

  private def valueClassName_ = {
    if (value == null)
      null
    else
      value.getClass.getName
  }

  private def numOps_[N: math.Numeric](v: N): Seq[Any] = {
    val n = implicitly[math.Numeric[N]]
    List(n.mkNumericOps(v), n.mkOrderingOps(v))
  }

  private def peel_(value: Any): Any = {
    if (value.isInstanceOf[GenericOps[_]]) {
      value.asInstanceOf[GenericOps[_]].value
    } else {
      value
    }
  }

  private def findDecl(name: String) = {
    def sub(mirrors: List[InstanceMirror]): Option[(Symbol, InstanceMirror)] = mirrors match {
      case Nil => None
      case mirror :: otherMirrors =>
        val tpe = mirror.symbol.asType.toType
        val symbol =
          tpe.member(TermName(name)) orElse
            tpe.member(TermName(reflect.NameTransformer.encode(name)))
        if (symbol == NoSymbol) { //} || !(mirror.symbol.asType.toType <:< symbol.owner.asType.toType)) {
          // println("No " + name + " or " + reflect.NameTransformer.encode(name) + " in " + mirror.instance.getClass.getName + ": " + tpe + ": " + tpe.members)
          sub(otherMirrors)
        } else {
          // println("FOUND " + name + ": " + symbol.owner + "." + symbol + ", " + mirror)
          // println(s"mirror.symbol.asType.toType = ${mirror.symbol.asType.toType}, symbol.owner.asType.toType = ${symbol.owner.asType.toType}")
          Some(symbol -> mirror)
        }
    }
    sub(targetMirrors_)
  }
  def applyDynamic(name: String)(args: Any*): Any = {
    findDecl(name) match {
      case Some((symbol, mirror)) if symbol.isMethod =>
        val unwrapped = args.map(peel_(_))
        // println("Calling " + name + " on " + mirror.instance)
        mirror.reflectMethod(symbol.asMethod)(unwrapped: _*)
      case _ =>
        throw new NoSuchMethodException("No method '" + name + "' in " + valueClassName_)
    }
  }
  def selectDynamic(name: String): Any = {
    findDecl(name) match {
      case Some((symbol, mirror)) if symbol.isMethod =>
        mirror.reflectMethod(symbol.asMethod)()
      case Some((symbol, mirror)) if symbol.isTerm =>
        mirror.reflectField(symbol.asTerm).get
      case _ =>
        throw new NoSuchFieldException("No field '" + name + "' in " + valueClassName_)
    }
  }
  def updateDynamic(name: String)(value: Any) {
    findDecl(name) match {
      case Some((symbol, mirror)) if symbol.isTerm =>
        mirror.reflectField(symbol.asTerm).set(value)
      case _ =>
        throw new NoSuchFieldException("No field '" + name + "' in " + valueClassName_)
    }
  }

  override def equals(other: Any) = value.equals(other)
  override def hashCode() = value.hashCode()
  override def toString() = String.valueOf(value) //"GenericOps(" + value + ")"
}

trait NumberOps[+A] {
  def +(rhs: A): A = macro internal.methodHomogeneous[A, A]
  def -(rhs: A): A = macro internal.methodHomogeneous[A, A]
  def *(rhs: A): A = macro internal.methodHomogeneous[A, A]
  def /(rhs: A): A = macro internal.methodHomogeneous[A, A]
  def /%(rhs: A): (A, A) = macro internal.methodHomogeneous[A, (A, A)]
  def ==:(rhs: A): Boolean = macro internal.methodHomogeneous[A, Boolean]
  def !=:(rhs: A): Boolean = macro internal.methodHomogeneous[A, Boolean]
  def <=(rhs: A): Boolean = macro internal.methodHomogeneous[A, Boolean]
  def <(rhs: A): Boolean = macro internal.methodHomogeneous[A, Boolean]
  def >=(rhs: A): Boolean = macro internal.methodHomogeneous[A, Boolean]
  def >(rhs: A): Boolean = macro internal.methodHomogeneous[A, Boolean]
  def abs: A = macro internal.method0[A, A]
  def signum: Int = macro internal.method0[A, Int]
  def toInt: Int = macro internal.method0[A, Int]
  def toLong: Long = macro internal.method0[A, Long]
  def toFloat: Float = macro internal.method0[A, Float]
  def toDouble: Double = macro internal.method0[A, Double]
}

object GenericOps {
}
