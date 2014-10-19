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
    "++" -> (s"Collection composition is $assumedSideEffectFreeMessageSuffix"),
    "--" -> (s"Collection composition is $assumedSideEffectFreeMessageSuffix")
  ))
}

private[streams] trait SideEffects extends SideEffectsMessages
{
  val global: scala.reflect.api.Universe
  import global._

  case class SideEffect(tree: Tree, description: String, severity: SideEffectSeverity)

  private[this] def hasSymbol(sym: Symbol): Boolean = sym != null && sym != NoSymbol

  private[this] lazy val immutablePackage = rootMirror.staticPackage("scala.collection.immutable")

  lazy val PredefModule = rootMirror.staticModule("scala.Predef")
  lazy val PredefTpe = PredefModule.moduleClass.asType.toType

  private[this] lazy val predefImmutableSymbols = Set[Symbol](
    rootMirror.staticClass("java.lang.String"),
    rootMirror.staticClass("scala.collection.SetLike"),
    rootMirror.staticClass("scala.collection.SeqLike"),
    rootMirror.staticClass("scala.collection.generic.GenericCompanion"),
//    (PredefTpe member TermName("Set")),
//    (PredefTpe member TermName("Map")),
    definitions.LongTpe.typeSymbol,
    definitions.IntTpe.typeSymbol,
    definitions.ShortTpe.typeSymbol,
    definitions.ByteTpe.typeSymbol,
    definitions.CharTpe.typeSymbol,
    definitions.FloatTpe.typeSymbol,
    definitions.DoubleTpe.typeSymbol,
    definitions.UnitTpe.typeSymbol,
    definitions.BooleanTpe.typeSymbol
  )
  private[this] def isImmutableSymbol(sym: Symbol): Boolean = {
    val isImmutable = predefImmutableSymbols(sym) ||
      // TODO: fix this symbol equality issue, as usual...
      sym.owner.fullName == immutablePackage.fullName// ||
      // sym.owner == PredefModule && (
      //   sym.typeSignature <:< typeOf[Set[_]] ||
      //   sym.typeSignature <:< typeOf[List[_]]
      // )

    // if (!isImmutable) {
    //   println(s"isImmutableSymbol($sym = ${sym.fullName}) = $isImmutable (owner = ${sym.owner.fullName})")
    // }

    isImmutable
  }
  private[this] def isSideEffectFree(sym: Symbol): Boolean = {
    // TODO support @nosideeffects annotation.
    val result = {
      if (sym.isPackage) {
        // Note: class-loader and other similar runtime side-effects are negliged.
        true
      } else if (sym.isMethod) {
        isImmutableSymbol(sym.owner)
      } else if (isImmutableSymbol(sym)) {
        true
      // } else if (sym.isModule || sym.isClass) {
      //   isImmutableSymbol(sym)
      } else if (sym.isTerm) {
        // TODO: what is considered stable?
        sym.asTerm.isStable
      } else {
        // println(s"TODO Symbol $sym (${sym.getClass}")
        false
      }
    }
    // if (!result) {
    //   println(s"isSideEffectFree($sym = ${sym.fullName}) = $result (owner = ${sym.owner.fullName}, owner.isImmutable = ${isImmutableSymbol(sym.owner)}")
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
    var localSymbols = Set[Symbol]()

    def keepWorstSideEffect(block: => Unit) {
      val size = effects.size
      block
      if (effects.size > size) {
        val slice = effects.slice(size, effects.size)
        val worstSeverity = slice.map(_.severity).max
        var worstEffect = effects.find(_.severity == worstSeverity).get
        effects.remove(size, effects.size - size)
        effects += worstEffect
      }
    }

    new Traverser {

      override def traverse(tree: Tree) {
        def treeHasSafeSymbol: Boolean =
          localSymbols.contains(tree.symbol) ||
          isSideEffectFree(tree.symbol)

        tree match {
          case TypeTree() | EmptyTree | Function(_, _) | Literal(_) | Block(_, _) =>
            super.traverse(tree)

          case Assign(_, _) =>
            super.traverse(tree)

          case SelectOrApply(qualifier, ProbablySafeNullaryNames(msg), _, List(Nil)) =>
            if (!treeHasSafeSymbol) {
              keepWorstSideEffect {
                effects += SideEffect(tree, msg, SideEffectSeverity.ProbablySafe)
                traverse(qualifier)
              }
            }

          case SelectOrApply(qualifier, ProbablySafeUnaryNames(msg), _, argss @ (List(_) :: _)) =>
            // println(s"SelectOrApply(argss = $argss)")
            if (!treeHasSafeSymbol) {
              keepWorstSideEffect {
                effects += SideEffect(tree, msg, SideEffectSeverity.ProbablySafe)
                traverse(qualifier)
              }
            }
            argss.foreach(_.foreach(traverse(_)))

          case SelectOrApply(qualifier, name, _, argss) =>
            if (!treeHasSafeSymbol) {
              keepWorstSideEffect {
                effects += SideEffect(tree, "Unknown reference / call",
                  SideEffectSeverity.Unsafe)
                traverse(qualifier)
              }
            }
            argss.foreach(_.foreach(traverse(_)))

          case (_: DefTree) | ValDef(_, _, _, _) if hasSymbol(tree.symbol) =>
            localSymbols += tree.symbol
            super.traverse(tree)

          case _ =>
            val msg = s"TODO: proper message for ${tree.getClass.getName}: $tree"
            // new RuntimeException(msg).printStackTrace()
            effects += SideEffect(tree, msg, SideEffectSeverity.Unsafe)
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
