package scalaxy.streams

import scala.reflect.macros.blackbox.Context

private[scalaxy] object HacksAndWorkarounds
{
  def call(obj: Any, method: String, args: Any*): Any = {
    val cls = obj.getClass
    val ms = cls.getMethods
    val name = method.replace("=", "$eq")
    ms.filter(_.getName == name) match {
      case Array(m) =>
        m.invoke(obj, args.map(_.asInstanceOf[Object]).toArray:_*)
      case Array() =>
        sys.error(s"No method $name in $cls:\n\t${ms.map(_.getName).sorted.reduce(_ + "\n" + _)}")
    }
  }

  def replaceDeletedOwner(u: scala.reflect.api.Universe)
                         (tree: u.Tree, deletedOwner: u.Symbol, newOwner: u.Symbol) = {
    import u._
    val dup = tree // TODO: tree.duplicate (but does it keep positions??)

    new Traverser {
      override def traverse(tree: Tree) {
        for (sym <- Option(tree.symbol); if sym != NoSymbol) {
          if (sym.owner == deletedOwner) {
            call(sym, "owner_=", newOwner)
            if (tree.isInstanceOf[DefTree]) {
              val decls = newOwner.info.decls
              if (decls.toString == "[]") {
                // println(s"\nENTERING SYM IN NEW OWNER.")
                call(decls, "enter", sym)
              }
            }
          }
        }
        super.traverse(tree)
      }
    } traverse dup

    dup
  }

  // TODO(ochafik): Remove this!
  def cast[A](a: Any): A = a.asInstanceOf[A]
}
