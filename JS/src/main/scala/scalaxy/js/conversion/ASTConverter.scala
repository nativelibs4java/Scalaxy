package scalaxy.js
import ast._

import scala.language.implicitConversions

import scala.reflect.NameTransformer.{ encode, decode }

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.macros.Context
import scala.reflect.api.Universe
// import scala.reflect.api.Universe

trait ASTConverter extends Globals {

  val global: Universe
  import global._

  object N {
    def unapply(name: Name): Option[String] = Option(name).map(n => decode(n.toString))
  }

  // TODO use Nodes here
  case class GlobalPrefix(path: String = "") {
    def derive(subPath: String) =
      GlobalPrefix(if (path == "") subPath else path + "." + subPath)
  }

  case class GuardedPrefixes(set: mutable.Set[String] = mutable.Set()) {
    def add(prefix: String) = {
      if (set(prefix)) {
        false
      } else {
        set += prefix
        true
      }
    }
  }

  private implicit def n2s(name: Name): String = name.toString
  private implicit class ListExt(list: List[JS.Node]) {
    def unique: JS.Node = list match {
      case Nil => JS.NoNode
      case List(v) => v
    }
  }
  private def pos(tree: Tree) = tree.pos match {
    case NoPosition => NoSourcePos
    case p => SourcePos(p.source.path, p.line, p.column)
  }

  private def resolve(sym: Symbol, pos: SourcePos): Option[JS.Node] = {
    if (sym == NoSymbol || sym.name == null)
      None
    else {
      var path: List[String] = sym.name.toString :: Nil
      var reachedGlobalScope = false
      var ownerPackage = sym.owner
      while (ownerPackage != NoSymbol && !ownerPackage.isPackage) {
        if (!reachedGlobalScope) {
          if (hasGlobalAnnotation(ownerPackage) ||
              ownerPackage.isClass && ownerPackage.asClass.module != NoSymbol && 
                hasGlobalAnnotation(ownerPackage.asClass.module)) {
            reachedGlobalScope = true;
          } else {
            path = ownerPackage.name.toString :: path
          }
        }
        ownerPackage = ownerPackage.owner
      }

      if (sym.isClass) {
        if (ownerPackage != NoSymbol) {//} && !reachedGlobalScope) {
          path = ownerPackage.fullName :: path
        }
      }
      Some(JS.newSelect(pos, path:_*))
    }
  }

  def convertFunction(vparams: List[ValDef], rhs: Tree, funPos: SourcePos)
                     (implicit globalPrefix: GlobalPrefix,
                      guardedPrefixes: GuardedPrefixes): JS.Function = {

    val (stats, value) = rhs match {
      case Block(stats, value) => (stats, value)
      case value => (Nil, value)
    }
    JS.Function(
      None,
      vparams.map(p => JS.Ident(p.name, pos(p))),
      JS.Block(
        stats.flatMap(convert(_)) ++
        (
          if (value.tpe != null && !(value.tpe <:< typeOf[Unit]))
            (JS.Return(convert(value).unique, pos(value)): JS.Node) :: Nil
          else
            convert(value)
        ),
        funPos),
      funPos)
  }

  def assembleBlock(stats: List[JS.Node], value: JS.Node, pos: SourcePos, applyArgs: List[JS.Node] = Nil): JS.Node = {
    val fun = JS.Function(None, Nil, JS.Block(stats :+ JS.Return(value, pos), pos), pos)
    if (applyArgs.isEmpty)
      JS.Apply(fun, Nil, pos)
    else
      JS.Apply(JS.Select(fun, JS.Ident("apply", pos), pos), applyArgs, pos)
  }

