package scalaxy.js

import scala.reflect.api.Universe
import scala.collection.JavaConversions._

import com.google.javascript.rhino.Node
import com.google.javascript.rhino.JSTypeExpression
import com.google.javascript.rhino.jstype._

trait JSToScalaTypeConversions {

  val global: Universe
  import global._

  def convertTypeRef(jsType: JSType, resolver: Name => Tree, nullable: Boolean = false): Tree = {
    jsType match {
      case null =>
        TypeTree(typeOf[Any])
        case (_: AllType) | (_: NoType) | (_: UnknownType) =>
          TypeTree(typeOf[Any])
        case t: BooleanType =>
          TypeTree(if (nullable) typeOf[java.lang.Boolean] else typeOf[Boolean])
        case t: NullType =>
          TypeTree(typeOf[Null])
        case t: NumberType =>
          TypeTree(if (nullable) typeOf[java.lang.Double] else typeOf[Double])
        case t: StringType =>
          TypeTree(typeOf[String])
        case t: VoidType =>
          TypeTree(typeOf[Unit])

        case t: UnionType =>
          var hasNull = false
          var hasUndefined = false
          val alts = t.getAlternates.toList.filter(t => t match {
            case _: NullType =>
              hasNull = true
              false
            case _: VoidType =>
              hasUndefined = true
              false
            case _ =>
              true
          })
          val convertedAlts = alts.map(t => convertTypeRef(t, resolver, hasNull || hasUndefined))
          val conv = convertedAlts match {
            case List(t) =>
              t
            case ts =>
              // println("Reducing types: " + ts.mkString(", ") + " (jsType = " + jsType + ")")
              ts.reduceLeft((a, b) => {
                AppliedTypeTree(
                  Ident(rootMirror.staticClass("scala.util.Either")),
                  // TODO: alternatives in separate dependency
                  // Ident(rootMirror.staticClass("scalaxy.js.|")),
                  List(a, b)
                )
              })
          }
          if (hasUndefined) {
            AppliedTypeTree(
              Ident(rootMirror.staticClass("scala.Option")),
              List(conv)
            )
          } else {
            conv
          }
          // TypeTree(typeOf[AnyRef])
        case t: ObjectType =>
          val n = t.getDisplayName
          if (n == null)
            TypeTree(typeOf[AnyRef])
          else {
            (n, Option(t.getTemplateTypes).map(_.toList).getOrElse(Nil)) match {
              case ("Array", List(elementType)) =>
                AppliedTypeTree(
                  Ident(rootMirror.staticClass("scala.Array")),
                  List(
                    convertTypeRef(elementType, resolver)
                  )
                )
              case ("Function", elementTypes) =>
                println("FUNCTION elementTypes = " + elementTypes.map(convertTypeRef(_, resolver)))
                TypeTree(typeOf[AnyRef])//n: TypeName)
                // AppliedTypeTree(
                //   Ident(rootMirror.staticClass("scala.Function" + )),
                //   List(convertTypeRef(u)(elementType, resolver).asInstanceOf[u.Tree])
                // )
              case ("Object", _) =>
                TypeTree(typeOf[AnyRef])//n: TypeName)
              case (_, _ :: _) =>
                sys.error("Template type not handled for type " + n + ": " + jsType)
              case _ =>
                Option(resolver).map(_(n: TypeName)).getOrElse {
                  Ident(n: TypeName)
                }
            }
          }
    }
  }
}
