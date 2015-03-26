package scalaxy.parano

import scala.reflect.api.Universe
import scala.collection.mutable

/**
 * Perform extra compilation-time checks in compilation units where scalaxy.parano.verify() is called.
 * Checks:
 * - Confusing names in case class extractors
 * - Ambiguous unnamed arguments with same type
 * - Confusing names in method calls
 * - (TODO) Potential side-effect free statements (e.g. missing + between multiline concatenations)
 */
trait ParanoChecks {
  val global: Universe
  import global._

  import ParanoChecks._

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

  private val methodParams = new mutable.HashMap[MethodSymbol, List[List[RichName]]]
  private def getMethodParams(sym: MethodSymbol): List[List[RichName]] = {
    methodParams synchronized {
      methodParams.getOrElseUpdate(sym, {
        // Case class extractors only care about the first params group.
        sym.paramLists.map(_.map(param => RichName(param.name.toString)))
      })
    }
  }
  private val methodParamTypeGroups = new mutable.HashMap[MethodSymbol, Map[Int, List[(String, Int)]]]
  private def getMethodParamTypeGroups(sym: MethodSymbol): Map[Int, List[(String, Int)]] = {
    methodParamTypeGroups synchronized {
      methodParamTypeGroups.getOrElseUpdate(sym, {
        // Case class extractors only care about the first params group.
        for (
          (tpe, params) <- sym.paramLists.flatten.zipWithIndex.groupBy(_._1.typeSignature);
          if params.size > 1
        ) yield {
          val paramNames = params.map { case (param, i) => param.name.toString -> i }
          for ((param, i) <- params) yield {
            i -> paramNames
          }
        }
      }.flatten.toMap)
    }
  }
  private val caseClassesFields = new mutable.HashMap[ClassSymbol, List[RichName]]
  private def getCaseClassFields(sym: ClassSymbol): List[RichName] = {
    caseClassesFields synchronized {
      caseClassesFields.getOrElseUpdate(sym, {
        val csym = sym.asClass
        val ctor = csym.typeSignature.member(termNames.CONSTRUCTOR).asMethod
        // Case class extractors only care about the first params group.
        ctor.paramLists.head.map(param => RichName(param.name.toString))
      })
    }
  }

  def info(pos: Position, msg: String, force: Boolean): Unit
  def error(pos: Position, msg: String): Unit

  def check(tree: Tree) {
    checker traverse tree
  }

  def isSynthetic(mods: Modifiers): Boolean

  private val checker = new Traverser {
    override def traverse(tree: Tree) {
      var traverseChildren = true

      val sym = tree.symbol
      tree match {
        case DefDef(mods, _, _, _, _, _) if isSynthetic(mods) =>
          // Don't check synthetic methods (created by the compiler itself, so better be good already).
          traverseChildren = false
        case Apply(target, args) if target.symbol.isMethod =>
          val msym = target.symbol.asMethod
          if (!whitelistedMethods((msym.owner.fullName, msym.name.toString))) {
            val groups = getMethodParamTypeGroups(msym)
            val params = getMethodParams(msym).flatten
            def isArgSpecified(arg: Tree): Boolean =
              arg.pos != NoPosition && arg.pos != target.pos

            val decisiveIdents = args.zip(params).map({
              case (arg, param) if isArgSpecified(arg) =>
                arg match {
                  case Ident(name) =>
                    val n = name.toString
                    val matches = params.filter(_.containsExactFragment(n))
                    if (matches.contains(param)) {
                      // Decisive if not matched by any other.
                      matches.size == 1
                    } else {
                      // Not decisive.
                      if (!matches.isEmpty) {
                        if (params.exists(_.name == n)) {
                          error(arg.pos,
                            s"""Confusing name: $n not used for same-named param but for param ${param.name}""")
                        } else {
                          error(arg.pos,
                            s"""Confusing name: $n sounds like ${matches.map(_.name).mkString(",")} but used for param ${param.name}""")
                        }
                      }
                      false
                    }
                  case _ =>
                    false
                }
              case _ =>
                true
            })
            for {
              (arg, i) <- args.zipWithIndex
              named <- isNamedParam(arg)
              if !named && isArgSpecified(arg)
            } {
              for {
                group <- groups.get(i)
                if i != group.last._2 /* Report in all per group */
              } {
                val (namedParams, unnamedParams) = group.partition({
                  case (name, index) =>
                    // TODO add unequivocal ident name check here.
                    index < args.size && (
                      isNamedParam(args(index)) == Some(true) ||
                      decisiveIdents(index)
                    )
                })
                if (namedParams.size < group.size - 1) {
                  val param = msym.paramLists.flatten.apply(i)
                  val paramName = param.name.toString
                  val others = unnamedParams.map(_._1).filter(_ != paramName)
                  error(arg.pos,
                    s"""Unnamed param $paramName can be confused with param${if (others.size == 1) "" else "s"} ${others.mkString(", ")} of same type ${param.typeSignature} (method: ${msym.owner.fullName}.${msym.name}""")
                }
              }
            }
          }

        case Match(selector, cases) =>
          val sym = selector.tpe.typeSymbol
          if (sym.isClass && sym.asClass.isCaseClass) {
            val fields = getCaseClassFields(sym.asClass)
            for (CaseDef(pat, guard, body) <- cases) {
              pat match {
                case Apply(target, args) =>
                  for ((arg, field) <- args.zip(fields)) {
                    arg match {
                      case Bind(argName, _) if !field.containsExactFragment(argName.toString) =>
                        val an = argName.toString
                        for (clashingField <- fields.find(_.containsExactFragment(an))) {
                          error(arg.pos,
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
      if (traverseChildren)
        super.traverse(tree)
    }
  }

  /**
   * Figure whether a parameter value is given "by name".
   * There's no clean official way to do it, so the hack here is to do some simple back-parsing
   * of the source before the value's position to skip spaces and comments until a = (or any
   * other meaningful char) is met.
   */
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
}

object ParanoChecks {
  // TODO(ochafik): Add flag.
  val whitelistedMethods = Set(
    ("java.lang.String", "replace")
  )
}
