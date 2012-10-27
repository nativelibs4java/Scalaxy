package scalaxy; package components

//import scala.reflect.mirror._
//import scala.reflect.api._
import scala.reflect.runtime._

case class MatchActionDefinition(
  name: String,
  paramCount: Int,
  typeParamCount: Int,
  matchAction: MatchAction
)

object MatchActionDefinitions
{
  import scala.reflect.runtime.currentMirror
  import scala.reflect.runtime.{universe => ru}

  private def cast[T](v: Any): T = v.asInstanceOf[T]

  def WeakTypeTag[T](tpe: ru.Type): ru.TypeTag[T] = {
    val u = ru.asInstanceOf[ru.type with scala.reflect.internal.StdCreators]
    val m = currentMirror.asInstanceOf[scala.reflect.api.Mirror[u.type]]

    //cast(u.WeakTypeTag[T](cast(m), u.FixedMirrorTypeCreator(cast(m), cast(tpe))))

    val weakTTag = u.WeakTypeTag[T](cast(m), u.FixedMirrorTypeCreator(cast(m), cast(tpe)))

    val manifest = u.typeTagToManifest[Any](m, cast(weakTTag))
    cast(u.manifestToTypeTag(m, cast(manifest)))
  }

  // TODO pick from compilet module, or auto-generate on the fly with ASM voodoo?
  class DefaultNumeric[T] extends scala.math.Numeric[T] {
    def compare(x: T, y: T): Int = ???
    def fromInt(x: Int): T       = ???
    def minus(x: T, y: T): T     = ???
    def negate(x: T): T          = ???
    def plus(x: T, y: T): T      = ???
    def times(x: T, y: T): T     = ???
    def toDouble(x: T): Double   = ???
    def toFloat(x: T): Float     = ???
    def toInt(x: T): Int         = ???
    def toLong(x: T): Long       = ???
  }

  import scala.reflect.api.Universe
  import scala.reflect.api.Mirror