  def convert(tree: Tree, topLevel: Boolean = false)
             (implicit globalPrefix: GlobalPrefix,
              guardedPrefixes: GuardedPrefixes): List[JS.Node] = {

    def defineVar(name: Name, value: JS.Node, pos: SourcePos, isLazy: Boolean = false): List[JS.Node] = {
      val emptyObj = JS.newEmptyJSON(pos)
      val (predefs, target) = {
        if (!topLevel || globalPrefix.path.isEmpty) {
          (Nil, JS.Ident("this", pos))
        } else {
          val components = globalPrefix.path.split("\\.")
          val (predefs, guards) = (for (n <- (1 to components.size).toList) yield {
            val ancestorComponents = components.take(n)
            val ancestorName = ancestorComponents.mkString(".")
            if (guardedPrefixes.add(ancestorName))
              Some {
                val ancestorRef = JS.newSelect(pos, ancestorComponents:_*)
                (
                  JS.If(
                    JS.PrefixOp("!", ancestorRef, pos),
                    if (n == 1)
                      JS.VarDef(components.head, emptyObj, pos)
                    else
                      JS.Assign(ancestorRef, emptyObj, pos),
                    JS.NoNode,
                    pos): JS.Node,
                  ancestorName
                )
              }
            else
              None
          }).flatten.unzip
          (predefs, JS.Ident(globalPrefix.path, pos))
        }
      }

      if (isLazy) {
        predefs :+
        JS.Apply(
          JS.newSelect(pos, "scalaxy", "defineLazyFinalProperty"),
          List(
            target,
            JS.Literal(name.toString.trim, pos),
            JS.Function(
              None,
              Nil,
              JS.Block(
                List(
                  JS.Return(value, pos)),
                pos),
              pos)),
          pos)
      } else {
        if (!topLevel || globalPrefix.path.isEmpty) {
          JS.VarDef(name, value, pos) :: Nil
        } else {
          predefs :+
          JS.Assign(JS.newSelect(pos, globalPrefix.path, name), value, pos)
        }
      }
    }

    if (tree.toString.contains("new scala.Array"))
      println("ARRAY IS: " + tree)

    val res: List[JS.Node] = //try {
      tree match {

        case q"$jsStringContext(scala.StringContext.apply(..$fragments)).js(..$args)" =>
          JS.Interpolation(
            fragments.map { case Literal(Constant(s: String)) => s },
            args.flatMap(convert(_)),
            pos(tree)) :: Nil

        // Represent array with... array.
        case q"scala.Array.apply[..$tparams](..$values)" =>
          JS.JSONArray(values.map(convert(_).unique), pos(tree)) :: Nil

        case Apply(Select(a, N(op @ ("+" | "-" | "*" | "/" | "%" | "<" | "<=" | ">" | ">=" | "==" | "!=" | "===" | "!==" | "<<" | ">>" | ">>>" | "||" | "&&" | "^^" | "^" | "&" | "|"))), List(b)) =>
          JS.BinOp(convert(a).unique, op, convert(b).unique, pos(tree)) :: Nil

        //case q"new scala.Array[..$tparams]($length)($classTag)" =>
        case Apply(Apply(TypeApply(Select(New(arr), constr), List(tpt)), List(length)), List(classTag))
            if tree.tpe != null && tree.tpe <:< typeOf[Array[_]] =>
          val p = pos(tree)
          JS.Apply(
            JS.New(
              JS.Ident("Array", p),
              p),
            List(
              convert(length).unique),
            p) :: Nil

        // Represent tuples as arrays (this will go well with ES6 destructuring assignments).
        case q"$target.apply[..$tparams](..$values)" 
            if target.symbol != null && target.symbol.fullName.toString.matches("""scala\.Tuple\d+""") =>
          //println("SYMMM: " + target.symbol.fullName)
          JS.JSONArray(values.map(convert(_).unique), pos(tree)) :: Nil

        case q"$mapCreator[$keyType, $valueType](..$pairs)" if tree.tpe != null && tree.tpe <:< typeOf[Map[_, _]] =>
          (try {
            JS.JSONObject(
              pairs.map({
                //case q"$key -> $value" =>
                case q"$assocBuilder[$keyType]($key).->[$valueType]($value)" =>
                  val Literal(Constant(keyString: String)) = key
                  keyString -> convert(value).unique
              }).toMap,
              pos(tree))
          } catch { case ex: MatchError =>
            val objName = "obj"
            val pairName = "pair"
            var needsPairVar = false
            val keyVals = pairs.toList.map(pair => pair match {
              case q"$assocBuilder[$keyType]($key).->[$valueType]($value)" =>
                (
                  Nil,
                  convert(key).unique,
                  pos(key),
                  convert(value).unique,
                  pos(value)
                )
              case _ =>
                println("FAILED TO MATCH PAIR: " + pair)
                val p = pos(pair)
                val convertedPair = convert(pair).unique
                val (predefs, pairRef) = pair match {
                  case _: JS.Ident =>
                    (
                      Nil,
                      convertedPair
                    )
                  case _ =>
                    needsPairVar = true
                    (
                      JS.Assign(JS.Ident(pairName, p), convertedPair, p) :: Nil,
                      JS.Ident(pairName, p)
                    )
                }
                (
                  predefs,
                  JS.Select(pairRef, JS.Literal(0, p), p),
                  p,
                  JS.Select(pairRef, JS.Literal(1, p), p),
                  p
                )
            })
            val p = pos(tree)
            assembleBlock(
              List(
                JS.VarDef(objName, JS.newEmptyJSON(p), p)
              ) ++
              (
                if (needsPairVar)
                  JS.VarDef(pairName, JS.NoNode, p) :: Nil
                else
                  Nil
              ) ++
              keyVals.flatMap({
                case (predefs, key, keyPos, value, valuePos) =>
                  predefs :+
                  JS.Assign(
                    JS.Select(JS.Ident(objName, keyPos), key, keyPos),
                    value,
                    keyPos)
              }),
              JS.Ident(objName, pos(tree)),
              pos(tree),
              applyArgs = List(JS.Ident("this", pos(tree))))
          }) :: Nil

        // case ClassDef(mods, name, tparams, Template(parents, self, q"def $init(..$vparams) = $initBody" :: body)) =>
        //   // val (constructor, body) = decls
        //   // val q"def $init(..$vparams) = $initBody" = constructor
        case q"class $name[..$tparams](..$vparams) { ..$body }" =>
          defineVar(
            name,
            JS.Function(
              None,
              vparams.map(p => JS.Ident(p.name, pos(p))),
              JS.Block(
                body.flatMap({
                  case d: ValDef =>
                    convert(d) :+
                    JS.Assign(
                      JS.newSelect(pos(d), "this", d.name),
                      JS.Ident(d.name, pos(d)),
                      pos(d))
                  case _: DefDef =>
                    Nil
                  case t =>
                    convert(t)
                }) :+
                JS.Return(JS.Ident("this", pos(tree)), pos(tree)),
                pos(tree)),
              pos(tree)),
            pos(tree)) ++
          body.collect({
            case d: DefDef if d.name != nme.CONSTRUCTOR =>
              JS.Assign(
                JS.newSelect(pos(d), name, "prototype", d.name),
                convert(d).unique,
                pos(d))
          })

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
            //assembleBlock(stats, value, pos, applyArgs)
            assembleBlock(
              List(
                JS.VarDef(
                  name,
                  JS.newEmptyJSON(pos(tree)),
                  pos(tree)),
                assembleBlock(
                  body.flatMap({
                    case d: ValOrDefDef if !d.mods.hasFlag(Flag.LAZY) && d.name != nme.CONSTRUCTOR =>
                      val p = pos(d)
                      convert(d) :+
                      JS.Assign(
                        JS.Select(
                          JS.Ident(name, p),
                          JS.Ident(d.name, p),
                          p),
                        JS.Ident(d.name, p),
                        p)
                    case t =>
                      convert(t)
                  }),
                  JS.NoNode,
                  pos(tree),
                  applyArgs = JS.Ident(name, pos(tree)) :: Nil
                )
              ),
              JS.Ident(name, pos(tree)),
              pos(tree)
            ),
            pos(tree),
            isLazy = true
          )

        case Apply(target, args) =>
          JS.Apply(
            convert(target).unique,
            args.flatMap(convert(_)),
            pos(tree)) :: Nil

        case Import(_, _) =>
          Nil

        case Super(qual, mix) =>
          Nil // TODO

        case Block(stats, value) =>
          // TODO better job here
          // if (value.tpe <:< typeOf[Unit])
          assembleBlock(
            stats.flatMap(convert(_)),
            convert(value).unique,
            pos(tree)) :: Nil

        case Select(target, name) =>
          val convTarget = convert(target)
          if (name == nme.CONSTRUCTOR)
            convTarget
          else
            JS.Select(convTarget.unique, JS.Ident(name, pos(tree)), pos(tree)) :: Nil

        case Ident(name) =>
          resolve(tree.symbol, pos(tree)).getOrElse {
            JS.Ident(name, pos(tree))
          } :: Nil

        case Literal(Constant(v)) =>
          JS.Literal(v, pos(tree)) :: Nil

        case PackageDef(pid, stats) =>
          val subGlobalPrefix = globalPrefix.derive(pid.toString)
          stats.flatMap(convert(_, topLevel = true)(subGlobalPrefix, guardedPrefixes))

        case This(qual) =>
          JS.Ident("this", pos(tree)) :: Nil
          //JS.Ident(qual.toString, pos(tree)) :: Nil // TODO

        case EmptyTree =>
          Nil

        case ValDef(mods, name, tpt, rhs) =>
          // println("VALDEF: " + tree)
          if (mods.hasFlag(Flag.LAZY)) {
             //println("&& rhs.toString == "_") 
            // On typed trees, lazy vals are split into declaration and assignment.
            // Only transforming the assignment.
            Nil
          } else {
            defineVar(
              name,
              convert(rhs).unique,
              pos(tree),
              isLazy = mods.hasFlag(Flag.LAZY))
          }

        case Function(vparams, body) =>
          convertFunction(vparams, body, pos(tree)) :: Nil

        case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
          val termSym = tree.symbol.asTerm
          if (name == nme.CONSTRUCTOR) {
            Nil
          } else if (termSym.isAccessor) {
            if (termSym.isLazy) {
              val Block(List(Assign(lhs, rhs2)), value) = rhs
              // Assignment of lazy val, introduced by typer.
              defineVar(
                lhs.symbol.name,
                convert(rhs2).unique,
                pos(tree),
                isLazy = true)
            } else {
              Nil
            }
          } else {
            defineVar(
              name,
              convertFunction(vparamss.flatten, rhs, pos(tree)),
              pos(tree))
          }

        case Assign(lhs, rhs) =>
          JS.Assign(convert(lhs).unique, convert(rhs).unique, pos(tree)) :: Nil

        case New(target) =>
          JS.New(
            resolve(target.symbol, pos(tree)) match {
              case Some(resolved) => resolved
              case None => convert(target).unique
            },
            pos(tree)) :: Nil

        case _ =>
          sys.error("Case not handled (" + tree.getClass.getName + ", tpe = " + tree.tpe + ", sym = " + tree.symbol + "): " + tree)
          Nil
      }
    // } catch { case e: MatchError =>
    //   e.printStackTrace(System.out)
    //   throw new MatchError(e.getMessage + "\nOn: " + tree, e)
    // }

    // if (res == "") "" else "/* " + tree + "*/\n" + res
    // if (res.length < 100)
    //   println("CONVERTED:\n\t" + tree + "\n\t=>\n\t\t" + res)
    res
  }
}
