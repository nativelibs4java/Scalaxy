package scalaxy.fastcaseclasses

import scala.collection.breakOut

import scala.reflect.api.Universe

/**
 * Make case classes faster!
 */
trait UntypedFastCaseClassesTransforms extends ExpandingTransformers {
  val global: Universe
  import global._

  def info(pos: Position, msg: String): Unit

  private[this] lazy val preservedFlags = List(
    Flag.ABSTRACT,
    Flag.FINAL,
    Flag.PRIVATE,
    Flag.PROTECTED,
    Flag.SEALED)

  private[this] def removeCaseFlag(mods: Modifiers): Modifiers = {
    val Modifiers(_, privateWithin, annotations) = mods
    var flags = NoFlags
    for (flag <- preservedFlags; if mods.hasFlag(flag)) {
      flags |= flag
    }
    Modifiers(flags, privateWithin, annotations)
  }

  private[this] def getMethodNames(trees: List[Tree]): List[String] = trees collect {
    case DefDef(_, name, _, _, _, _) => name.toString
  }

  private[this] lazy val forbiddenMethodNames = Set(
    "isEmpty",
    "get",
    "productElement",
    "productArity",
    "productPrefix",
    "canEqual",
    "hashCode",
    "toString",
    "equals",
    "copy") ++ (1 to 22).map("_" + _)

  // def computeHash()
  def transformUntyped(tree: Tree, fresh: String => String) = new ExpandingTransformer {
    override def expandOne(tree: Tree, hasCompanion: Boolean): List[Tree] = tree match {
      // TODO: treat case of size == 1 separately (don't return this, no _1, extend AnyVal if no known subclasses + no superclass)
      case q"case class $typeName(..$args) extends ..$parents { ..$decls }" if enabled && !hasCompanion && !getMethodNames(decls).exists(forbiddenMethodNames) && args.size > 1 =>
        val termName = TermName(typeName.toString)

        val ClassDef(oldMods, _, _, _) = tree
        val mods = removeCaseFlag(oldMods)
        val param = TermName(fresh("param"))

        // Note: don't extend scala.Product2... to avoid boxing of return value of _1, _2...
        val newParents = parents :+ tq"scala.Product"

        val indexedGetters = for ((arg, i) <- args.zipWithIndex) yield {
          val m = TermName("_" + (i + 1))
          q"def $m: ${arg.tpt} = this.${arg.name}"
        }

        val indexedCases = for ((arg, i) <- args.zipWithIndex) yield {
          cq"$i => this.${arg.name}"
        }

        val hashImpl = {
          // if (this.${arg.name} == null) 0
          // else this.${arg.name}.hashCode)
          val stats = for (arg <- args) yield q"""
            acc = scala.runtime.Statics.mix(acc,
              // TODO check that this doesn't really Int.unbox(null) at runtime
              if (this.${arg.name} == null.asInstanceOf[${arg.tpt}]) 0
              else this.${arg.name}.hashCode)
          """

          q"""
            var acc: Int = -889275714;
            ..$stats
            scala.runtime.Statics.finalizeHash(acc, ${args.size})
          """
        }

        val equalImpl = {
          val isInstanceOf = q"o.isInstanceOf[$typeName]"
          val result = if (args.isEmpty)
            isInstanceOf
          else {
            val conds = for (arg <- args) yield q"oo.${arg.name} == this.${arg.name}"
            q"""
              $isInstanceOf && {
                val oo = o.asInstanceOf[$typeName]
                ..${conds.reduce((a, b) => q"$a && $b")}
            }
            """
          }

          q"override def equals(o: Any) = $result"
        }

        val functionTpt = {
          val funName = TypeName("Function" + args.size)
          val targs = args.map(_.tpt) :+ tq"$typeName"
          tq"scala.$funName[..$targs]"
        }

        val copyImpl = {
          val params = for (arg <- args) yield {
            ValDef(Modifiers(Flag.PARAM | Flag.DEFAULTPARAM), arg.name, arg.tpt, q"this.${arg.name}")
          }
          q"def copy(..$params) = new $typeName(..${args.map(_.name)})"
        }

        val result: List[Tree] = List(q"""
          $mods class $typeName(..$args) extends ..$newParents {
            ..${expandMany(decls)}

            def isEmpty = false
            def get = this
            ..$indexedGetters

            override def productElement(i: Int) =
              ${Match(q"(i: @scala.annotation.switch)", indexedCases)}
            override def productArity = ${args.length}
            override def productPrefix = ${typeName.toString}
            override def canEqual(o: Any): Boolean = o.isInstanceOf[$typeName]
            override def hashCode = $hashImpl
            override def toString = scala.runtime.ScalaRunTime._toString(this)
            $equalImpl
            $copyImpl
          }
        """, q"""
          object $termName extends $functionTpt {
            override def toString = ${typeName.toString}
            override def apply(..$args) = new $typeName(..${args.map(_.name)})
            def unapply($param: $typeName) = $param
            import scala.language.implicitConversions
            implicit def toOption($param: $typeName) = Option($param)
          }
        """)
        // println(s"result = $result")
        info(tree.pos, s"Optimized $typeName for Option-less named extraction.")

        result

      case _ =>
        super.expandOne(tree, hasCompanion = hasCompanion)
    }
  } transform tree
}
