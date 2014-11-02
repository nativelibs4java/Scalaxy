package scalaxy.fastcaseclasses

import scala.collection.breakOut

import scala.reflect.api.Universe

import scala.tools.nsc.Mode

/**
 * Make case classes faster!
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

      comps.classDef.tparams.isEmpty &&
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
    def makeMethod(name: TermName, mods: Modifiers, owner: Symbol, params: List[(TermName, Type)], retTpe: Type, rhsMaker: List[Tree] => Tree): DefDef = {
      val methodSym = owner.newMethodSymbol(name, NoPosition, mods.flags)
      owner.info.decls enter methodSym

      val (methodTpe, paramDefs, paramRefs) = params match {
        case Nil =>
          (new NullaryMethodType(retTpe), Nil, Nil)

        case _ =>
          val (paramDefs: List[ValDef], paramRefs: List[Tree]) =
            (for ((paramName, paramTpe) <- params) yield {
              val paramSym = methodSym.newTermSymbol(paramName, newFlags = Flag.PARAM)
              paramSym.info = paramTpe

              val paramDef =
                ValDef(Modifiers(Flag.PARAM), paramName, TypeTree(paramTpe), EmptyTree)
              val paramRef = Ident(paramSym)
              setSym(paramSym, paramTpe, List(paramDef, paramRef))

              (paramDef, paramRef)
            }).unzip

          (new MethodType(paramDefs.map(_.symbol), retTpe), paramDefs, paramRefs)
      }

      val vparamss = if (paramDefs.isEmpty) Nil else List(paramDefs)
      val rhs = atOwner(owner) {
        atOwner(methodSym) {
          localTyper.typed(rhsMaker(paramRefs), pt = retTpe)
        }
      }
      val methodDef = q"$mods def $name(...$vparamss): $retTpe = $rhs"
      setSym(methodSym, methodTpe, List(methodDef))

      methodDef
    }

    override def expandTreeOrCompanions(toc: Either[Companions, Tree]) = toc match {
      case Left(
        comps @ Companions(
          classDef @ q"$mods class ${ _ }[..${ _ }](..$args) extends ..${ _ } { ..${ _ } }",
          moduleDef)) if shouldOptimize(comps) =>

        val sym = classDef.symbol
        val tpe = sym.asType.toType

        val needsTupleLikeMethods = args.size > 1

        def tupleLikeMethods = for ((arg, i) <- args.zipWithIndex) yield {
          // q"def ${TermName("_" + (i + 1))}: ${arg.tpt.tpe} = $sym.this.${arg.name}"
          makeMethod(
            name = TermName("_" + (i + 1)),
            mods = Modifiers(Flag.STABLE),
            owner = classDef.symbol,
            params = Nil,
            retTpe = arg.tpt.tpe,
            rhsMaker = _ => q"$sym.this.${arg.name}")
        }

        // q"def isEmpty: ${BooleanTpe} = false",
        val isEmptyDef =
          makeMethod(
            name = TermName("isEmpty"),
            mods = Modifiers(Flag.STABLE),
            owner = classDef.symbol,
            params = Nil,
            retTpe = BooleanTpe,
            rhsMaker = _ => q"false")

        // q"def get: $tpe = $sym.this")
        def getDef =
          if (needsTupleLikeMethods)
            makeMethod(
              name = TermName("get"),
              mods = Modifiers(Flag.STABLE),
              owner = classDef.symbol,
              params = Nil,
              retTpe = tpe,
              rhsMaker = _ => q"$sym.this")
          else {
            val List(arg) = args
            makeMethod(
              name = TermName("get"),
              mods = Modifiers(Flag.STABLE),
              owner = classDef.symbol,
              params = Nil,
              retTpe = arg.tpt.tpe,
              rhsMaker = _ => q"$sym.this.${arg.name}")
          }

        val newObjectMethods = List(
          // q"def unapply($param: $tpe): $tpe = $param",
          makeMethod(
            name = TermName("unapply"),
            mods = Modifiers(Flag.STABLE),
            owner = classDef.symbol,
            params = List(TermName("param") -> tpe),
            retTpe = tpe,
            rhsMaker = { case List(paramRef) => paramRef }),
          // q"""
          //   //import scala.language.implicitConversions
          //   implicit def toOption($param: $tpe): scala.Option[$tpe] = Option[$tpe]($param)
          // """)
          makeMethod(
            name = TermName("toOption"),
            mods = Modifiers(Flag.STABLE | Flag.IMPLICIT),
            owner = classDef.symbol,
            params = List(TermName("param") -> tpe),
            retTpe = appliedType(optionSym.asType.toType, List(tpe)),
            rhsMaker = { case List(paramRef) => q"scala.Option[$tpe]($paramRef)" }))

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

        // println(s"result = $result")
        info(comps.classDef.pos, s"Optimized case class ${classDef.name} for Option-less named extraction.")

        expandTreeOrCompanions(result)

      case _ =>
        super.expandTreeOrCompanions(toc)
    }
  } transform tree
}
