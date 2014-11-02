package scalaxy.fastcaseclasses

import scala.collection.breakOut

import scala.reflect.api.Universe

import scala.tools.nsc.Mode

/**
 * Make case classes faster by making them named-based extractors.
 */
trait TypedFastCaseClassesTransforms extends CompanionTransformers {
  // val global: Universe
  import global._
  import definitions._

  def info(pos: Position, msg: String): Unit

  private[this] lazy val optionSym = rootMirror.staticClass("scala.Option")

  def transformTyped(tree: Tree, unit: CompilationUnit) = new CompanionsTransformer(unit) {

    def alterMethods(
      template: Template,
      owner: Symbol,
      filter: Tree => Boolean,
      newMethods: List[DefDef]): Template = {
      assert(owner != NoSymbol)
      atOwner(owner) {
        treeCopy.Template(template,
          parents = template.parents,
          self = template.self,
          body = template.body.filter(filter) ++ newMethods
        )
      }
    }

    def containsMethod(template: Template, name: String) =
      template.body.exists {
        case DefDef(_, name, _, _, _, _) if name.toString == name =>
          true
        case _ =>
          false
      }

    def shouldOptimize(comps: Companions): Boolean = {
      // TODO test type members for forbidden symbols
      // false
      val sym = comps.classDef.symbol.asType.toType
      val osym = comps.moduleDef.symbol.moduleClass.asType.toType

      !comps.classDef.symbol.name.toString.endsWith("_NoFastCaseClasses") &&
        comps.classDef.tparams.forall({
          case t @ TypeDef(mods, _, _, _) if mods.hasFlag(Flag.COVARIANT) || mods.hasFlag(Flag.CONTRAVARIANT) =>
            false
          case _ =>
            true
        }) &&
        (sym member TermName("isEmpty")) == NoSymbol &&
        (sym member TermName("get")) == NoSymbol &&
        (sym member TermName("_1")) == NoSymbol &&
        (osym member TermName("toOption")) == NoSymbol
    }

    def setSym(sym: Symbol, tpe: Type, trees: List[Tree]) {
      sym.info = tpe
      for (tree <- trees)
        tree.setSymbol(sym).setType(tpe)
    }
    def typeMethod(owner: Symbol, tree: DefDef, rhsMaker: List[Tree] => Tree): DefDef = {

      val (methodSym, methodTpe, paramRefs, mods: Modifiers, name: TermName, vparamss, retTpt) = tree match {
        case q"$mods def $name: $retTpt = $rhs" =>
          val methodSym = owner.newMethodSymbol(name, NoPosition, mods.flags)
          (
            methodSym,
            new NullaryMethodType(retTpt.tpe),
            Nil,
            mods, name, Nil, retTpt
          )

        case q"$mods def $name(..$vparams): $retTpt = $rhs" =>
          val methodSym = owner.newMethodSymbol(name, NoPosition, mods.flags)
          val (paramDefs: List[ValDef], paramRefs: List[Tree]) =
            (for (param <- vparams) yield {
              val paramSym = methodSym.newTermSymbol(param.name, newFlags = Flag.PARAM)
              paramSym.info = param.tpt.tpe

              val paramDef =
                ValDef(Modifiers(Flag.PARAM), param.name, TypeTree(param.tpt.tpe), EmptyTree)
              val paramRef = Ident(paramSym)
              setSym(paramSym, param.tpt.tpe, List(paramDef, paramRef))

              (paramDef, paramRef)
            }).unzip

          (
            methodSym,
            new MethodType(paramDefs.map(_.symbol), retTpt.tpe),
            paramRefs,
            mods, name, List(paramDefs), retTpt
          )
      }
      owner.info.decls enter methodSym

      val rhs = atOwner(owner) {
        atOwner(methodSym) {
          localTyper.typed(rhsMaker(paramRefs), pt = retTpt.tpe)
        }
      }

      val methodDef = q"$mods def $name(...$vparamss): $retTpt = $rhs"
      setSym(methodSym, methodTpe, List(methodDef))

      methodDef
    }

    // def makeMethod(name: TermName, mods: Modifiers, owner: Symbol, params: List[(TermName, Type)], retTpe: Type, rhsMaker: List[Tree] => Tree): DefDef = {
    //   val methodSym = owner.newMethodSymbol(name, NoPosition, mods.flags)
    //   owner.info.decls enter methodSym

    //   val (methodTpe, paramDefs, paramRefs) = params match {
    //     case Nil =>
    //       (new NullaryMethodType(retTpe), Nil, Nil)

    //     case _ =>
    //       val (paramDefs: List[ValDef], paramRefs: List[Tree]) =
    //         (for ((paramName, paramTpe) <- params) yield {
    //           val paramSym = methodSym.newTermSymbol(paramName, newFlags = Flag.PARAM)
    //           paramSym.info = paramTpe

    //           val paramDef =
    //             ValDef(Modifiers(Flag.PARAM), paramName, TypeTree(paramTpe), EmptyTree)
    //           val paramRef = Ident(paramSym)
    //           setSym(paramSym, paramTpe, List(paramDef, paramRef))

    //           (paramDef, paramRef)
    //         }).unzip

    //       (new MethodType(paramDefs.map(_.symbol), retTpe), paramDefs, paramRefs)
    //   }

    //   val vparamss = if (paramDefs.isEmpty) Nil else List(paramDefs)
    //   val rhs = atOwner(owner) {
    //     atOwner(methodSym) {
    //       localTyper.typed(rhsMaker(paramRefs), pt = retTpe)
    //     }
    //   }
    //   val methodDef = q"$mods def $name(...$vparamss): $retTpe = $rhs"
    //   setSym(methodSym, methodTpe, List(methodDef))

    //   methodDef
    // }

    override def expandTreeOrCompanions(toc: Either[Companions, Tree]) = toc match {
      case Left(
        comps @ Companions(
          classDef @ q"$mods class ${ _ }[..${ _ }](..$args) extends ..${ _ } { ..${ _ } }",
          moduleDef)) if shouldOptimize(comps) =>

        val sym = classDef.symbol
        val tpe = sym.asType.toType

        val needsTupleLikeMethods = args.size > 1

        def tupleLikeMethods = for ((arg, i) <- args.zipWithIndex) yield {
          typeMethod(owner = classDef.symbol,
            q"def ${TermName("_" + (i + 1))}: ${arg.tpt.tpe} = ???",
            rhsMaker = _ => q"$sym.this.${arg.name}")
          // makeMethod(
          //   name = TermName("_" + (i + 1)),
          //   mods = Modifiers(Flag.STABLE),
          //   owner = classDef.symbol,
          //   params = Nil,
          //   retTpe = arg.tpt.tpe,
          //   rhsMaker = _ => q"$sym.this.${arg.name}")
        }

        // q"def isEmpty: ${BooleanTpe} = false",
        val isEmptyDef =
          typeMethod(owner = classDef.symbol,
            q"def isEmpty: ${BooleanTpe} = ???",
            rhsMaker = _ => q"false")
        // makeMethod(
        //   name = TermName("isEmpty"),
        //   mods = Modifiers(Flag.STABLE),
        //   owner = classDef.symbol,
        //   params = Nil,
        //   retTpe = BooleanTpe,
        //   rhsMaker = _ => q"false")

        // q"def get: $tpe = $sym.this")
        def getDef =
          if (needsTupleLikeMethods)
            typeMethod(owner = classDef.symbol,
              q"def get: $tpe = ???",
              rhsMaker = _ => q"$sym.this")
          // makeMethod(
          //   name = TermName("get"),
          //   mods = Modifiers(Flag.STABLE),
          //   owner = classDef.symbol,
          //   params = Nil,
          //   retTpe = tpe,
          //   rhsMaker = _ => q"$sym.this")
          else {
            val List(arg) = args
            typeMethod(owner = classDef.symbol,
              q"def get: ${arg.tpt.tpe} = ???",
              rhsMaker = _ => q"$sym.this.${arg.name}")
            // makeMethod(
            //   name = TermName("get"),
            //   mods = Modifiers(Flag.STABLE),
            //   owner = classDef.symbol,
            //   params = Nil,
            //   retTpe = arg.tpt.tpe,
            //   rhsMaker = _ => q"$sym.this.${arg.name}")
          }

        val optionTpe = appliedType(optionSym.asType.toType, List(tpe))
        val newObjectMethods = List(
          typeMethod(owner = classDef.symbol,
            q"def unapply(param: $tpe): $tpe = ???",
            rhsMaker = { case List(paramRef) => paramRef }),
          // makeMethod(
          //   name = TermName("unapply"),
          //   mods = Modifiers(Flag.STABLE),
          //   owner = classDef.symbol,
          //   params = List(TermName("param") -> tpe),
          //   retTpe = tpe,
          //   rhsMaker = { case List(paramRef) => paramRef }),
          typeMethod(owner = classDef.symbol,
            q"""
              //import scala.language.implicitConversions
              implicit def toOption(param: $tpe): $optionTpe = ???
            """,
            rhsMaker = { case List(paramRef) => q"scala.Option[$tpe]($paramRef)" }))
        // makeMethod(
        //   name = TermName("toOption"),
        //   mods = Modifiers(Flag.STABLE | Flag.IMPLICIT),
        //   owner = classDef.symbol,
        //   params = List(TermName("param") -> tpe),
        //   retTpe = appliedType(optionSym.asType.toType, List(tpe)),
        //   rhsMaker = { case List(paramRef) => q"scala.Option[$tpe]($paramRef)" }))

        val result = Left(Companions(
          classDef = treeCopy.ClassDef(classDef,
            mods = classDef.mods,
            name = classDef.name,
            tparams = classDef.tparams,
            impl = alterMethods(
              classDef.impl,
              classDef.symbol,
              _ => true,
              if (needsTupleLikeMethods)
                List(getDef, isEmptyDef) ++ tupleLikeMethods
              else if (args.nonEmpty)
                List(getDef, isEmptyDef)
              else
                List(isEmptyDef)
            )
          ),
          moduleDef = treeCopy.ModuleDef(moduleDef,
            mods = moduleDef.mods,
            name = moduleDef.name,
            impl = alterMethods(
              moduleDef.impl,
              moduleDef.symbol,
              _ match {
                case DefDef(_, name, _, _, _, _) if name.toString == "unapply" => false
                case _ => true
              },
              newObjectMethods
            )
          )
        ))

        // if (unit.source.file.toString.contains("PathResolver.scala"))
        //   println(s"result = $result")

        info(comps.classDef.pos, s"Optimized case class ${classDef.name} for Option-less named extraction.")

        expandTreeOrCompanions(result)

      case _ =>
        super.expandTreeOrCompanions(toc)
    }
  } transform tree
}
