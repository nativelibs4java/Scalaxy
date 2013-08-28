package scalaxy.js
import ast._

import scala.reflect.api.Universe

class ScalaToJavaScriptConverter(val global: Universe) extends ApiMappings with ASTConverter {

  import global._

  private def collectGlobals(tree: Tree): List[Tree] = tree match {
    case ModuleDef(mods, name, Template(parents, self, body))
        if hasGlobalAnnotation(tree.symbol) =>
      body
    case PackageDef(pid, stats) =>
      List(PackageDef(pid, stats.flatMap(stat => collectGlobals(stat))))
    case input =>
      List(input)
  }

  def convert[T <: Universe#Tree](trees: T*): String = {
    val conv: List[Tree] =
      trees.map(_.asInstanceOf[Tree]).toList
      .flatMap(collectGlobals _)
      .map(replaceScalaApisByCallsToExterns _)

    val fragments: List[PosAnnotatedString] =
      conv.flatMap(convert(_)).map(JS.prettyPrint(_)).map(_ ++ a";\n")

    val result: PosAnnotatedString =
      if (fragments.isEmpty) PosAnnotatedString()
      else fragments.reduce(_ ++ _)

    // TODO create source map from result.map
    result.value
  }
}
