package scalaxy

import scala.language.experimental.macros

import scala.reflect.macros.Context
import scala.collection.mutable

/**
 * Perform extra compilation-time checks in compilation units where scalaxy.parano.verify() is called.
 * Checks:
 * - Confusing names in case class extractors
 * - Ambiguous unnamed arguments with same type
 * - Confusing names in method calls
 * - (TODO) Potential side-effect free statements (e.g. missing + between multiline concatenations)
 */
package object parano {
  // def verify(wholeFile, extractors, methodCalls, ambiguousTypes) = macro impl.parano
  def verify() = macro impl.parano
}

package parano {

  package object impl {
    /** getFragments("this_isTheEnd") == Array("this", "is", "The", "End") */
    private def getFragments(name: String) =
      name.split("""\b|[_-]|(?<=[a-z])(?=[A-Z])""").filter(_.length >= 3)

    private case class RichName(name: String) {
      val fragments = getFragments(name).toSet

      def containsExactFragment(str: String): Boolean = {
        name.contains(str) ||
          fragments.exists(_.equalsIgnoreCase(str)) ||
          getFragments(str).exists(frag => fragments.exists(_.equalsIgnoreCase(frag)))
      }
    }

    def parano(c: Context)(): c.Expr[Unit] =
      {
        import c.universe._

        val tree = c.typeCheck(c.enclosingUnit.body, withMacrosDisabled = true)
        //val tree = c.typeCheck(block.tree)
        val caseClassesFields = new mutable.HashMap[ClassSymbol, List[RichName]]
        def getCaseClassFields(sym: ClassSymbol): List[RichName] = {
          caseClassesFields.getOrElseUpdate(sym, {
            val csym = sym.asClass
            val ctor = csym.typeSignature.member(nme.CONSTRUCTOR).asMethod
            // Case class extractors only care about the first params group.
            ctor.paramss.head.map(param => RichName(param.name.toString))
          })
        }
        val methodParams = new mutable.HashMap[MethodSymbol, List[List[RichName]]]
        def getMethodParams(sym: MethodSymbol): List[List[RichName]] = {
          methodParams.getOrElseUpdate(sym, {
            // Case class extractors only care about the first params group.
            sym.paramss.map(_.map(param => RichName(param.name.toString)))
          })
        }
        val methodParamTypeGroups = new mutable.HashMap[MethodSymbol, Map[Int, List[(String, Int)]]]
        def getMethodParamTypeGroups(sym: MethodSymbol): Map[Int, List[(String, Int)]] = {
          methodParamTypeGroups.getOrElseUpdate(sym, {
            // Case class extractors only care about the first params group.
            for (
              (tpe, params) <- sym.paramss.flatten.zipWithIndex.groupBy(_._1.typeSignature);
              if params.size > 1
            ) yield {
              val paramNames = params.map { case (param, i) => param.name.toString -> i }
              for ((param, i) <- params) yield {
                i -> paramNames
              }
            }
          }.flatten.toMap)
        }
        def isNamedParam(value: Tree): Option[Boolean] = {
          val pos = value.pos
          if (pos == NoPosition) None
          else {
            val src = pos.source.content
            var i = pos.point - 1
            var done = false
            var inComments = false
            while (i >= 0 && !done) {
              val c = src(i)
              if (inComments) {
                if (c == '*' && i > 0 && src(i - 1) == '/') {
                  i -= 2
                  inComments = false
                } else {
                  i -= 1
                }
              } else {
                c match {
                  case '/' if i > 0 && src(i - 1) == '*' =>
                    i -= 2
                    inComments = true
                  case ' ' | '\t' | '\n' | '\r' =>
                    i -= 1
                  case _ =>
                    done = true
                }
              }
            }
            Some(src(i) == '=')
          }
        }
        new Traverser {
          override def traverse(tree: Tree) {
            tree match {
              case Apply(target, args) if target.symbol.isMethod =>
                val msym = target.symbol.asMethod
                // if (args.size ==)
                val groups = getMethodParamTypeGroups(msym)
                val params = getMethodParams(msym).flatten
                val decisiveIdents = args.zip(params).map({
                  case (arg, param) =>
                    arg match {
                      case Ident(name) =>
                        val n = name.toString
                        //if (param.containsExactFragment(n)
                        val matches = params.filter(_.containsExactFragment(n))
                        if (matches.contains(param)) {
                          // Decisive if not matched by any other.
                          matches.size == 1
                        } else {
                          // Not decisive.
                          if (!matches.isEmpty) {
                            if (params.exists(_.name == n)) {
                              c.error(arg.pos,
                                s"""Confusing name: $n not used for same-named param but for param ${param.name}""")
                            } else {
                              c.error(arg.pos,
                                s"""Confusing name: $n sounds like ${matches.map(_.name).mkString(",")} but used for param ${param.name}""")
                            }
                          }
                          false
                        }
                      case _ =>
                        false
                    }
                })
                for ((arg, i) <- args.zipWithIndex; named <- isNamedParam(arg)) {
                  if (!named) {
                    for (
                      group <- groups.get(i);
                      if i != group.last._2 /* Report in all  per group */ ) {
                      val (namedParams, unnamedParams) = group.partition({
                        case (name, index) =>
                          // TODO add unequivocal ident name check here.
                          index < args.size && (
                            isNamedParam(args(index)) == Some(true) ||
                            decisiveIdents(index)
                          )
                      })
                      if (namedParams.size < group.size - 1) {
                        val param = msym.paramss.flatten.apply(i)
                        val paramName = param.name.toString
                        val others = unnamedParams.map(_._1).filter(_ != paramName)
                        c.error(arg.pos,
                          s"""Unnamed param $paramName can be confused with param${if (others.size == 1) "" else "s"} ${others.mkString(", ")} of same type ${param.typeSignature}""")
                      }
                    }
                  }
                  // c.warning(arg.pos, "Named arg!")
                  // named <- isNamedParam(arg); if named
                }

              // println(msym)
              // println(showRaw(args))
              case Match(selector, cases) =>
                val sym = selector.tpe.typeSymbol
                if (sym.isClass && sym.asClass.isCaseClass) {
                  val fields = getCaseClassFields(sym.asClass)
                  // println(s"fields = $fields")
                  for (CaseDef(pat, guard, body) <- cases) {
                    pat match {
                      case Apply(target, args) =>
                        for ((arg, field) <- args.zip(fields)) {
                          arg match {
                            case Bind(argName, _) if !field.containsExactFragment(argName.toString) =>
                              val an = argName.toString
                              for (clashingField <- fields.find(_.containsExactFragment(an))) {
                                c.error(arg.pos,
                                  s"Confusing name: $an sounds like ${sym.name}.${clashingField.name} but extracts ${sym.name}.${field.name}")
                              }
                            case _ =>
                          }
                        }
                      case _ =>
                    }
                  }
                }
              case _ =>
            }
            super.traverse(tree)
          }
        } traverse tree

        //println(tree)
        //println(showRaw(tree))

        c.literalUnit
      }
  }
}
