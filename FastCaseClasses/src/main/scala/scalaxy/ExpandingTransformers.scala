package scalaxy.fastcaseclasses

import scala.reflect.api.Universe

trait ExpandingTransformers {
  val global: Universe
  import global._

  private[this] def getClassNames(trees: List[Tree]) =
    trees.collect {
      case ClassDef(_, name, _, _) => name
    }

  private[this] def getObjectNames(trees: List[Tree]) =
    trees.collect {
      case ModuleDef(_, name, _) => name
    }

  abstract class ExpandingTransformer extends Transformer {

    def expandOne(tree: Tree, hasCompanion: Boolean): List[Tree] =
      List(transform(tree))

    def expandMany(trees: List[Tree]): List[Tree] = {
      // val classNames = getClassNames(trees).toSet
      // val objectNames = getObjectNames(trees).toSet
      var classNames = Set[String]()
      var objectNames = Set[String]()
      var nameMap = Map[Tree, String]()

      for (tree <- trees; otree = Option(tree)) {
        for (ClassDef(_, name, _, _) <- otree; n = name.toString) {
          classNames += n
          nameMap += (tree -> n)
        }
        for (ModuleDef(_, name, _) <- otree; n = name.toString) {
          objectNames += n
          nameMap += (tree -> n)
        }
      }
      val intersection = classNames intersect objectNames

      trees.flatMap(tree =>
        expandOne(tree,
          hasCompanion = nameMap.get(tree).exists(intersection)))
    }

    def expandValue(tree: Tree): Tree = {
      expandOne(tree, hasCompanion = false) match {
        case List(result) =>
          result
        case list =>
          Block(list.dropRight(1), list.last)
      }
    }

    def transformSame[A <: Tree](tree: A): A = transform(tree).asInstanceOf[A]

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

      case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        treeCopy.DefDef(tree,
          mods,
          name,
          tparams.map(transformSame),
          vparamss.map(_.map(transformSame)),
          transform(tpt),
          expandValue(rhs))

      case ValDef(mods, name, tpt, rhs) =>
        treeCopy.ValDef(tree,
          mods,
          name,
          transformSame(tpt),
          expandValue(rhs))

      case _ =>
        super.transform(tree)
    }
  }
}
