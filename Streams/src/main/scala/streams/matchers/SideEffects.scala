package scalaxy.streams
import scala.reflect.NameTransformer
import scala.collection.breakOut
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

sealed class SideEffectSeverity(private val level: Int)
    extends Comparable[SideEffectSeverity] {
  override def compareTo(s: SideEffectSeverity) = level.compareTo(s.level)
}

object SideEffectSeverity {
  // case object Safe extends SideEffectSeverity
  /** For instance, toString, equals, hashCode and + are usually considererd probably safe. */
  case object ProbablySafe extends SideEffectSeverity(1)
  // case object ProbablyUnsafe extends SideEffectSeverity(2)
  case object Unsafe extends SideEffectSeverity(3)
}

private[streams] trait SideEffectsMessages
{
  val global: scala.reflect.api.Universe
  import global._

  implicit class ExtractibleMap[A, B](m: Map[A, B]) {
    def unapply(key: A): Option[B] = {
      val msg = m.get(key)
      // println(s"msg for $key: $msg (known keys: ${m.keys})")
      msg
    }
  }

  def termNamesMessages(m: Map[String, String]): ExtractibleMap[TermName, String] =
    m.map({ case (k, v) => TermName(NameTransformer.encode(k)) -> v })

  private[this] val assumedSideEffectFreeMessageSuffix = "generally assumed to be side-effect free"

  private[this] def anyMethodMessage(name: String) =
    s"Any.$name is $assumedSideEffectFreeMessageSuffix"

  lazy val ProbablySafeNullaryNames = termNamesMessages(Map(
    "hashCode" -> anyMethodMessage("hashCode"),
    "toString" -> anyMethodMessage("toString")
  ))
  private[this] val aritMessage = s"Arithmetic / ensemblist operators are $assumedSideEffectFreeMessageSuffix"

  lazy val ProbablySafeUnaryNames = termNamesMessages(Map(
    "+" -> aritMessage,
    "-" -> aritMessage,
    "/" -> aritMessage,
    "*" -> aritMessage,
    "equals" -> anyMethodMessage("equals"),
    "++" -> s"Collection composition is $assumedSideEffectFreeMessageSuffix",
    "--" -> s"Collection composition is $assumedSideEffectFreeMessageSuffix",
    "canBuildFrom" -> s"CanBuildFrom's are $assumedSideEffectFreeMessageSuffix",
    "zipWithIndex" -> s"zipWithIndex is $assumedSideEffectFreeMessageSuffix"
  ))
}

private[streams] trait SideEffects extends SideEffectsMessages
{
  val global: scala.reflect.api.Universe
  import global._

  case class SideEffect(tree: Tree, description: String, severity: SideEffectSeverity)

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

    // if (!result) {
    //   println(s"""
    //     isSideEffectFree($sym = ${sym.fullName}) = $result
    //       owner = ${sym.owner.fullName}
    //       enclosingClass = ${sym.enclosingClass.fullName}
    //       enclosingPackage = ${sym.enclosingPackage.fullName}
    //   """)
    // }

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
  // private[this] val safeOwners = Set(

  // case class NameMessageExtractor(messageMap: Map[String, String]) {}
  // private[this] def isProbablySafeName 

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
          case Assign(_, _) |
              TypeTree() | EmptyTree | Function(_, _) | Literal(_) | Block(_, _) |
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

          // case (_: DefTree) | ValDef(_, _, _, _) if Option(tree.symbol).exists(_ != NoSymbol) =>
          //   localSymbols += tree.symbol
          //   super.traverse(tree)

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
          // case ValDef(mods, _, _, rhs) => if rhs != EmptyTree =>
          //   localSymbols += tree.symbol
          //   if (mods.hasFlag(Flag.MUTABLE)) {
          //     val externalSymbols = findExternalSymbols(rhs)
          //     if (!externalSymbols.isEmpty) {
          //       effects += SideEffect(
          //         tree,
          //         s"Mutating a symbol declared outside the lambda",
          //         SideEffectSeverity.ProbablyUnsafe)

          //     }
          //   }
          //   super.traverse(tree)

          // case Assign(lhs, rhs) if !localSymbols.contains(lhs.symbol) =>
          //   effects += SideEffect(
          //     tree,
          //     s"Mutating a symbol declared outside the lambda",
          //     SideEffectSeverity.Unsafe)
          //   // No need to traverse down.

          // case RefTree(_, _) | Apply(_, _) if treeHasSafeSymbol =>
          //   super.traverse(tree)

          // case Ident(_) | Select(_, _) | Apply(_, _) =>
          //   if (treeHasSafeSymbol) {
          //     super.traverse(tree)
          //   } else {
          //     effects += SideEffect(tree, "Unknown reference / call", SideEffectSeverity.ProbablyUnsafe)
          //     // No need to traverse down.
          //   }
