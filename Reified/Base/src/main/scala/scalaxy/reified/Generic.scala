package scalaxy.reified

import scala.language.experimental.macros
import scala.reflect.macros.Context

/**
 * foo.method("blah")      ~~> foo.applyDynamic("method")("blah")
 * foo.method(x = "blah")  ~~> foo.applyDynamicNamed("method")(("x", "blah"))
 * foo.method(x = 1, 2)    ~~> foo.applyDynamicNamed("method")(("x", 1), ("", 2))
 * foo.field           ~~> foo.selectDynamic("field")
 * foo.varia = 10      ~~> foo.updateDynamic("varia")(10)
 * foo.arr(10) = 13    ~~> foo.selectDynamic("arr").update(10, 13)
 * foo.arr(10)         ~~> foo.applyDynamic("arr")(10)
 */

import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Type used to model constraint alternatives in Generic
 */
trait |[A, B]

/**
 * A dynamic wrapper for generic values that can be optimized away in reified ASTs, and can verify basic union-style static type constraints.
 */
class Generic[A, ConstraintOnA: TypeTag](val value: A, implicitConversions: Any*) extends Dynamic {

  private def numOps_[N: Numeric](v: N): Seq[Any] = {
    val n = implicitly[Numeric[N]]
    List(v, n.mkNumericOps(v), n.mkOrderingOps(v))
  }

  // checkStaticConstraint_()
  // private def checkStaticConstraint_() = macro Generic.checkStaticConstraint[A, ConstraintOnA]

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

  // println("Implementations for " + value + ": " + targets_.mkString(", "))

  private[reified] lazy val targetMirrors_ = targets_.map(implementation => {
    val mirror = runtimeMirror(Thread.currentThread.getContextClassLoader)
    mirror.reflect(implementation)(ClassTag(implementation.getClass))
  })

  private[reified] def valueClassName_ = {
    if (value == null)
      null
    else
      value.getClass.getName
  }

  def applyDynamic(name: String)(args: Any*): Any = macro internal.genericApplyDynamic[A, ConstraintOnA]
  def selectDynamic(name: String): Any = macro internal.genericSelectDynamic[A, ConstraintOnA]
  def updateDynamic(name: String)(value: Any): Unit = macro internal.genericUpdateDynamic[A, ConstraintOnA]

  // Special case for numeric operations, to stay in callee's type
  def +(rhs: Generic[A, ConstraintOnA]): A = macro internal.genericMethodHomogeneous[A, ConstraintOnA, A]
  def -(rhs: Generic[A, ConstraintOnA]): A = macro internal.genericMethodHomogeneous[A, ConstraintOnA, A]
  def *(rhs: Generic[A, ConstraintOnA]): A = macro internal.genericMethodHomogeneous[A, ConstraintOnA, A]
  def /(rhs: Generic[A, ConstraintOnA]): A = macro internal.genericMethodHomogeneous[A, ConstraintOnA, A]
  def /%(rhs: Generic[A, ConstraintOnA]): (A, A) = macro internal.genericMethodHomogeneous[A, ConstraintOnA, (A, A)]
  def ==(rhs: Generic[A, ConstraintOnA]): Boolean = macro internal.genericMethodHomogeneous[A, ConstraintOnA, Boolean]
  def !=(rhs: Generic[A, ConstraintOnA]): Boolean = macro internal.genericMethodHomogeneous[A, ConstraintOnA, Boolean]
  def <=(rhs: Generic[A, ConstraintOnA]): Boolean = macro internal.genericMethodHomogeneous[A, ConstraintOnA, Boolean]
  def <(rhs: Generic[A, ConstraintOnA]): Boolean = macro internal.genericMethodHomogeneous[A, ConstraintOnA, Boolean]
  def >=(rhs: Generic[A, ConstraintOnA]): Boolean = macro internal.genericMethodHomogeneous[A, ConstraintOnA, Boolean]
  def >(rhs: Generic[A, ConstraintOnA]): Boolean = macro internal.genericMethodHomogeneous[A, ConstraintOnA, Boolean]
  def abs: A = macro internal.genericMethod0[A, ConstraintOnA, A]
  def signum: Int = macro internal.genericMethod0[A, ConstraintOnA, Int]
  def toInt: Int = macro internal.genericMethod0[A, ConstraintOnA, Int]
  def toLong: Long = macro internal.genericMethod0[A, ConstraintOnA, Long]
  def toFloat: Float = macro internal.genericMethod0[A, ConstraintOnA, Float]
  def toDouble: Double = macro internal.genericMethod0[A, ConstraintOnA, Double]

