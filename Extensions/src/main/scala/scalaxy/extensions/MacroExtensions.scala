// Author: Olivier Chafik (http://ochafik.com)
// Feel free to modify and reuse this for any purpose ("public domain / I don't care").
package scalaxy.extensions

import scala.reflect.internal._
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.transform.TypingTransformers

/**
 *  This compiler plugin demonstrates how to do "useful" stuff before the typer phase.
 *  
 *  It defines a toy syntax that uses annotations to define implicit classes:
 *  
 *    @extend(Int) def str(base: Int = 10): String = macro reify(self.splice.toString)
 *    
 *  Which gets desugared to:
 *  
 *    import scala.language.experimental.macros
 *    implicit class str(self: Int) {
 *      def str = macro str$.str
 *    }
 *    object str$ {
 *      def str(c: scala.reflect.macros.Context): c.Expr[String] = {
 *        import c.universe._
 *        val Apply(_, List(selfTree)) = c.prefix.tree
 *        val self = c.Expr[String](selfTree)
 *        reify(self.splice.toString)
 *      }
 *    }
 *
 *  This example code doesn't try to be hygienic: it assumes @extend is not locally redefined to something else.
 *
 *  To see the AST before and after the rewrite, run the compiler with -Xprint:parser -Xprint:runtime-extensions.
 */
object MacroExtensionsCompiler {
  private val scalaLibraryJar =
    Option(classOf[List[_]].getProtectionDomain.getCodeSource).map(_.getLocation.getFile)

  def main(args: Array[String]) {
    try {
      val settings = new Settings
      val command = 
        new CompilerCommand(scalaLibraryJar.map(jar => List("-bootclasspath", jar)).getOrElse(Nil) ++ args, settings)

      if (!command.ok)
        System.exit(1)

      val global = new Global(settings, new ConsoleReporter(settings)) {
        override protected def computeInternalPhases() {
          super.computeInternalPhases
          phasesSet += new MacroExtensionsComponent(this)
        }
      }
      new global.Run().compile(command.files)
    } catch { 
      case ex: Throwable =>
        ex.printStackTrace
        System.exit(2)
    }
  }
}

/**
 *  To use this, just write the following in `src/main/resources/scalac-plugin.xml`:
 *  <plugin>
 *    <name>scalaxy-extensions</name>
 *    <classname>scalaxy.extensions.MacroExtensionsPlugin</classname>
 *  </plugin>
 */
class MacroExtensionsPlugin(override val global: Global) extends Plugin {
  override val name = "scalaxy-extensions"
  override val description = "Compiler plugin that adds a `@extend(Int) def toStr = self.toString` syntax to create extension methods."
  override val components: List[PluginComponent] =
    List(new MacroExtensionsComponent(global))
}

/**
 *  To understand / reproduce this, you should use paulp's :power mode in the scala console:
 *  
 *  scala
 *  > :power
 *  > :phase parser // will show us ASTs just after parsing
 *  > val Some(List(ast)) = intp.parse("@extend(Int) def str = self.toString")
 *  > nodeToString(ast)
 *  > val DefDef(mods, name, tparams, vparamss, tpt, rhs) = ast // play with extractors to explore the tree and its properties.
 */ 
