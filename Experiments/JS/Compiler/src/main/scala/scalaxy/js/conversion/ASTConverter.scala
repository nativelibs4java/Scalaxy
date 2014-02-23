package scalaxy.js
import ast._

import scala.language.implicitConversions

import scala.reflect.NameTransformer.{ encode, decode }

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.macros.Context
import scala.reflect.api.Universe
// import scala.reflect.api.Universe

trait ASTConverter
    extends Globals
    with ScalaToJSTypeConversions
    with Matchers {

  val global: Universe
  import global._

  def runtimeObjectClassName = "scalaxy.lang.Object"

  // TODO use Nodes here
  case class GlobalPrefix(path: String = "") {
    def derive(subPath: String) =
      GlobalPrefix(if (path == "") subPath else path + "." + subPath)
  }

  case class GuardedPrefixes(set: java.util.LinkedHashSet[String] = new java.util.LinkedHashSet()) {
    def add(prefix: String) = set.add(prefix)
    def generateGuards(implicit pos: SourcePos): List[JS.Node] = set.toList.map(name => {
      val ref = JS.path(name)
      JS.If(
        JS.PrefixOp("!", ref),
        if (name.contains("."))
          JS.Assign(ref, JS.newEmptyJSON)
        else
          JS.newEmptyJSON.asVar(name),
        JS.NoNode)
    })
  }

  private implicit def n2s(name: Name): String =
    if (name == nme.EMPTY || name == tpnme.EMPTY) ""
    else name.toString

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

  private def resolve(sym: Symbol)(implicit pos: SourcePos): Option[JS.Node] = {
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
      Some(JS.path(path.mkString(".")))
    }
  }

  def convertFunction(vparams: List[ValDef], rhs: Tree, thisType: Option[Symbol], isConstructor: Boolean)
                     (implicit globalPrefix: GlobalPrefix,
                      guardedPrefixes: GuardedPrefixes,
                      funPos: SourcePos): JS.Node = {

    val (stats, value) = rhs match {
      case Block(stats, value) => (stats, value)
      case value => (Nil, value)
    }
    var jsDocLines =
      (
        if (isConstructor) List("@constructor")
        else Nil
      ) ++
      thisType.map(t => "@this {!" + t.fullName + "}").toList ++
      vparams.map(p => "@param {" + eraseType(p.tpe) + "} " + p.name) ++
      (
        if (Option(rhs.tpe).exists(_ =:= typeOf[Unit])) Nil
        else List("@return {" + eraseType(rhs.tpe) + "}")
      )

    val fun = JS.Function(
      None,
      vparams.map(param => JS.Ident(param.name)(pos(param))),
      JS.Block(
        stats.flatMap(convert(_)) ++
        (
          if (Option(value.tpe).exists(tpe => !(tpe <:< typeOf[Unit])))
            (JS.Return(convert(value).unique)(pos(value)): JS.Node) :: Nil
          else
            convert(value)
        )))
    if (jsDocLines.isEmpty)
      fun
    else
      JS.Commented(
        jsDocLines.map(" * " + _).mkString("/**\n", "\n", "\n */"),
        fun)
  }

  def assembleBlock(stats: List[JS.Node], value: JS.Node, applyArgs: List[JS.Node] = Nil)
                   (implicit pos: SourcePos): JS.Node = {
    val fun = JS.Function(None, Nil, JS.Block(stats :+ JS.Return(value)))
    if (applyArgs.isEmpty)
      fun.apply(Nil)
    else
      fun.apply("apply", applyArgs)
  }


  def defineVar(name: String, value: JS.Node, isLazy: Boolean = false, topLevel: Boolean = false)
               (implicit globalPrefix: GlobalPrefix,
                guardedPrefixes: GuardedPrefixes,
                pos: SourcePos): JS.Node = {
    val target = {
      if (/*!topLevel ||*/ globalPrefix.path.isEmpty) {
        JS.Ident("this")
      } else {
        val components = globalPrefix.path.split("\\.")
        for (n <- (1 to components.size).toList) {
          guardedPrefixes.add(components.take(n).mkString("."))
        }
        JS.Ident(globalPrefix.path)
      }
    }
    def sub(value: JS.Node) = {
      if (isLazy) {
        JS.path("scalaxy.defineLazyFinalProperty")
          .apply(
            List(
              target,
              JS.Literal(name.trim),
              JS.Function(
                None,
                Nil,
                JS.Block(List(JS.Return(value))))))
      } else {
        if (/*!topLevel ||*/ globalPrefix.path.isEmpty) {
          value.asVar(name)
        } else {
          JS.Assign(JS.path(globalPrefix.derive(name).path), value)
        }
      }
    }

    value match {
      case JS.Commented(comment, value) => JS.Commented(comment, sub(value))(value.pos)
      case _ => sub(value)
    }
  }
  def convertSingle(tree: Tree, topLevel: Boolean = false)
                   (implicit globalPrefix: GlobalPrefix,
                    guardedPrefixes: GuardedPrefixes): JS.Node = {
    convert(tree, topLevel).unique
  }

  def convert(tree: Tree, topLevel: Boolean = false)
             (implicit globalPrefix: GlobalPrefix,
              guardedPrefixes: GuardedPrefixes): List[JS.Node] = {



    if (tree.toString.contains("new scala.Array"))
      println("ARRAY IS: " + tree)

    implicit val p = pos(tree)
    val res: List[JS.Node] = //try {
      tree match {

        case q"$jsStringContext(scala.StringContext.apply(..$fragments)).js(..$args)" =>
          JS.Interpolation(
            fragments.map { case Literal(Constant(s: String)) => s },
            args.flatMap(convert(_))) :: Nil

        // Represent array with... array.
        case q"scala.Array.apply[..$tparams](..$values)" =>
          JS.JSONArray(values.map(convert(_).unique)) :: Nil

        case Apply(Select(a, N(op)), List(b)) if JS.binaryOperators(op) =>
          JS.BinOp(convert(a).unique, op, convert(b).unique) :: Nil

        //case q"new scala.Array[..$tparams]($length)($classTag)" =>
        case Apply(Apply(TypeApply(Select(New(arr), constr), List(tpt)), List(length)), List(classTag))
            if Option(tree.tpe).exists(_ <:< typeOf[Array[_]]) =>
          JS.new_("Array").apply(List(convert(length).unique)) :: Nil

        // Represent tuples as arrays (this will go well with ES6 destructuring assignments).
        case Apply(TypeApply(target, tparams), values)
            if Option(target.symbol).exists(_.fullName.toString.matches("""scala\.Tuple\d+""")) =>
          JS.JSONArray(values.map(convert(_).unique)) :: Nil

        case Apply(TypeApply(target, List(_, _)), pairs)
            if Option(tree.tpe).exists(_ <:< typeOf[Map[_, _]]) =>
          (try {
            JS.JSONObject(
              pairs.map({
                //case q"$key -> $value" =>
                case q"$assocBuilder[$keyType]($key).->[$valueType]($value)" =>
                  val Literal(Constant(keyString: String)) = key
                  keyString -> convert(value).unique
              }).toMap)
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
                // println("FAILED TO MATCH PAIR: " + pair)
                implicit val p = pos(pair)
                val convertedPair = convert(pair).unique
                val (predefs, pairRef) = pair match {
                  case _: JS.Ident =>
                    (
                      Nil,
                      convertedPair
                    )
                  case _ =>
                    needsPairVar = true
                    val pairRef = JS.Ident(pairName)
                    (
                      pairRef.assign(convertedPair) :: Nil,
                      pairRef
                    )
                }
                (
                  predefs,
                  pairRef.select(JS.Literal(0)),
                  p,
                  pairRef.select(JS.Literal(1)),
                  p
                )
            })
            assembleBlock(
              List(
                JS.newEmptyJSON.asVar(objName)
              ) ++
              (
                if (needsPairVar)
                  JS.VarDef(pairName, JS.NoNode) :: Nil
                else
                  Nil
              ) ++
              keyVals.flatMap({
                case (predefs, key, keyPos, value, valuePos) =>
                  implicit val p = keyPos
                  predefs :+
                  JS.Ident(objName).select(key).assign(value)
              }),
              JS.Ident(objName),
              applyArgs = List(JS.Ident("this")))
          }) :: Nil

        // case ClassDef(mods, name, tparams, Template(parents, self, q"def $init(..$vparams) = $initBody" :: body)) =>
        //   // val (constructor, body) = decls
        //   // val q"def $init(..$vparams) = $initBody" = constructor
        case ClassDef(mods, name, tparams, Template(parents, self, body)) =>
          val vparams = tree match {
            case q"class $name[..$tparams](..$vparams) { ..$body }" =>
              vparams
            case _ =>
              Nil
          }
          defineVar(
            name,
            JS.Function(
              None,
              vparams.map(param => JS.Ident(param.name)(pos(param))),
              JS.Block(
                body.flatMap({
                  case d: ValDef =>
                    implicit val p = pos(d)
                    convert(d) :+
                    JS.path("this." + d.name).assign(JS.Ident(d.name))
                  case _: DefDef =>
                    Nil
                  case t =>
                    convert(t)
                }) :+
                JS.Return(JS.Ident("this")))),
            topLevel = topLevel) ::
          body.collect({
            case d: DefDef if d.name != nme.CONSTRUCTOR =>
              implicit val p = pos(d)
              // TODO fix prefix mechanism
              val subGlobalPrefix = globalPrefix.derive(name + ".prototype")
              convert(d)(subGlobalPrefix, guardedPrefixes).unique
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
            assembleBlock(
              List(
                JS.newEmptyJSON.asVar(name),
                assembleBlock(
                  body.flatMap({
                    case d: ValOrDefDef if !d.mods.hasFlag(Flag.LAZY) && d.name != nme.CONSTRUCTOR =>
                      implicit val p = pos(d)
                      convert(d) :+ JS.Ident(name).select(d.name).assign(d.name)
                    case t =>
                      convert(t)
                  }),
                  JS.NoNode,
                  applyArgs = JS.Ident(name) :: Nil
                )
              ),
              JS.Ident(name)
            ),
            isLazy = true,
            topLevel = topLevel
          ) :: Nil

        case SuperCall(/*qual, mix, */methodName, args) =>
          JS.Ident("goog").apply(
            "base",
            List(JS.Ident("this")) ++
            (
              if (methodName == nme.CONSTRUCTOR) Nil
              else JS.Literal(methodName.toString) :: Nil
            ) ++
            args.flatMap(convert(_))) :: Nil

        case Apply(target, args) =>
          convert(target).unique.apply(
            args.flatMap(convert(_))) :: Nil

        case Import(_, _) =>
          Nil

        case Super(qual, mix) =>
          JS.Ident("super") :: Nil // TODO

        case If(cond, thenp, elsep) =>
          JS.If(convert(cond).unique, convert(thenp).unique, convert(elsep).unique) :: Nil // TODO

        case Block(stats, value) =>
          // TODO better job here
          // if (value.tpe <:< typeOf[Unit])
          assembleBlock(
            stats.flatMap(convert(_)),
            convert(value).unique) :: Nil

        case Select(target, name) =>
          val convTarget = convert(target).unique
          if (name == nme.CONSTRUCTOR)
            convTarget :: Nil
          else
            convTarget.select(name) :: Nil

        case Ident(name) =>
          resolve(tree.symbol).getOrElse {
            JS.Ident(name)
          } :: Nil

        case Literal(Constant(v)) =>
          if (v == (())) JS.NoNode :: Nil
          else JS.Literal(v) :: Nil

        case PackageDef(pid, stats) =>
          val subGlobalPrefix = pid match {
            case EmptyTree | Ident(N("<empty>")) =>
              globalPrefix
            case _ =>
              globalPrefix.derive(pid.toString)
          }
          stats.flatMap(convert(_, topLevel = true)(subGlobalPrefix, guardedPrefixes))

        case This(qual) =>
          JS.Ident("this") :: Nil
          //JS.Ident(qual.toString) :: Nil // TODO

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
              isLazy = mods.hasFlag(Flag.LAZY),
              topLevel = topLevel) :: Nil
          }

        case Function(vparams, body) =>
          convertFunction(vparams, body, thisType = None, isConstructor = false) :: Nil

        case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
          val termSym = tree.symbol.asTerm
          if (name == nme.CONSTRUCTOR) {
            Nil
          } else if (termSym.isAccessor) {
            if (termSym.isLazy) {
              val Block(List(Assign(lhs, rhs2)), value) = rhs
              // Assignment of lazy val, introduced by typer.
              defineVar(
                name.toString,//globalPrefix.derive(lhs.symbol.name).path,
                convert(rhs2).unique,
                isLazy = true) :: Nil
            } else {
              Nil
            }
          } else {
            defineVar(
              name.toString,//globalPrefix.derive(name).path,
              convertFunction(vparamss.flatten, rhs, thisType = Some(tree.symbol.owner), isConstructor = false),
              topLevel = topLevel) :: Nil
          }

        case Assign(lhs, rhs) =>
          convert(lhs).unique.assign(convert(rhs).unique) :: Nil

        case New(target) =>
          JS.New(
            resolve(target.symbol) match {
              case Some(resolved) => resolved
              case None => convert(target).unique
            }) :: Nil

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