  // def +(rhs: Generic[A, ConstraintOnA]): A = applyDynamic("+")(rhs).asInstanceOf[A]
  // def -(rhs: Generic[A, ConstraintOnA]): A = applyDynamic("-")(rhs).asInstanceOf[A]
  // def *(rhs: Generic[A, ConstraintOnA]): A = applyDynamic("*")(rhs).asInstanceOf[A]
  // def /(rhs: Generic[A, ConstraintOnA]): A = applyDynamic("/")(rhs).asInstanceOf[A]
  // def /%(rhs: Generic[A, ConstraintOnA]): (A, A) = applyDynamic("/%")(rhs).asInstanceOf[(A, A)]
  // def ==(rhs: Generic[A, ConstraintOnA]): Boolean = applyDynamic("==")(rhs).asInstanceOf[Boolean]
  // def !=(rhs: Generic[A, ConstraintOnA]): Boolean = applyDynamic("!=")(rhs).asInstanceOf[Boolean]
  // def <=(rhs: Generic[A, ConstraintOnA]): Boolean = applyDynamic("<=")(rhs).asInstanceOf[Boolean]
  // def <(rhs: Generic[A, ConstraintOnA]): Boolean = applyDynamic("<")(rhs).asInstanceOf[Boolean]
  // def >=(rhs: Generic[A, ConstraintOnA]): Boolean = applyDynamic(">=")(rhs).asInstanceOf[Boolean]
  // def >(rhs: Generic[A, ConstraintOnA]): Boolean = applyDynamic(">")(rhs).asInstanceOf[Boolean]
  // def abs: A = applyDynamic("abs")().asInstanceOf[A]
  // def signum: Int = applyDynamic("signum")().asInstanceOf[Int]
  // def toInt: Int = applyDynamic("toInt")().asInstanceOf[Int]
  // def toLong: Long = applyDynamic("toLong")().asInstanceOf[Long]
  // def toFloat: Float = applyDynamic("toFloat")().asInstanceOf[Float]
  // def toDouble: Double = applyDynamic("toDouble")().asInstanceOf[Double]

  override def equals(other: Any) = value.equals(other)
  override def hashCode() = value.hashCode()
  override def toString() = value.toString()
}

object Generic {
  import scala.reflect.macros.Context

  type NumericTypes = Byte | Short | Int | Long | Float | Double

  type Numeric[A] = Generic[A, NumericTypes]

  object Numeric {
    def apply[N: Numeric](value: N) = new Generic.Numeric(value)
  }

  implicit def apply[A](value: A, implicitConversions: Any*) = new Generic[A, Any](value, implicitConversions)
  implicit def apply(value: Byte) = new Generic.Numeric(value)
  implicit def apply(value: Short) = new Generic.Numeric(value)
  implicit def apply(value: Int) = new Generic.Numeric(value)
  implicit def apply(value: Long) = new Generic.Numeric(value)
  implicit def apply(value: Float) = new Generic.Numeric(value)
  implicit def apply(value: Double) = new Generic.Numeric(value)

  private def findDecl(g: Generic[_, _])(name: String) = {
    def sub(mirrors: List[InstanceMirror]): Option[(Symbol, InstanceMirror)] = mirrors match {
      case Nil => None
      case mirror :: otherMirrors =>
        val tpe = mirror.symbol.asType.toType
        val symbol = tpe.member(name: TermName) orElse tpe.member(reflect.NameTransformer.encode(name): TermName)
        if (symbol == NoSymbol) {
          // println("No " + name + " or " + reflect.NameTransformer.encode(name) + " in " + mirror.instance.getClass.getName + ": " + tpe + ": " + tpe.members)
          sub(otherMirrors)
        } else
          Some(symbol -> mirror)

    }
    sub(g.targetMirrors_)
  }
  def applyDynamicImpl[A, ConstraintOnA](g: Generic[A, ConstraintOnA], name: String, args: Any*): Any = {
    findDecl(g)(name) match {
      case Some((symbol, mirror)) if symbol.isMethod =>
        val unwrapped = args.map({
          case g: Generic[_, _] => g.value
          case arg => arg
        })
        mirror.reflectMethod(symbol.asMethod)(unwrapped: _*)
      case _ =>
        throw new NoSuchMethodException("No method '" + name + "' in " + g.valueClassName_)
    }
  }
  def selectDynamicImpl[A, ConstraintOnA](g: Generic[A, ConstraintOnA], name: String): Any = {
    findDecl(g)(name) match {
      case Some((symbol, mirror)) if symbol.isMethod =>
        mirror.reflectMethod(symbol.asMethod)()
      case Some((symbol, mirror)) if symbol.isTerm =>
        mirror.reflectField(symbol.asTerm).get
      case _ =>
        throw new NoSuchFieldException("No field '" + name + "' in " + g.valueClassName_)
    }
  }
  def updateDynamicImpl[A, ConstraintOnA](g: Generic[A, ConstraintOnA], name: String, value: Any) {
    findDecl(g)(name) match {
      case Some((symbol, mirror)) if symbol.isTerm =>
        mirror.reflectField(symbol.asTerm).set(value)
      case _ =>
        throw new NoSuchFieldException("No field '" + name + "' in " + g.valueClassName_)
    }
  }
}
