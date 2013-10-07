package scalaxy.js
import ast._

import scala.reflect.api.Universe

trait ScalaToJavaScriptConversion extends ApiMappings with ASTConverter {

  val global: Universe
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

  def convert(trees: Tree*): String = {
    val conv: List[Tree] =
      trees.toList.flatMap(collectGlobals _)
      .map(replaceScalaApisByCallsToExterns _)

    implicit val globalPrefix = GlobalPrefix()
    implicit val guardedPrefixes = GuardedPrefixes()
    implicit val pos = SourcePos(null, -1, -1)
    var jsTrees: List[JS.Node] = conv.flatMap(convert(_))
    jsTrees = guardedPrefixes.generateGuards ++ jsTrees

    val fragments = jsTrees.map(JS.prettyPrint(_)).filter(_ != JS.NoNode).map(_ ++ a";\n")

    val result: PosAnnotatedString =
      if (fragments.isEmpty) PosAnnotatedString()
      else fragments.reduce(_ ++ _)

    // TODO create source map from result.map
    result.value
  }
}