  private class SyntheticTypeCreator[T](
      copyIn: Universe => Universe#TypeTag[T])
    extends scala.reflect.api.TypeCreator
  {
    def apply[U <: Universe with Singleton](m: scala.reflect.api.Mirror[U]): U # Type = {
      copyIn(m.universe).asInstanceOf[U # TypeTag[T]].tpe
    }
  }

  private class WeakTypeTagImpl[T](val tpec: SyntheticTypeCreator[T]) extends ru.WeakTypeTag[T] {
    lazy val tpe: ru.Type = tpec(currentMirror)
    override val mirror = currentMirror
    def in[U <: Universe with Singleton](otherMirror: scala.reflect.api.Mirror[U]) = {
      val otherMirror1 = otherMirror.asInstanceOf[scala.reflect.api.Mirror[otherMirror.universe.type]]
      otherMirror.universe.TypeTag[T](otherMirror1, tpec)
    }
  }

  private class TypeTagImpl[T](tpec: SyntheticTypeCreator[T]) extends WeakTypeTagImpl[T](tpec) with ru.TypeTag[T]

  private class SyntheticTypeTag[T](_tpe: ru.Type, copyIn: Universe => Universe#TypeTag[T])
    extends TypeTagImpl[T](new SyntheticTypeCreator[T](copyIn)) {
    override lazy val tpe: ru.Type = _tpe
  }

  private def defaultValue(t: ru.Type): AnyRef = {
    import ru.definitions._
    t match {
      case _ if t <:< IntTpe => java.lang.Integer.valueOf(0)
      case _ if t <:< LongTpe => java.lang.Long.valueOf(0L)
      case _ if t <:< ShortTpe => java.lang.Short.valueOf(0: Short)
      case _ if t <:< CharTpe => java.lang.Character.valueOf('\0')
      case _ if t <:< BooleanTpe => java.lang.Boolean.FALSE
      case _ if t <:< DoubleTpe => java.lang.Double.valueOf(0.0)
      case _ if t <:< FloatTpe => java.lang.Float.valueOf(0.0f)
      case _ if t <:< ByteTpe => java.lang.Byte.valueOf(0: Byte)
      case _ if t.toString.contains("TypeTag")/* <:< ru.typeOf[ru.AbsTypeTag[_]]*/ =>
        //WeakTypeTag(t)
        def st: SyntheticTypeTag[Any] =
          new SyntheticTypeTag[Any](t, _ => st)
        st
      //case _ if t.toString.contains("Numeric") =>
      //  new DefaultNumeric[Any]
      case s =>
        //println("\t\tWEIRD s = " + t + ": " + t.getClass.getName)
        null
    }
  }

  def getMatchActionDefinitions(holder: AnyRef): Seq[MatchActionDefinition] = {
    var typeParamCount = 0
    var paramCount = 0

    val compiletName = holder.getClass.getName.replaceAll("\\$", "")
    //println("INSPECTING " + compiletName)

    val compiletModule = currentMirror.staticModule(compiletName)
    //println("\t-> " + compiletModule)

    val mm = currentMirror.reflectModule(compiletModule)
    val im = currentMirror.reflect(mm.instance)

    val declarations = compiletModule.typeSignature.declarations.toSeq

    val out = collection.mutable.ArrayBuilder.make[MatchActionDefinition]()

    /*
      Ideally we'd do this:
        declarations.collect {
          case m: ru.MethodSymbol
          if m.returnType <:< ru.typeOf[MatchAction] =>
            ...
        }
      But there's an issue with matching of MethodSymbol types: it will fail with a
      ClassCastException when we reach a ClassSymbol or another kind of declaration.
    */
    for (s <- declarations; if s.isInstanceOf[ru.MethodSymbol]) {
      val m = s.asInstanceOf[ru.MethodSymbol]
      if (m.returnType <:< ru.typeOf[MatchAction]) {
        val mm = im.reflectMethod(m)
        //println("\n" + m.name + ": " + m.paramss)

        val args = m.paramss.flatten.map(_.typeSignature).map(defaultValue _)
        //println("\t\targs = " + args.mkString(", "))
        try {
          val Some(javaMethod) =
            holder.getClass.getMethods.find(_.getName == m.name.toString)

          val checkArgTypes = true//false
          if (checkArgTypes) {
            //println("\t\targs.types = " + args.map(arg => Option(arg).map(_.getClass.getName).orNull))
            //println("\t\tparams.types = " + javaMethod.getParameterTypes.mkString(", "))

            for (((arg, paramClass), paramType) <- args.zip(javaMethod.getParameterTypes).zip(javaMethod.getGenericParameterTypes); if arg != null) {
              if (!paramClass.isPrimitive && !paramClass.isInstance(arg)) {
                println("\t\tERROR: " + arg + " is not an instance of class " + paramClass.getName + ", type " + paramType)
                var c: Class[_] = arg.getClass
                println("\t\tIt extends:")
                while (c != null) {
                  println("\t\t\t-> " + c.getName)
                  c = c.getSuperclass
                }
                println("\t\tIt implements:\n\t\t\t" + arg.getClass.getGenericInterfaces.mkString(",\n\t\t\t"))
              }
            }
          }
          val result = javaMethod.invoke(holder, args.toArray:_*)
          //val result = mm(args:_*)
          //println("\tresult = " + result)
          out += MatchActionDefinition(
            m.name.toString,
            paramCount,
            typeParamCount,
            result.asInstanceOf[MatchAction])
        } catch { case ex: Throwable =>
          ex.printStackTrace
          val exx = new RuntimeException("Failed to call " + m.name + " with params (" + args.mkString(", ") + "); Method symbol = " + m + ", paramss.typeSignature = (" + m.paramss.flatten.map(_.typeSignature).mkString(", ") + ")", ex)
          throw exx
        }
      }
    }
    out.result.toSeq
  }
}
