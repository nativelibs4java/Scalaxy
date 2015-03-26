package scalaxy.react

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.xml._

package object jsx extends jsx.WithJSXStringContext[ReactElement, ReactClass, React.type] {

  // implicit class JSXStringContext(val context: StringContext) {
  //   object jsx {
  //     def apply(args: Any*): JValue =
  //       macro impl.jsxApply[ReactElement, ReactClass, React.type]
  //   }
  // }
}

package jsx {

  trait WithJSXStringContext[E, C, F <: ElementFactory[E, C]] {
    implicit class JSXStringContext(val context: StringContext) {
      object jsx {
        def apply(args: Any*): E =
          macro impl.jsxApply[E, C, F]
      }
    }
  }
  // trait ElementFactory {

  // }
  object impl {
    // private[this] val paramRx = "\\$\\{.*?\\}".r

    def jsxApply[E : c.WeakTypeTag,
                 C : c.WeakTypeTag,
                 F <: ElementFactory[E, C] : c.WeakTypeTag]
                (c: Context)
                (args: c.Expr[Any]*)
                : c.Expr[E] = {
      import c.universe._

      // println("PREFIX: " + c.prefix)
      val q"${_}(scala.StringContext.apply(..$fragmentTrees)).jsx" = c.prefix.tree
      val fragmentsPosAndStrings: List[(Position, String)] = fragmentTrees map {
        case t @ Literal(Constant(s: String)) =>
          t.pos -> s
      }

      val argNamesAndTrees: List[(String, Tree)] =
        for ((((_, fragment), arg), i) <- fragmentsPosAndStrings.zip(args).zipWithIndex) yield {
          val name = "arg" + i
          (if (fragment.endsWith("\"")) name else name + "=\"...\"") -> arg.tree
        }

      val argTreesByName = argNamesAndTrees.toMap

      val SingleType(_, factorySymbol) = weakTypeOf[F]
      // println(s"SingleType($pre, $sym)")
      println(s"""
        Fragments:
          ${fragmentsPosAndStrings.mkString(",\n        ")}
        Args:
          ${argNamesAndTrees.mkString(",\n        ")}
        C:
          ${weakTypeOf[C]}
        E:
          ${weakTypeOf[E]}
        F:
          ${weakTypeOf[F]}: ${weakTypeOf[F].getClass}
      """)
      //c.Expr[E](q"null.asInstanceOf[${weakTypeOf[E]}]")
      // 
      val src =
        fragmentsPosAndStrings.map(_._2)
          .zip(argNamesAndTrees.map(_._1) :+ "").map({
             case (f, a) => f + a
          }).mkString

      println(src) 

      def processNode(node: Node): List[Tree] = node match {
        case t: Text =>
          val txt = t.data.trim
          if (txt.isEmpty)
            Nil
          else {

            List(Literal(Constant(txt)))
          }

        case e: Elem =>
          val children = node.child.flatMap(processNode(_))
          // println(node.attributes)

          def getAttrs(m: MetaData): List[Tree] = {
            val nextAttrs = Option(m.next).map(getAttrs(_)).getOrElse(Nil)

            Option(m.value).map {
              case Seq(value: Text) =>
                val res = argTreesByName.get(value.data).getOrElse(q"${value.data}")
                q"${m.key} -> $res" :: nextAttrs
            } getOrElse {
              nextAttrs
            }
          }

          val attrsArg = getAttrs(e.attributes) match {
            case Nil =>
              q"null"

            case attrs =>
              q"scala.scalajs.js.Dynamic.literal(..$attrs)"
          }

          val args = List(q"${e.label}", attrsArg) ++ children
          List(q"$factorySymbol.createElement(..$args)")

        case _ =>
          println("EMPTY NODE: " + node)
          Nil
      }

      val node = XML.loadString(src)

      val List(tree) = processNode(node)
      // println(processNode(node))
      val res = c.Expr[E](tree)
      // val res = c.Expr[E](q"""
      //   $factorySymbol.createElement("div", null)
      // """)
      println("RES: " + res)
      res
    }
  }

}
