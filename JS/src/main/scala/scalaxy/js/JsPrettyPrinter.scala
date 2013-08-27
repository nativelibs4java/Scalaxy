package scalaxy.js

import scala.collection.JavaConversions._
import scala.reflect.macros.Context
// import scala.reflect.api.Universe

object JsPrettyPrinter {
  case class GlobalPrefix(path: String = "") {
    def derive(subPath: String): GlobalPrefix =
      GlobalPrefix(if (path == "") subPath else path + "." + subPath)
  }
  def prettyPrintJs(c: Context)(tree: c.universe.Tree): String = {
    import c.universe._

    def resolve(sym: Symbol): Option[String] = if (sym == NoSymbol || sym.name == null) None else Some({
      var path: List[String] = sym.name.toString :: Nil
      var reachedGlobalScope = false
      var ownerPackage = sym.owner
      while (ownerPackage != NoSymbol && !ownerPackage.isPackage) {
        if (!reachedGlobalScope) {
          if (scalaxy.js.global.hasAnnotation(c.universe)(ownerPackage) ||
              ownerPackage.isClass && ownerPackage.asClass.module != NoSymbol && 
                global.hasAnnotation(c.universe)(ownerPackage.asClass.module)) {
            reachedGlobalScope = true;
          } else {
            path = ownerPackage.name.toString :: path
          }
        }
        ownerPackage = ownerPackage.owner
      }

      if (sym.isClass) {
        if (ownerPackage != NoSymbol) {
          path = ownerPackage.fullName :: path
        }
      }
      path.mkString(".")
    })

    def convertStatement(tree: Tree, indent: String)(implicit globalPrefix: GlobalPrefix): String = {
      val s = convert(tree, indent)
      if (s.trim.isEmpty || s.endsWith(";"))
        s
      else
        s + ";"
    }
    def convert(tree: Tree, indent: String = "", topLevel: Boolean = false)(implicit globalPrefix: GlobalPrefix = GlobalPrefix()): String = {
      val subIndent = indent + "  "
      val subSubIndent = subIndent + "  "

      def assembleBlock(stats: List[String], value: String): String = {
        "(function() {\n" +
          subIndent + stats.filter(!_.trim.isEmpty).map(_ + ";").mkString("\n" + subIndent) + "\n" +
          subIndent + "return " + value + ";\n" +
        indent + "})()"
      }
      def defineVar(name: Name, value: String, isLazy: Boolean = false): String = {
        val (predefs, target) = {
          if (!topLevel || globalPrefix.path.isEmpty) {
            ("", "this")
          } else {
            val components = globalPrefix.path.split("\\.")
            val predefs = (for (n <- 1 to components.size) yield {
              val ancestor = components.take(n).mkString(".")
              "if (!" + ancestor + ") " + (if (n == 1) "var " else "") + ancestor + " = {};"
            }).mkString("\n" + indent) + "\n"
            val target = globalPrefix.path

            (predefs, target)
          }
        }

        if (isLazy) {
          predefs +
          indent + "scalaxy.defineLazyFinalProperty(" + target + ", '" + name + "', function() {\n" +
            subIndent + "return " + value + ";\n" +
          indent + "});"
        } else {
          if (!topLevel || globalPrefix.path.isEmpty) {
            "var " + name + " = " + value + ";"
          } else {
            predefs +
            indent + globalPrefix.path + "." + name + " = " + value + ";"
          }
        }
      }

      val res = tree match {
        // case ClassDef(mods, name, tparams, Template(parents, self, q"def $init(..$vparams) = $initBody" :: body)) =>
        //   // val (constructor, body) = decls
        //   // val q"def $init(..$vparams) = $initBody" = constructor
        case q"class $name[..$tparams](..$vparams) { ..$body }" =>
          defineVar(
            name,
            "function(" + vparams.map(_.name).mkString(", ") + ") {\n" +
              subIndent + body.collect({
                case d: ValDef =>
                  convertStatement(d, indent + "  ") + "\n" +
                  indent + "this." + d.name + " = " + d.name + ";"
                case _: DefDef =>
                  ""
                case t =>
                  convertStatement(t, indent + "  ")
              }).mkString("\n" + indent) + "\n" +
            indent + "}"
          ) + "\n" +
          indent + body.collect({
            case d: DefDef if d.name != nme.CONSTRUCTOR =>
              name + ".prototype." + d.name + " = " + convert(d, indent) + ";"
          }).mkString("\n" + indent)

        case ModuleDef(mods, name, Template(parents, self, body)) =>
        // case q"object $name { ..$body }" =>
          val needsStableName = body.exists({
            case d: ValOrDefDef if d.name != nme.CONSTRUCTOR =>
              true
            case d: ModuleDef =>
              true
            case _=>
              false
          })
          defineVar(
            name,
            assembleBlock(
              List(
                "var " + name + " = {}",
                "(function() {\n" +
                  subSubIndent + body.collect({
                    case d: ValOrDefDef if !d.mods.hasFlag(Flag.LAZY) && d.name != nme.CONSTRUCTOR =>
                      convertStatement(d, subSubIndent) + "\n" +
                      subSubIndent + name + "." + d.name + " = " + d.name + ";"
                    case t =>
                      convertStatement(t, subSubIndent)
                  }).mkString("\n" + subSubIndent) + "\n" +
                subIndent + "}).apply(" + name + ");"
              ),
              name.toString
            ),
            isLazy = true
          )

        case Apply(target, args) =>
          convert(target, subIndent) + "(" + args.map(convert(_, subIndent)).mkString(", ") + ")"

        case Import(_, _) =>
          ""

        case Super(qual, mix) =>
          "" // TODO

        case Block(stats, value) =>
          assembleBlock(
            stats.map(s => convert(s, subSubIndent)),
            convert(value, subSubIndent)
          )

        case Select(target, name) =>
          val convTarget = convert(target, subIndent)
          if (name == nme.CONSTRUCTOR)
            convTarget
          else
            convTarget + "." + name

        case Ident(name) =>
          val resolved = resolve(tree.symbol)
          // println("RESOLVED(" + name + ") = " + resolved)
          name.toString

        case Literal(Constant(v)) =>
          v match {
            case s: String =>
              "'" + s.replaceAll("'", "\\\\'") + "'" // TODO: proper JS escapes
            case c: Char =>
              "'" + c + "'"
            case _ =>
              v.toString // TODO: proper toString for numbers and all
          }

        case PackageDef(pid, stats) =>
          val subGlobalPrefix = globalPrefix.derive(pid.toString)
          stats.map(convert(_, indent, topLevel = true)(subGlobalPrefix)).mkString("\n" + indent)

        case This(qual) =>
          qual.toString // TODO

        case EmptyTree =>
          ""

        case ValDef(mods, name, tpt, rhs) =>
          // println("VALDEF: " + tree)
          if (mods.hasFlag(Flag.LAZY)) {
             //println("&& rhs.toString == "_") 
            // On typed trees, lazy vals are split into declaration and assignment.
            // Only transforming the assignment.
            ""
          } else {
            defineVar(name, convert(rhs, subIndent), isLazy = mods.hasFlag(Flag.LAZY))
          }

        case DefDef(mods, name, tparams, vparams, tpt, rhs) =>
          val termSym = tree.symbol.asTerm
          if (name == nme.CONSTRUCTOR) {
            ""
          } else if (termSym.isAccessor) {
            if (termSym.isLazy) {
              val Block(List(Assign(lhs, rhs2)), value) = rhs
              // Assignment of lazy val, introduced by typer.
              defineVar(lhs.symbol.name, convert(rhs2, subIndent), isLazy = true)
            } else {
              ""
            }
          } else {
            defineVar(
              name,
              "function(" + vparams.flatten.map(_.name).mkString(", ") + ") {\n" +
                indent + convert(rhs, subIndent) + "\n" +
              indent + "}"
            )
          }

        case Assign(lhs, rhs) =>
          // println("ASSIGN: " + tree)
          // println("RHS: " + rhs)
          val sym = lhs.symbol
          // if (sym != null && sym.asTerm.isLazy) {
          //   // Assignment of lazy val, introduced by typer.
          //   defineVar(lhs.symbol.name, convert(rhs, subIndent), isLazy = true)
          // } else {
            convert(lhs, indent) + " = " + convert(rhs, indent)
          // }

        case New(target) =>
          resolve(target.symbol) match {
            case Some(resolved) => "new " + resolved
            case None => "new " + convert(target, indent)
          }
          // println("RESOLVED NEW(" + target + ") = " + resolved)
          // println("NEW target = " + target + ", tpe = " + target.tpe + ", sym = " + target.symbol)
          // "new " + convert(target, indent)

        case _ =>
          sys.error("Case not handled (" + tree.getClass.getName + ", tpe = " + tree.tpe + ", sym = " + tree.symbol + "): " + tree)
      }

      // if (res == "") "" else "/* " + tree + "*/\n" + indent + res
      // if (res.length < 100)
      //   println("CONVERTED:\n\t" + tree + "\n\t=>\n\t\t" + res)
      res
    }
    convert(tree, "", topLevel = true)
  }
}
