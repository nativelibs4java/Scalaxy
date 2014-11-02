package scalaxy.fastcaseclasses

// import scala.reflect.api.Universe

import scala.tools.nsc.transform.TypingTransformers
import scala.tools.nsc.CompilationUnits

trait CompanionTransformers
    extends TypingTransformers {
  // val global: Universe
  import global._

  private[this] def getClassNames(trees: List[Tree]) =
    trees.collect {
      case ClassDef(_, name, _, _) => name
    }

  private[this] def getObjectNames(trees: List[Tree]) =
    trees.collect {
      case ModuleDef(_, name, _) => name
    }

  case class Companions(classDef: ClassDef, moduleDef: ModuleDef) {
    def toList = List(classDef, moduleDef)
  }

  //case class Companions(classDef: ClassDef, moduleDef: ModuleDef)
  type CompanionsList = List[Either[Companions, Tree]]

  private[this] def gatherCompanions(trees: List[Tree]): CompanionsList = {
    // val classNames = getClassNames(trees).toSet
    // val objectNames = getObjectNames(trees).toSet
    val map = collection.mutable.LinkedHashMap[Tree, Either[Companions, Tree]]()
    val byName = collection.mutable.LinkedHashMap[String, Tree]()

    for (tree <- trees) {
      val name = Option(tree) collect {
        case ClassDef(mods, name, _, _) if mods.hasFlag(Flag.CASE) => name.toString
        case ModuleDef(mods, name, _) => name.toString
      }
      // println("name = " + name)
      name.flatMap(byName.get(_)) match {
        case Some(comp) =>
          val (cls, mod) = (comp, tree) match {
            case (cls: ClassDef, mod: ModuleDef) => (cls, mod)
            case (mod: ModuleDef, cls: ClassDef) => (cls, mod)
          }
          map(comp) = Left(Companions(cls, mod))

        case None =>
          map(tree) = Right(tree)
      }
      name.foreach(n => byName(n) = tree)
    }

    val companions = map.values.toList
    // println("companions = " + companions.collect({ case Left(v) => v }))
    companions
  }

  abstract class CompanionsTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {

    def expandTreeOrCompanions(toc: Either[Companions, Tree]): Either[Companions, Tree] =
      toc match {
        case Left(Companions(cls, mod)) =>
          Left(Companions(transformAndCast(cls), transformAndCast(mod)))

        case Right(tree) =>
          Right(transform(tree))
      }

    def expandMany(trees: List[Tree]): List[Tree] =
      // trees
      gatherCompanions(trees).map(expandTreeOrCompanions).flatMap {
        case Right(result) => List(result)
        case Left(comps) => comps.toList
      }

    def expandValue(tree: Tree): Tree = {
      expandTreeOrCompanions(Right(tree)) match {
        case Right(result) =>
          result
        case Left(comps) =>
          Block(comps.toList, EmptyTree)
      }
    }

    def transformAndCast[A <: Tree](tree: A): A = transform(tree).asInstanceOf[A]

    override def transform(tree: Tree): Tree = tree match {
      case Block(stats, value) =>
        treeCopy.Block(tree,
          expandMany(stats),
          transform(value))

      case Template(parents, self, body) =>
        treeCopy.Template(tree,
          expandMany(parents),
          self,
          expandMany(body))

      case PackageDef(pid, stats) =>
        treeCopy.PackageDef(tree,
          transformAndCast(pid),
          expandMany(stats))

      case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        treeCopy.DefDef(tree,
          mods,
          name,
          tparams.map(transformAndCast),
          vparamss.map(_.map(transformAndCast)),
          transform(tpt),
          expandValue(rhs))

      case ValDef(mods, name, tpt, rhs) =>
        treeCopy.ValDef(tree,
          mods,
          name,
          transformAndCast(tpt),
          expandValue(rhs))

      case _ =>
        super.transform(tree)
    }
  }
}
