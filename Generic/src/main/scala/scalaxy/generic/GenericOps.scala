package scalaxy.generic

import scala.language.experimental.macros

import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * A dynamic wrapper for generic values that can be optimized away in reified ASTs, and can verify basic union-style static type constraints.
 */
class GenericOps[+A: Generic](value_ : A, implicitConversions: Any*) extends Dynamic with NumberOps[A] {
  import GenericOps._

  val value = peel_(value_)

  private lazy val targets_ = value :: implicitConversions.toList ++ (value match {
    case (v: java.lang.Byte) => numOps_(v.byteValue)
    case (v: java.lang.Short) => numOps_(v.shortValue)
    case (v: java.lang.Integer) => numOps_(v.intValue)
    case (v: java.lang.Long) => numOps_(v.longValue)
    case (v: java.lang.Float) => numOps_(v.floatValue)
    case (v: java.lang.Double) => numOps_(v.doubleValue)
    case (v: String) => List(v, v: collection.immutable.StringOps)
    case v => Nil
  })

  private[generic] lazy val targetMirrors_ = targets_.map(implementation => {
    val mirror = runtimeMirror(Thread.currentThread.getContextClassLoader)
    mirror.reflect(implementation)(ClassTag(implementation.getClass))
  })

  private[generic] def valueClassName_ = {
    if (value == null)
      null
    else
      value.getClass.getName
  }

  def applyDynamic(name: String)(args: Any*): Any = macro internal.applyDynamic[A]
  def selectDynamic(name: String): Any = macro internal.selectDynamic[A]
  def updateDynamic(name: String)(value: Any): Unit = macro internal.updateDynamic[A]

  override def equals(other: Any) = value.equals(other)
  override def hashCode() = value.hashCode()
  override def toString() = "GenericOps(" + value + ")"
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

  private[generic] def numOps_[N: math.Numeric](v: N): Seq[Any] = {
    val n = implicitly[math.Numeric[N]]
    List(n.mkNumericOps(v), n.mkOrderingOps(v))
  }

  private[generic] def peel_(value: Any): Any = value match {
    case ops: GenericOps[_] => peel_(ops.value)
    case _ => value
  }

  private def findDecl(ops: GenericOps[_])(name: String) = {
    def sub(mirrors: List[InstanceMirror]): Option[(Symbol, InstanceMirror)] = mirrors match {
      case Nil => None
      case mirror :: otherMirrors =>
        val tpe = mirror.symbol.asType.toType
        val symbol =
          tpe.member(name: TermName) orElse tpe.member(reflect.NameTransformer.encode(name): TermName)
        if (symbol == NoSymbol) { //} || !(mirror.symbol.asType.toType <:< symbol.owner.asType.toType)) {
          // println("No " + name + " or " + reflect.NameTransformer.encode(name) + " in " + mirror.instance.getClass.getName + ": " + tpe + ": " + tpe.members)
          sub(otherMirrors)
        } else {
          // println("FOUND " + name + ": " + symbol.owner + "." + symbol + ", " + mirror)
          // println(s"mirror.symbol.asType.toType = ${mirror.symbol.asType.toType}, symbol.owner.asType.toType = ${symbol.owner.asType.toType}")
          Some(symbol -> mirror)
        }
    }
    sub(ops.targetMirrors_)
  }
  def applyDynamicImpl[A](ops: GenericOps[A], name: String, args: Any*): Any = {
    findDecl(ops)(name) match {
      case Some((symbol, mirror)) if symbol.isMethod =>
        val unwrapped = args.map(peel_(_))
        // println("Calling " + name + " on " + mirror.instance)
        mirror.reflectMethod(symbol.asMethod)(unwrapped: _*)
      case _ =>
        throw new NoSuchMethodException("No method '" + name + "' in " + ops.valueClassName_)
    }
  }
  def selectDynamicImpl[A](ops: GenericOps[A], name: String): Any = {
    findDecl(ops)(name) match {
      case Some((symbol, mirror)) if symbol.isMethod =>
        mirror.reflectMethod(symbol.asMethod)()
      case Some((symbol, mirror)) if symbol.isTerm =>
        mirror.reflectField(symbol.asTerm).get
      case _ =>
        throw new NoSuchFieldException("No field '" + name + "' in " + ops.valueClassName_)
    }
  }
  def updateDynamicImpl[A](ops: GenericOps[A], name: String, value: Any) {
    findDecl(ops)(name) match {
      case Some((symbol, mirror)) if symbol.isTerm =>
        mirror.reflectField(symbol.asTerm).set(value)
      case _ =>
        throw new NoSuchFieldException("No field '" + name + "' in " + ops.valueClassName_)
    }
  }
}
