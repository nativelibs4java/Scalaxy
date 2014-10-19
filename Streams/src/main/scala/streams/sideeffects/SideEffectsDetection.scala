package scalaxy.streams
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

private[streams] trait SideEffectsDetection
    extends Streams
    with SideEffectsMessages
{
  val global: scala.reflect.api.Universe
  import global._

  implicit class RichSymbol(sym: Symbol) {
    def enclosingPackage: Symbol = {
      var sym = this.sym
      while (sym != NoSymbol && !sym.isPackageClass && !sym.isPackage)
        sym = sym.owner
      sym
    }
    def enclosingClass: Symbol = {
      def encl(sym: Symbol): Symbol =
        if (sym.isClass || sym == NoSymbol) sym else encl(sym.owner)
      encl(this.sym)
    }
  }

  private[this] def isSideEffectFree(sym: Symbol): Boolean = {
    import SideEffectsWhitelists._

    val result =
      sym.isPackage ||
      whitelistedSymbols(sym.fullName) ||
      whitelistedClasses(sym.enclosingClass.fullName) ||
      whitelistedPackages(sym.enclosingPackage.fullName)

    result
  }

  object SelectOrApply {
    def unapply(tree: Tree): Option[(Tree, Name, List[Tree], List[List[Tree]])] =
      Option(tree) collect {
        case Ident(name) =>
          (EmptyTree, name, Nil, Nil)

        case Select(target, name) =>
          (target, name, Nil, Nil)

        case TypeApply(Select(target, name), targs) =>
          (target, name, targs, Nil)

        case Apply(SelectOrApply(target, name, targs, argss), newArgs) =>
          (target, name, targs, argss :+ newArgs)
      }
  }

  def analyzeSideEffects(tree: Tree): List[SideEffect] = {
    val effects = ArrayBuffer[SideEffect]()
    def addEffect(e: SideEffect) {
      effects += e
    }
    var localSymbols = Set[Symbol]()
    new Traverser {
      override def traverse(tree: Tree) {
        tree match {
          case (_: DefTree) | ValDef(_, _, _, _) if Option(tree.symbol).exists(_ != NoSymbol) =>
            localSymbols += tree.symbol
          case _ =>
        }
        super.traverse(tree)
      }
    } traverse tree

    def keepWorstSideEffect(block: => Unit) {
      val size = effects.size
      block
      if (effects.size > size) {
        val slice = effects.slice(size, effects.size)
        val worstSeverity = slice.map(_.severity).max
        var worstEffect = effects.find(_.severity == worstSeverity).get
        effects.remove(size, effects.size - size)
        effects += worstEffect

        // println(s"""
        //   Side-Effect:
        //     tree: ${worstEffect.tree}
        //     tree.symbol = ${worstEffect.tree.symbol} (${Option(worstEffect.tree.symbol).map(_.fullName)}}
        //     description: ${worstEffect.description}
        //     severity: ${worstEffect.severity}
        // """)
      }
    }

    new Traverser {
      override def traverse(tree: Tree) {
        tree match {
          case SomeStream(stream) if !stream.ops.isEmpty || stream.hasExplicitSink =>
            for (sub <- stream.subTrees; if tree != sub) {
              assert(tree != sub, s"stream = $stream, sub = $sub")
              traverse(sub)
            }

          case Assign(_, _) | Function(_, _) |
              TypeTree() | EmptyTree |
              Literal(_) | Block(_, _) |
              Match(_, _) | Typed(_, _) | This(_) |
              (_: DefTree) =>
            super.traverse(tree)

          case CaseDef(_, guard, body) =>
            traverse(guard)
            traverse(body)

          case SelectOrApply(qualifier, name, _, argss) =>
            keepWorstSideEffect {
              val sym = tree.symbol
              val safeSymbol =
                localSymbols.contains(sym) ||
                isSideEffectFree(sym)
              if (!safeSymbol) {
                (name, argss) match {
                  case (ProbablySafeNullaryNames(msg), (Nil | List(Nil))) =>
                    addEffect(SideEffect(tree, msg, SideEffectSeverity.ProbablySafe))

                  case (ProbablySafeUnaryNames(msg), (List(_) :: _)) =>
                    addEffect(SideEffect(tree, msg, SideEffectSeverity.ProbablySafe))

                  case _ =>
                    addEffect(
                      SideEffect(
                        tree,
                        s"Unknown reference / call (local symbols: $localSymbols",
                        SideEffectSeverity.Unsafe))
                }
              }

              traverse(qualifier)
              // if (!(safeSymbol && sym.isTerm && sym.asTerm.isStable)) {
                // println(s"""
                //   sym: $sym
                //   sym.isStable: ${sym.asTerm.isStable}
                // """)
              // }
              argss.foreach(_.foreach(traverse(_)))
            }

          case _ =>
            val msg = s"TODO: proper message for ${tree.getClass.getName}: $tree"
            // new RuntimeException(msg).printStackTrace()
            addEffect(SideEffect(tree, msg, SideEffectSeverity.Unsafe))
            super.traverse(tree)
        }
      }
    } traverse tree

    effects.toList
  }
}
