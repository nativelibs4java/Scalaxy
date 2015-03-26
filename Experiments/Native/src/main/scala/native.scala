package scalaxy

import scala.language.reflectiveCalls

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect.ClassTag

import scala.annotation.tailrec
import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox.Context

import scala.collection.JavaConversions._

import java.io.File
import org.bridj.Pointer

package object native {

  def library(name: String): Unit = macro impl.libraryImpl

  implicit class CStringContext(val context: StringContext) {
    object c {
      def apply[A](args: Any*): A = macro impl.cInterpolationImpl[A]
      // def apply(args: Any*): Unit = macro impl.cInterpolationImpl
    }
  }
}

package native
{
  class includes(
    files: Array[String],
    classes: Array[Class[_]]) extends StaticAnnotation

  object impl extends LLVMUtils
  {
    def libraryImpl(c: Context)(name: c.Expr[String]): c.Expr[Unit] = {

      import c.universe._
      
      // println("c.internal.enclosingOwner: " + c.internal.enclosingOwner)
      
      c.Expr[Unit](q"""{
        System.loadLibrary($name)
      }""")
    }
    def cInterpolationImpl[A : c.WeakTypeTag]
                          (c: Context)
                          (args: c.Expr[Any]*)
                          : c.Expr[A] = {
      import c.universe._
      import definitions._

      val q"${_}.CStringContext(scala.StringContext.apply(..$fragments)).c" = c.prefix.tree

      if (args.nonEmpty) {
        c.error(args.head.tree.pos, "String interpolation is not supported yet.")
        null
      } else {
        val List(fragmentTree @ Literal(Constant(fragment: String))) = fragments

        val isEnclosingDefImplementation = try {
          c.enclosingDef.rhs.pos == c.macroApplication.pos
        } catch {
          case ex: Throwable =>
            false
        }

        def convertTpe(tpe: Type, arrayComponent: Boolean = false): String = tpe match {
          case IntTpe => "jint"
          case LongTpe => "jlong"
          case DoubleTpe => "jdouble"
          case FloatTpe => "jfloat"
          case ShortTpe => "jshort"
          case ByteTpe => "jchar"
          case BooleanTpe => "jchar"
          case UnitTpe | NothingTpe => "void"
          case _ if tpe <:< typeOf[AnyVal] && !arrayComponent =>
            convertTpe(tpe.members.collectFirst({
              case m if m.isTerm && m.asTerm.isVal => m.typeSignature
            }).getOrElse(sys.error(s"Failed to find $tpe")))
          case _ if tpe <:< typeOf[Pointer[_]] && !arrayComponent =>
            val TypeRef(_, _, List(tparam)) = tpe
            convertTpe(tparam) + "*"
          case _ if tpe <:< typeOf[Array[_]] && !arrayComponent =>
            val TypeRef(_, _, List(tparam)) = tpe
            convertTpe(tparam) + "Array"
          case _ => "jobject"
        }
        def typecheckDefDefSig(defDef: DefDef): DefDef =
          c.typecheck(defDef match {
            case DefDef(mods, name, tparams, vparamss, tpt, _) =>
              DefDef(mods, name, tparams, vparamss, tpt, q"???")
          }).asInstanceOf[DefDef]

        def isStatic = c.enclosingImpl match {
          case ClassDef(mods, name, tparams, impl) => false
          case ModuleDef(mods, name, impl) => true
        }
        if (isEnclosingDefImplementation) {
          val defDef = typecheckDefDefSig(c.enclosingDef)
          val retSig = convertTpe(defDef.tpt.tpe)
          val paramsSig =
            defDef.vparamss.flatten
              .map(p => convertTpe(p.tpt.tpe) + " " + p.name)
              .mkString(", ")

          val envParamName = "env"
          val clazzParamName = "clazz"
          val instanceParamName = "instance"

          val functionName = "Java_" + c.enclosingDef.symbol.fullName.replace(".", "_")
          val targetParamDef = if (isStatic) "jclass clazz" else "jobject instance"
          val prefix = List(
            "#include <jni.h>",
            "#include <scalaxy.hpp>",
            s"""extern "C" JNICALL $retSig $functionName(JNIEnv *env, $targetParamDef, $paramsSig) {""",
            "  try {"
          ).mkString("\n")
          val suffix = List(
            "} catch (const scalaxy::error& e) {",
            "    e.throw_if_needed(env);",
            "  } catch (...) {",
            "    scalaxy::error::caught_unexpected(env);",
            "  }",
            if (retSig == "void") "" else "  return 0;",
            "}"
          ).filter(_ != "").mkString("\n") + "\n"
          // Leave an empty line at the end of the file.

          val ignoreWarnings =
            List(envParamName, clazzParamName, instanceParamName)
            .map(n => s"unused parameter '$n' [-Wunused-parameter]")
            .toSet

          val prefixLinesCount = prefix.count(_ == '\n')

          val src = prefix + fragment + suffix
          // println(src)

          val srcLines = src.split("\n")
          def getFragmentOffset(column: Int, line: Int): Int = {
            (for (i <- (prefixLinesCount + 1) until (line - 1)) yield {
              srcLines(i).length + 1
            }).sum + column
          }

          val javaHome =
            Option(System.getenv("JAVA_HOME"))
              .getOrElse("/Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home")
          val resourcesDir =
            "/Users/ochafik/github/Scalaxy/" +
            "Native/src/main/resources"
          val includeDir = new File(javaHome + "/include")
          val (irSrc, msgs) =
            compileCSource(src,
              includes =
                includeDir.getPath ::
                resourcesDir ::
                includeDir.listFiles.filter(_.isDirectory).map(_.getPath).toList)

          println(irSrc)

          for (msg <- msgs if !ignoreWarnings(msg.text)) {
            import msg.{ column, line, tpe, text }
            try {
              val fragmentOffset = getFragmentOffset(column, line)
              val pos = fragmentTree.pos.withPoint(fragmentTree.pos.point + fragmentOffset)
              tpe match {
                case "info" => c.info(pos, text, force = true)
                case "warning" => c.warning(pos, text)
                case "error" | "fatal error" => c.error(pos, text)
              }
            } catch {
              case ex: Throwable =>
                throw new RuntimeException(s"Failed to process message $msg: $ex", ex)
            }
          }
        }
      
        // println("ENCLOSING IS MACRO APP: " + (c.enclosingDef.rhs eq c.macroApplication))
        // c.Expr[A](q"""sys.error("Not implemented")""")
        c.Expr[A](q"null.asInstanceOf[${weakTypeTag[A].tpe}]")
        // c.Expr[Unit](q"()")
      }
    }
  }
}
