package scalaxy.streams
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

import scalaxy.streams.SideEffectsWhitelists._

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
    val result =
      sym.isPackage ||
      whitelistedSymbols(sym.fullName) ||
      whitelistedClasses(sym.enclosingClass.fullName) ||
      whitelistedPackages(sym.enclosingPackage.fullName)

    result
  }

  def isTrulyImmutableClass(tpe: Type): Boolean = tpe != null && {
    val sym = tpe.typeSymbol
    val result =
      sym != null && sym != NoSymbol &&
      trulyImmutableClasses(sym.fullName)

    // println(s"isTrulyImmutableClass($sym) = $result")
    result
  }

  object SelectOrApply {
    def unapply(tree: Tree): Option[(Tree, Name, List[Tree], List[List[Tree]])] =
      Option(tree) collect {
        case Ident(name) =>
          (EmptyTree, name, Nil, Nil)

        case Select(target, name) =>
          (target, name, Nil, Nil)

        case TypeApply(SelectOrApply(target, name, Nil, Nil), targs) =>
        // case TypeApply(Select(target, name), targs) =>
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

    // println("TRAVERSING " + tree)

    new Traverser {
      override def traverse(tree: Tree) {
        tree match {
          case SomeStream(stream) if !stream.ops.isEmpty || stream.hasExplicitSink =>
            // println("FOUND stream " + stream)
            for (sub <- stream.subTrees; if tree != sub) {
              assert(tree != sub, s"stream = $stream, sub = $sub")
              // println("\tFOUND sub " + sub)

              traverse(sub)
            }

          case SelectOrApply(qualifier, N(name @ ("hashCode" | "toString")), Nil, Nil | List(Nil))
              if !isTrulyImmutableClass(qualifier.tpe) =>
            keepWorstSideEffect {
              addEffect(SideEffect(tree, anyMethodMessage(name), SideEffectSeverity.ProbablySafe))
              traverse(qualifier)
            }

          case SelectOrApply(qualifier, N("equals" | "$eq$eq"), Nil, List(List(other)))
              if !isTrulyImmutableClass(qualifier.tpe) =>
            keepWorstSideEffect {
              addEffect(SideEffect(tree, anyMethodMessage("equals"), SideEffectSeverity.ProbablySafe))
              traverse(qualifier)
              traverse(other)
            }

          case SelectOrApply(qualifier, name, _, argss) =>
            keepWorstSideEffect {
              val sym = tree.symbol

              def qualifierIsImmutable =
                qualifier != EmptyTree &&
                qualifier.tpe != null &&
                qualifier.tpe != NoType &&
                isTrulyImmutableClass(qualifier.tpe)

              val safeSymbol =
                localSymbols.contains(sym) ||
                isSideEffectFree(sym) ||
                qualifierIsImmutable

              // if (safeSymbol) {
              //   println(s"""
              //     SAFE SYMBOL(${sym.fullName})
              //       qualifier: $qualifier
              //       qualifier.tpe: ${qualifier.tpe}
              //       qualifier.tpe.typeSymbol: ${qualifier.tpe.typeSymbol}
              //       name: $name
              //       localSymbols.contains(sym): ${localSymbols.contains(sym)}
              //       isSideEffectFree(sym): ${isSideEffectFree(sym)}
              //       isSideEffectFree(qualifier.tpe.typeSymbol): ${isSideEffectFree(qualifier.tpe.typeSymbol)}
              //   """)
              // }
              if (!safeSymbol) {
                (name, argss) match {
                  case (ProbablySafeUnaryNames(msg), (List(_) :: _)) =>
                    addEffect(SideEffect(tree, msg + " (symbol: " + sym.fullName + ")", SideEffectSeverity.ProbablySafe))

                  case _ =>
                    addEffect(
                      SideEffect(
                        tree,
                        s"Reference to " + sym.fullName,// (local symbols: $localSymbols",
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

          case Assign(_, _) | Function(_, _) |
              TypeTree() | EmptyTree |
              Literal(_) | Block(_, _) |
              Match(_, _) | Typed(_, _) | This(_) |
              (_: DefTree) =>
            super.traverse(tree)

          case CaseDef(_, guard, body) =>
            traverse(guard)
            traverse(body)

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