class MacroExtensionsComponent(val global: Global)
    extends PluginComponent
    with TypingTransformers
    with Extensions
{
  import global._
  import definitions._

  override val phaseName = "scalaxy-extensions"

  override val runsRightAfter = Some("parser")
  override val runsAfter = runsRightAfter.toList
  override val runsBefore = List[String]("namer")

  private final val selfName = "self"
  
  def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    def apply(unit: CompilationUnit) {
      val onTransformer = new Transformer 
      {
        def newExtensionName(name: Name) =
          unit.fresh.newName("scalaxy$extensions$" + name + "$")
          
        def transformMacroExtension(tree: DefDef): List[Tree] = 
        {
          val DefDef(Modifiers(flags, privateWithin, annotations), name, tparams, vparamss, tpt, rhs) = tree
          annotations match 
          {
            case List(Apply(Select(New(Ident(annotationName)), initName), List(Ident(typeName)))) 
                if annotationName.toString == "extend" && 
                   initName == nme.CONSTRUCTOR =>
              val extensionName = newExtensionName(name)
              val targetTpt = Ident(typeName.toString: TypeName)
              val selfTreeName: TermName = unit.fresh.newName("selfTree")
              val contextName: TermName = unit.fresh.newName("c")
              assert(tparams == Nil)
              
              //val vparams2 = vparamss.flatten :+ ValDef(NoMods, "self", targetTpt, EmptyTree)
              List(
                newImportMacros(tree.pos),
                ClassDef(
                  Modifiers((flags | Flag.IMPLICIT) -- Flag.MACRO, privateWithin, Nil),
                  extensionName: TypeName,
                  tparams,
                  Template(
                    List(parentTypeTreeForImplicitWrapper(typeName)),
                    newSelfValDef(),
                    genParamAccessorsAndConstructor(
                      List(selfName -> targetTpt)
                    ) :+
                    // Copying the original def over, without its annotation.
                    DefDef(
                      Modifiers(flags | Flag.MACRO, privateWithin, Nil),
                      name, 
                      tparams, 
                      vparamss, 
                      tpt, 
                      termPath(extensionName + "." + name)
                    )
                  )
                ), 
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
                          ValDef(Modifiers(Flag.PARAM), contextName, typePath("scala.reflect.macros.Context"), EmptyTree)
                        )
                      ) ++
                      (
                        if (vparamss.flatten.isEmpty) 
                          Nil
                        else
                          List(
                            vparamss.flatten.map { 
                              case ValDef(pmods, pname, ptpt, prhs) =>
                                ValDef(
                                  pmods | Flag.PARAM,
                                  pname,
                                  AppliedTypeTree(
                                    typePath(contextName + ".Expr"),
                                    List(ptpt)),
                                  prhs)
                            }
                          )
                      ),
                      AppliedTypeTree(
                        typePath(contextName + ".Expr"),
                        List(tpt)
                      ),
                      Block(
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
                          selfName,
                          newEmptyTpt(),
                          Apply(
                            TypeApply(
                              termPath(contextName + ".Expr"),
                              List(tpt)),
                            List(
                              Ident(selfTreeName: TermName)))),
                        rhs // TODO
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
          annotations match {
            case List(Apply(Select(New(Ident(annotationName)), initName), List(Ident(typeName)))) 
                if annotationName.toString == "extend" && 
                   initName == nme.CONSTRUCTOR 
            =>
              unit.warning(tree.pos, "This extension will create a runtime dependency. To use macro extensions, move this up to a publicly accessible module / object")
              val extensionName = newExtensionName(name)
              val targetTpt = Ident(typeName.toString: TypeName)
              ClassDef(
                Modifiers((flags | Flag.IMPLICIT) -- Flag.MACRO, privateWithin, Nil),
                extensionName: TypeName,
                tparams,
                Template(
                  List(parentTypeTreeForImplicitWrapper(typeName)),
                  newSelfValDef(),
                  genParamAccessorsAndConstructor(
                    List(selfName -> targetTpt)
                  ) :+
                  // Copying the original def over, without its annotation.
                  DefDef(Modifiers(flags -- Flag.MACRO, privateWithin, Nil), name, tparams, vparamss, tpt, rhs)
                )
              )
            case _ =>
              super.transform(tree)
          }
        }
        override def transform(tree: Tree): Tree = tree match {
          case ModuleDef(mods, name, Template(parents, self, body)) =>
            val newBody = body.flatMap {
              case dd @ DefDef(_, _, _, _, _, _) =>
                transformMacroExtension(dd).map(transform(_))
              case member: Tree =>
                List(transform(member))
            }
            ModuleDef(mods, name, Template(parents, self, newBody))
          case dd @ DefDef(_, _, _, _, _, _) =>
            transformRuntimeExtension(dd)
          case _ =>
            super.transform(tree)
        }
      }
      unit.body = onTransformer.transform(unit.body)
    }
  }
}
