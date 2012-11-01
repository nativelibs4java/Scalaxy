package scalaxy; package components

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.{universe => u}

case class MatchActionDefinition(
  name: String,
  matchAction: MatchAction)

object MatchActionDefinitions
{
  private def defaultValue(t: u.Type): AnyRef = {
    import u.definitions._
    t match {
      case _ if t <:< IntTpe => java.lang.Integer.valueOf(0)
      case _ if t <:< LongTpe => java.lang.Long.valueOf(0L)
      case _ if t <:< ShortTpe => java.lang.Short.valueOf(0: Short)
      case _ if t <:< CharTpe => java.lang.Character.valueOf('\0')
      case _ if t <:< BooleanTpe => java.lang.Boolean.FALSE
      case _ if t <:< DoubleTpe => java.lang.Double.valueOf(0.0)
      case _ if t <:< FloatTpe => java.lang.Float.valueOf(0.0f)
      case _ if t <:< ByteTpe => java.lang.Byte.valueOf(0: Byte)
      //case _ if t.toString.contains("TypeTag")/* <:< u.typeOf[u.AbsTypeTag[_]]*/ =>
      //  //WeakTypeTag(t)
      // import SyntheticTypeTags._
      //  def st: SyntheticTypeTag[Any] =
      //    new SyntheticTypeTag[Any](t, _ => st)
      //  st
      case s =>
        //println("\t\tWEIRD s = " + t + ": " + t.getClass.getName)
        null
    }
  }

  def getCompiletName(compilet: AnyRef) =
    compilet.getClass.getName.replaceAll("\\$", "")
    
  def getCompiletDefinitions(compiletName: String): Seq[MatchActionDefinition] = {
    val compiletModule = currentMirror.staticModule(compiletName)
    val moduleMirror = currentMirror.reflectModule(compiletModule)
    val compilet = moduleMirror.instance
    val instanceMirror = currentMirror.reflect(compilet)

    compiletModule.typeSignature.declarations.toSeq.collect {
      case m: u.MethodSymbol
      if m.isMethod && m.returnType <:< u.typeOf[MatchAction] =>
        val args =
          m.paramss.flatten.map(_.typeSignature).map(defaultValue _)
        try {
          val methodMirror = instanceMirror.reflectMethod(m)
          val result = methodMirror(args:_*)
          MatchActionDefinition(
            m.name.toString,
            result.asInstanceOf[MatchAction])
        } catch { case ex: Throwable =>
          throw new RuntimeException(
            "Failed to call " + m.name + "; " +
            "Args = " + args.mkString(", ") + "; " +
            "Method symbol = " + m + "; " +
            "paramss.typeSignature = " +
              m.paramss.flatten.map(_.typeSignature).mkString(", ") + 
            ";",
            ex)
        }
    }
  }
}
// Check param types:
//val Some(javaMethod) =
//  holder.getClass.getMethods.find(_.getName == m.name.toString)
//
//val checkArgTypes = true//false
//if (checkArgTypes) {
//  //println("\t\targs.types = " + args.map(arg => Option(arg).map(_.getClass.getName).orNull))
//  //println("\t\tparams.types = " + javaMethod.getParameterTypes.mkString(", "))
//
//  for (((arg, paramClass), paramType) <- args.zip(javaMethod.getParameterTypes).zip(javaMethod.getGenericParameterTypes); if arg != null) {
//    if (!paramClass.isPrimitive && !paramClass.isInstance(arg)) {
//      println("\t\tERROR: " + arg + " is not an instance of class " + paramClass.getName + ", type " + paramType)
//      var c: Class[_] = arg.getClass
//      println("\t\tIt extends:")
//      while (c != null) {
//        println("\t\t\t-> " + c.getName)
//        c = c.getSuperclass
//      }
//      println("\t\tIt implements:\n\t\t\t" + arg.getClass.getGenericInterfaces.mkString(",\n\t\t\t"))
//    }
//  }
//}
//val result = javaMethod.invoke(holder, args.toArray:_*)
