package scalaxy.streams

import scala.reflect.macros.blackbox.Context

object HacksAndWorkarounds
{
  def call(obj: Any, method: String, args: Any*): Any = {
    val cls = obj.getClass
    val ms = cls.getMethods
    val List(m) = ms.filter(_.getName == method).toList
    m.invoke(obj, args.map(_.asInstanceOf[Object]).toArray:_*)
  }

  // TODO(ochafik): Remove this!
  def cast[A](a: Any): A = a.asInstanceOf[A]

  def safelyUnSymbolize(c: Context)(tree: c.universe.Tree): c.universe.Tree = {
    import c.universe._

    // val dup = tree.duplicate
    // new Traverser {
    //   override def traverse(tree: Tree) = {
    //     tree match {
    //       case CaseDef(_, _, _) | Block(_, _) | TypeTree() =>

    //       case tree =>
    //         try {
    //           if (tree.isInstanceOf[RefTree])
    //             call(tree, "setSymbol", NoSymbol)
    //         } catch { case ex: Throwable =>
    //           println(s"setSymbol failed on ${tree.getClass}: $tree")
    //           throw ex
    //         }
    //     }
    //     super.traverse(tree)
    //   }
    // } traverse(dup)

    //dup
    // c.typecheck(c.untypecheck(tree))
    tree

    // c.internal.typingTransform(tree) {
    //   case (tree @ (CaseDef(_, _, _) | Block(_, _) | TypeTree()), api) =>
    //     api.default(tree)

    //   case (tree, api) =>
    //     if (tree.isInstanceOf[RefTree])
    //       call(tree, "setSymbol", NoSymbol)
    //     api.default(tree)
    // }

   //def removeMacroSymbols(c: Context)(tree: c.universe.Tree): c.universe.Tree = {
   // import c.universe._
   //  c.internal.typingTransform(tree) {
   //    case (tree @ Function(vparams, body), api) =>

   //      def apiRecur(tree: Tree): Tree = cast(api.recur(cast(tree)))

   //      println("UNTYPING FUN: " + tree + ": " + tree.symbol + " @ " + tree.symbol.owner)
   //      val res = Function(vparams.map(api.recur(_)).map(_.asInstanceOf[ValDef]), api.recur(body))

   //      // call(res, "setType", tree.tpe)
   //      api.typecheck(res)

   //    case (tree, api) if tree.symbol != null &&
   //        // tree.symbol.isTerm &&
   //        // tree.symbol.asTerm.isVal &&
   //        // (
   //        tree.isInstanceOf[RefTree] && tree.symbol.name.toString.matches(".*?\\$macro\\$.*")
   //        // ||
   //        // tree.isInstanceOf[Function]
   //        //)
   //        =>
   //      println("UNTYPING: " + tree + ": " + tree.symbol + " @ " + tree.symbol.owner)
   //      val tpe = tree.symbol.typeSignature.dealias
   //      //val ttree = c.untypecheck(tree)
   //      val ttree = tree
   //      call(ttree, "setSymbol", NoSymbol)//c.untypecheck(tree)
   //      println("\tRETYPING: " + tpe + ": " + tpe.typeSymbol)
   //      call(ttree, "setType", tpe)

   //      ttree

   //      //             case (tree, api) if tree.symbol != null && tree.isInstanceOf[DefTree] =>
   //      //               println(s"currentOwner = ${api.currentOwner} ; currentOwner.info.decls = ${api.currentOwner.info.decls}")
   //      //               //api.currentOwner.info.decls enter tree.symbol
   //      //               HacksAndWorkarounds.call(api.currentOwner.info.decls, "enter", tree.symbol)
   //      //               api.default(tree)
      
   //    case (tree, api) =>
   //      api.default(tree)
   // }
 }
}
