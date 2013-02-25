// Author: Olivier Chafik (http://ochafik.com)
// Feel free to modify and reuse this for any purpose ("public domain / I don't care").
package scalaxy.extensions

import scala.collection.mutable

import scala.reflect.internal._
import scala.reflect.ClassTag

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.transform.TypingTransformers

/**
 *  To understand / reproduce this, you should use paulp's :power mode in the scala console:
 *
 *  scala
 *  > :power
 *  > :phase parser // will show us ASTs just after parsing
 *  > val Some(List(ast)) = intp.parse("@scalaxy.extension[Int] def str = self.toString")
 *  > nodeToString(ast)
 *  > val DefDef(mods, name, tparams, vparamss, tpt, rhs) = ast // play with extractors to explore the tree and its properties.
 */
class MacroExtensionsComponent(
  val global: Global, 
  macroExtensions: Boolean = true, 
  runtimeExtensions: Boolean = false,
  useThisForSelf: Boolean = true,
  useUntypedReify: Boolean = true)
    extends PluginComponent
    with TypingTransformers
    with TreeReifyingTransformers
    with Extensions
{
  import global._
  import definitions._
  import Flag._

  override val phaseName = "scalaxy-extensions"

  override val runsRightAfter = Some("parser")
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List[String]("namer")

  private final val selfName = "self"
  
  def newPhase(prev: Phase): StdPhase = new StdPhase(prev) 
  {
    def apply(unit: CompilationUnit) {
      val onTransformer = new Transformer
      {
        object ExtendAnnotation {
          def unapply(tree: Tree) = Option(tree) collect {
            case Apply(Select(New(AppliedTypeTree(name, List(tpt))), initName), Nil)
            if initName == nme.CONSTRUCTOR && name.toString == "scalaxy.extension" =>
              tpt
            case Apply(Select(New(name), initName), List(targetValueTpt))
            if initName == nme.CONSTRUCTOR && name.toString.matches("extend|scalaxy.extend") =>
              val msg = "Please use `@scalaxy.extension[T]` instead of `@extend(T)` or `@scalaxy.extend(T)`"
              unit.error(tree.pos, msg)
              sys.error(msg)
              null
          }
        }
        def banAmbiguousThis(tree: Tree, rootTpt: Tree) {
          new DefsTraverser {
            def implDefs = parents.filter(_.isInstanceOf[ImplDef])
            override def traverse(tree: Tree) = tree match {
              case This(q) if q.isEmpty && !implDefs.isEmpty =>
                unit.error(tree.pos, "Ambiguous reference to `this`. Please refine as any of " + (rootTpt :: implDefs.map(_.name)).map(_ + ".this").mkString(", "))
              case _ =>
                super.traverse(tree)
            }
          }.traverse(tree)
        }
        def banVariableNames(names: Set[String], root: Tree) {
          val banTraverser = new Traverser {
            override def traverse(tree: Tree) = {
              tree match {
                case d: DefTree if names.contains(Option(d.name).map(_.toString).getOrElse("")) =>
                  unit.error(tree.pos, s"Cannot redefine name ${d.name}")
                case _ =>
              }
              super.traverse(tree)
            }
          }
          banTraverser.traverse(root)
        }

        def newExtensionName(name: Name) =
          unit.fresh.newName("scalaxy$extensions$" + name + "$")

        def transformMacroExtension(tree: DefDef): List[Tree] =
        {
          val DefDef(Modifiers(flags, privateWithin, annotations), name, tparams, vparamss0, tpt, rhs) = tree
          val extendAnnotationOpt = annotations.find(ExtendAnnotation.unapply(_) != None)
          extendAnnotationOpt match
          {
            case Some(extendAnnotation @ ExtendAnnotation(targetTpt)) =>
              if (tpt.isEmpty)
                unit.error(tree.pos, "Macro extensions require explicit return type annotation")

              val extensionName = newExtensionName(name)
              val typeNamesInTarget = getTypeNames(targetTpt).toSet
              val (outerTParams, innerTParams) =
                tparams.partition { 
                  case tparam @ TypeDef(_, tname, _, _) => 
                    typeNamesInTarget.contains(tname) 
                }
                
              val typeNamesInSignature = typeNamesInTarget ++ tparams.flatMap(getTypeNames(_))
              
              def containsReferenceToTParams(tpt: Tree) = {
                val typeNames = getTypeNames(tpt)
                typeNames.exists((tpn: TypeName) => typeNamesInSignature.contains(tpn))
              }
              
              val tparamNames =
                (tparams.map { case tparam @ TypeDef(_, tname, _, _) => tname.toString }).toSet
              
              val selfTreeName: TermName = unit.fresh.newName("selfTree$")
              val selfExprName: TermName = unit.fresh.newName("self$Expr$")
              
              // Don't rename the context, otherwise explicit macros are hard to write.
              val contextName: TermName = "c" //unit.fresh.newName("c")

              def isImplicit(mods: Modifiers) = 
                ((mods.flags & IMPLICIT): Long) != 0
                
              def isByName(mods: Modifiers) = 
                ((mods.flags & BYNAMEPARAM): Long) != 0
                
              val isMacro = 
                ((flags & MACRO): Long) != 0
                
              val byValueParamExprNames = mutable.HashMap[String, String]()
              val vparamss = vparamss0.map(_.map {
                case vd @ ValDef(pmods, pname, ptpt, prhs) =>
                  val newPTpt = 
                    if (isByName(pmods)) {
                      if (isMacro)
                        unit.error(tree.pos, "Extensions expressed as macros cannot take by-name arguments")
                      
                      if (isImplicit(pmods))
                        unit.error(vd.pos, "Scalaxy does not support for by-name implicit params yet")
                    
                      val AppliedTypeTree(target, List(newPTpt)) = ptpt
                      assert(target.toString == "_root_.scala.<byname>")
                      newPTpt
                    } else {
                      byValueParamExprNames += pname.toString -> unit.fresh.newName(pname + "$Expr$") 
                      ptpt 
                    }
                    
                  if (!prhs.isEmpty)
                    unit.error(prhs.pos, "Default parameters are not supported yet")

                  ValDef(
                    pmods, 
                    // Due to https://issues.scala-lang.org/browse/SI-7170, we can have evidence name clashes.
                    pname,//if (isImplicit(pmods)) newTermName(unit.fresh.newName(pname + "$")) else pname, 
                    newPTpt, 
                    prhs)
              })
              
              def getRealParamName(name: TermName): TermName = {
                val n = name.toString
                byValueParamExprNames.get(n).getOrElse(n): String
              }
              def isByValueParam(name: TermName): Boolean = 
                byValueParamExprNames.contains(name.toString)
              
              
              val variableNames = (selfName.toString :: vparamss.flatten.map(_.name.toString)).toSet
              banVariableNames(
                variableNames + "reify", 
                rhs
              )
              banAmbiguousThis(rhs, targetTpt)
              
              def typeGetter(tpt: Tree): Tree = {
                //if (containsReferenceToTParams(tpt))
                Select(
                  TypeApply(
                    termPath(contextName + ".universe.weakTypeTag"),
                    List(tpt)),
                  "tpe")
              }
              
              def typeTreeGetter(tpt: Tree): Tree = {
                Apply(
                  Ident("TypeTree": TermName), 
                  List(typeGetter(tpt)))
              }
              
              List(
                newImportMacros(tree.pos),
                ClassDef(
                  Modifiers((flags | IMPLICIT) -- MACRO, privateWithin, Nil),
                  extensionName: TypeName,
                  outerTParams,
                  Template(
                    List(parentTypeTreeForImplicitWrapper(targetTpt.toString: TypeName)),
                    newSelfValDef(),
                    genParamAccessorsAndConstructor(
                      List(selfName -> targetTpt)
                    ) :+
                    // Copying the original def over, without its @scalaxy.extend annotation.
                    DefDef(
                      Modifiers((flags | MACRO) -- BYNAMEPARAM, privateWithin, annotations.filter(_ ne extendAnnotation)),
                      name,
                      innerTParams,
                      vparamss.map(_.map {
                        case ValDef(pmods, pname, ptpt, prhs) =>
                          ValDef(pmods.copy(flags = pmods.flags -- BYNAMEPARAM), getRealParamName(pname), ptpt, prhs)
                      }),
                      tpt,
                      {
                        val macroPath = termPath(extensionName + "." + name)
                        if (tparams.isEmpty)
                          macroPath
                        else
                          TypeApply(
                            macroPath,
                            tparams.map {
                              case TypeDef(_, tname, _, _) =>
                                Ident(tname.toString: TypeName)
                            }
                          )
                      }
                    )
                  )
                ),
                // Macro implementations module.
                ModuleDef(
                  NoMods,
                  extensionName,
                  Template(
                    List(typePath("scala.AnyRef")),
                    newSelfValDef(),
                    genParamAccessorsAndConstructor(Nil) :+
                    DefDef(
                      NoMods,
                      name,
                      tparams, // TODO map T => T : c.WeakTypeTag
                      List(
                        List(
                          ValDef(Modifiers(PARAM), contextName, typePath("scala.reflect.macros.Context"), EmptyTree)
                        )
                      ) ++
                      (
                        if (vparamss.flatten.isEmpty)
                          Nil
                        else
                          vparamss.map(_.map {
                            case ValDef(pmods, pname, ptpt, prhs) =>
                              ValDef(
                                Modifiers(PARAM),
                                getRealParamName(pname),
                                newExprType(contextName, ptpt),
                                EmptyTree)
                          })
                      ) ++
                      (
                        if (tparams.isEmpty)
                          Nil
                        else
                          List(
                            tparams.map {
                              case tparam @ TypeDef(_, tname, _, _) =>
                                ValDef(
                                  Modifiers(IMPLICIT | PARAM),
                                  unit.fresh.newName("evidence$"),
                                  AppliedTypeTree(
                                    typePath(contextName + ".WeakTypeTag"),
                                    List(Ident(tname))),
                                  EmptyTree)
                            }
                          )
                      ),
                      newExprType(contextName, tpt),
                      Block(
                        //newImportAll(termPath(contextName: T), tree.pos),
                        newImportAll(termPath(contextName + ".universe"), tree.pos),
                        ValDef(
                          NoMods,
                          selfTreeName,
                          newEmptyTpt(),
                          Match(
                            Annotated(
                              Apply(
                                Select(
                                  New(
                                    typePath("scala.unchecked")
                                  ),
                                  nme.CONSTRUCTOR
                                ),
                                Nil),
                              termPath(contextName + ".prefix.tree")),
                            List(
                              CaseDef(
                                Apply(
                                  Ident("Apply": TermName),
                                  List(
                                    Ident("_": TermName),
                                    Apply(
                                      Ident("List": TermName),
                                      List(
                                        Bind(
                                          selfTreeName,
                                          Ident("_": TermName)))))),
                                Ident(selfTreeName))
                            )
                          )
                        ),
                        ValDef(
                          NoMods,
                          if (isMacro) selfName else selfExprName,
                          newEmptyTpt,//newExprType(contextName, targetTpt),
                          newExpr(contextName, targetTpt, Ident(selfTreeName: TermName))),
                        {
                          if (isMacro) {
                            def selfTransformer = new Transformer {
                              override def transform(tree: Tree) = tree match {
                                case This(n) 
                                if n.isEmpty || 
                                   n.toString == targetTpt.toString =>
                                  Ident(selfName)
                                case Ident(n: TermName) if n.toString == selfName =>
                                  unit.warning(tree.pos, s"'$selfName' is deprecated, please use 'this' instead")
                                  tree
                                case _ =>
                                  super.transform(tree)
                              }
                            }
                            // Extension body is already expressed as a macro, like `macro
                            val implicits = vparamss.flatten collect {
                              case ValDef(pmods, pname, ptpt, prhs) if isImplicit(pmods) =>
                                DefDef(
                                  Modifiers(IMPLICIT),
                                  newTermName(unit.fresh.newName(pname.toString + "$")),
                                  Nil,
                                  Nil,
                                  newEmptyTpt,//newExprType(contextName, ptpt),
                                  newExpr(contextName, ptpt, Ident(getRealParamName(pname))))
                            }
                            Block(implicits :+ (if (useThisForSelf) selfTransformer.transform(rhs) else rhs): _*)
                          } else {
                            val splicer = new Transformer {
                              override def transform(tree: Tree) = tree match {
                                case This(n) if useThisForSelf && n.isEmpty =>
                                  Ident(selfName)
                                case Ident(n: TermName) 
                                if useThisForSelf && n.toString == selfName =>
                                  unit.warning(tree.pos, s"'$selfName' is deprecated, please use 'this' instead")
                                  tree
                                case Ident(n: TermName) 
                                if variableNames.contains(n.toString) &&
                                   n.toString != selfName &&
                                   !isByValueParam(n) && 
                                   !useUntypedReify =>
                                  newSplice(n)
                                case _ =>
                                  super.transform(tree)
                              }
                            }
                            val selfParam =
                              ValDef(
                                Modifiers(LOCAL), 
                                selfName, 
                                newEmptyTpt,//targetTpt.duplicate, 
                                newSplice(selfExprName)
                              )
                              
                            val byValueParams = vparamss.flatten collect {
                              case ValDef(pmods, pname, ptpt, prhs) 
                              if isByValueParam(pname) =>
                                ValDef(
                                  Modifiers(if (isImplicit(pmods)) LOCAL | IMPLICIT else LOCAL),
                                  pname,
                                  newEmptyTpt,//ptpt,
                                  // TODO pass implicits if (useUntypedReify)
                                  newSplice(getRealParamName(pname)))
                            }

                            // When using untyped reification, only pass implicits through. 
                            val result = if (useUntypedReify) {
                              /*val rei = Block(
                                //byValueParams.filter(vd => isImplicit(vd.mods)) :+
                                splicer.transform(rhs)//: _*
                              )*/
                              val rei = splicer.transform(rhs)

                              val untypedResult = 
                                new TreeReifyingTransformer(
                                  exprSplicer = tn => {
                                    val s = tn.toString
                                    if (s == selfName)
                                      Some(Select(Ident(selfExprName: TermName), "tree"))
                                    else
                                      byValueParamExprNames.get(s).map(n =>
                                        Select(Ident(n: TermName), "tree"))
                                  },
                                  typeGetter = typeGetter(_),
                                  typeTreeGetter = typeTreeGetter(_)
                                ).transform(rei)
                              
                              newExpr(
                                contextName, 
                                tpt,
                                Block(
                                  Apply(Ident("println"), List(untypedResult)),
                                  //untypedResult))
                                  Apply(
                                    termPath(contextName + ".typeCheck"), 
                                    List(
                                      untypedResult,
                                      typeGetter(tpt)))))
                            } else {
                              Apply(
                                Ident("reify": TermName),
                                List(
                                  Block(
                                    (selfParam :: byValueParams) :+ splicer.transform(rhs): _*
                                  )
                                )
                              )
                            }
                            //println(s"RESULT = ${nodeToString(result)}")
                            //println(s"RESULT = $result")
                            result
                          }
                        }
                      )
                    )
                  )
                )
              )
            case _ =>
              List(super.transform(tree))
          }
        }
        def transformRuntimeExtension(tree: DefDef): Tree = {
          val DefDef(Modifiers(flags, privateWithin, annotations), name, tparams, vparamss, tpt, rhs) = tree
          val extendAnnotationOpt = annotations.find(ExtendAnnotation.unapply(_) != None)
          extendAnnotationOpt match
          {
            case Some(extendAnnotation @ ExtendAnnotation(targetTpt)) =>
              unit.warning(tree.pos, "This extension will create a runtime dependency. To use macro extensions, move this up to a publicly accessible module / object")
              
              if (tpt.isEmpty)
                unit.error(tree.pos, "Macro extensions require explicit return type annotation")

              val extensionName = newExtensionName(name)
              val typeNamesInTarget = getTypeNames(targetTpt).toSet
              val (outerTParams, innerTParams) =
                tparams.partition({ case tparam @ TypeDef(_, tname, _, _) => typeNamesInTarget.contains(tname) })
              banVariableNames(
                (selfName.toString :: "reify" :: vparamss.flatten.map(_.name.toString)).toSet, 
                rhs
              )
              
              ClassDef(
                Modifiers((flags | IMPLICIT) -- MACRO, privateWithin, Nil),
                extensionName: TypeName,
                outerTParams,
                Template(
                  List(parentTypeTreeForImplicitWrapper(targetTpt.toString: TypeName)),
                  newSelfValDef(),
                  genParamAccessorsAndConstructor(
                    List(selfName -> targetTpt)
                  ) :+
                  // Copying the original def over, without its annotation.
                  DefDef(Modifiers(flags -- MACRO, privateWithin, Nil), name, innerTParams, vparamss, tpt, rhs)
                )
              )
            case _ =>
              super.transform(tree)
          }
        }
        override def transform(tree: Tree): Tree = tree match {
          // TODO check that this module is statically and publicly accessible.
          case ModuleDef(mods, name, Template(parents, self, body)) if macroExtensions =>
            val newBody = body.flatMap {
              case dd @ DefDef(_, _, _, _, _, _) =>
                // Duplicate the resulting tree to avoid sharing any type tree between class and module..
                // TODO be finer than that and only duplicate type trees (or names?? not as straightforward as I thought).
                transformMacroExtension(dd).map(t => transform(t).duplicate)
              case member: Tree =>
                List(transform(member))
            }
            ModuleDef(mods, name, Template(parents, self, newBody))
          case dd @ DefDef(_, _, _, _, _, _) if runtimeExtensions =>
            transformRuntimeExtension(dd)
          case _ =>
            super.transform(tree)
        }
      }
      unit.body = onTransformer.transform(unit.body)
    }
  }
}
