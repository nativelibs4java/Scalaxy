package scalaxy.reified

import scalaxy.reified.internal.Utils._

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.definitions._
import scala.reflect.runtime.currentMirror
import scala.collection.immutable
import scala.reflect.{ ClassTag, Manifest, ClassManifestFactory }

/**
 * Conversions for captured references of common types.
 */
object CaptureConversions {

  type Conversion = PartialFunction[(Any, Type, Any /*full Conversion*/ ), Tree]

  /**
   * Default conversion function that handles constants, arrays, immutable collections,
   * tuples and options.
   * Other conversion functions can be composed with this default (or with a subset of its
   * components) with the standard PartialFunction methods (orElse...).
   */
  final lazy val DEFAULT: Conversion = {
    CONSTANT orElse
      ARRAY orElse
      IMMUTABLE_COLLECTION orElse
      OPTION orElse
      TUPLE
  }

  /** Converts captured constants (AnyVal, String) to their corresponding AST */
  final lazy val CONSTANT: Conversion = {
    case (value @ (
      (_: Number) |
      (_: java.lang.Boolean) |
      (_: String) |
      (_: java.lang.Character)), tpe: Type, conversion: Conversion) =>
      Literal(Constant(value))
  }

  // returns collection.apply + elementType
  private def collectionApply(
    syms: (ModuleSymbol, TermSymbol),
    col: AnyRef,
    elements: List[_],
    tpe: Type,
    tpeArity: Int,
    conversion: Conversion): (Tree, List[Type]) = {

    val (moduleSym, methodSym) = syms
    val (elementTypes, castToAnyRef) = (tpe, col) match {
      case (TypeRef(_, _, targs), _) if (tpe <:< typeOf[Iterable[_]] || tpe <:< typeOf[Product]) && targs.size >= tpeArity =>
        targs.take(tpeArity) -> false
      case (_, wa: collection.mutable.WrappedArray[_]) =>
        assert(tpeArity == 1)
        val elementManifest = wa.elemTag.asInstanceOf[Manifest[_]]
        List(manifestToTypeTag(currentMirror, elementManifest).tpe.asInstanceOf[Type]) -> false
      case _ =>
        (0 until tpeArity).map(_ => typeOf[AnyRef]).toList -> true
    }

    (
      Apply(
        TypeApply(
          Select(
            getModulePath(universe)(moduleSym), //Ident(moduleSym),
            methodSym),
          elementTypes.map(TypeTree(_))),
        elements.zipWithIndex.map({
          case (value, i) =>
            val convertedValue = conversion((value, if (elementTypes.size == 1) elementTypes(0) else elementTypes(i), conversion))
            if (castToAnyRef) {
              val convertedValueExpr = newExpr[Any](convertedValue)
              universe.reify(
                convertedValueExpr.splice.asInstanceOf[AnyRef]
              ).tree
            } else {
              convertedValue
            }
        })
      ),
        elementTypes
    )
  }

  /**
   * Convert an array an AST that represents a call to Array.apply with a 'best guess' component
   *  type, and all values converted.
   */
  final lazy val ARRAY: Conversion = {
    lazy val Array_syms = (ArrayModule, ArrayModule_overloadedApply)

    {
      case (array: Array[_], tpe: Type, conversion: Conversion) =>
        val (conv, List(elementType)) = collectionApply(Array_syms, array: Traversable[_], array.toList, tpe, 1, conversion)
        val classTagType = for (t <- typeOf[ClassTag[Int]]) yield {
          if (t == typeOf[Int]) elementType
          else t
        }
        Apply(
          conv,
          List(
            resolveModulePaths(universe)(optimisingToolbox.inferImplicitValue(classTagType))))
    }
  }

  private def symsOf(name: String, pack: String) = {
    val moduleSym = currentMirror.staticModule(pack + "." + name)
    val methodSym = moduleSym.moduleClass.typeSignature.member("apply": TermName).asTerm
    (moduleSym, methodSym)
  }

  /**
   * Convert an immutable collection to an AST that represents a call to a constructor for that
   *  collection type with a 'best guess' component type, and all values converted.
   *  Types supported are HashSet, Set, List, Vector, Stack, Queue, Seq.
   */
  final lazy val IMMUTABLE_COLLECTION: Conversion = {
    val pack = "scala.collection.immutable"
    //lazy val TreeSet_syms = symsOf("TreeSet")
    //lazy val SortedSet_syms = symsOf("SortedSet")
    lazy val HashSet_syms = symsOf("HashSet", pack)
    //lazy val BitSet_syms = symsOf("BitSet", pack)
    lazy val Set_syms = symsOf("Set", pack)
    lazy val List_syms = symsOf("List", pack)
    lazy val Vector_syms = symsOf("Vector", pack)
    lazy val Stack_syms = symsOf("Stack", pack)
    lazy val Queue_syms = symsOf("Queue", pack)
    lazy val Seq_syms = symsOf("Seq", pack)

    {
      case (col: immutable.Range, tpe: Type, conversion: Conversion) =>
        val start = newExpr[Int](Literal(Constant(col.start)))
        val end = newExpr[Int](Literal(Constant(col.end)))
        val step = newExpr[Int](Literal(Constant(col.step)))
        if (col.isInclusive)
          universe.reify(start.splice to end.splice by step.splice).tree
        else
          universe.reify(start.splice until end.splice by step.splice).tree
      // TODO inject ordering
      //case (col: immutable.TreeSet[_], tpe: Type, conversion: Conversion)
      //    if tpe != typeOf[AnyRef] && tpe != typeOf[Any] =>
      //  collectionApply(TreeSet_syms, col, tpe, 1, conversion)._1
      //case (col: immutable.SortedSet[_], tpe: Type, conversion: Conversion)
      //    if tpe != typeOf[AnyRef] && tpe != typeOf[Any] =>
      //  collectionApply(SortedSet_syms, col, tpe, 1, conversion)._1
      case (col: immutable.HashSet[_], tpe: Type, conversion: Conversion) =>
        collectionApply(HashSet_syms, col, col.toList, tpe, 1, conversion)._1
      case (col: immutable.Set[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Set_syms, col, col.toList, tpe, 1, conversion)._1
      case (col: immutable.List[_], tpe: Type, conversion: Conversion) =>
        collectionApply(List_syms, col, col.toList, tpe, 1, conversion)._1
      case (col: immutable.Vector[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Vector_syms, col, col.toList, tpe, 1, conversion)._1
      case (col: immutable.Stack[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Stack_syms, col, col.toList, tpe, 1, conversion)._1
      case (col: immutable.Queue[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Queue_syms, col, col.toList, tpe, 1, conversion)._1
      case (col: immutable.Seq[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Seq_syms, col, col.toList, tpe, 1, conversion)._1
      // TODO: Map, BitSet
      //case (map: immutable.Map[_], tpe: Type, conversion: Conversion) =>      
    }
  }

  /**
   * Convert an option collection to an AST that represents its creation.
   */
  final lazy val OPTION: Conversion = {
    val pack = "scala"
    lazy val Some_syms = symsOf("Some", pack)
    lazy val None_sym = currentMirror.staticModule("scala.None")

    {
      case (col @ Some(v), tpe: Type, conversion: Conversion) =>
        collectionApply(Some_syms, col, List(v), tpe, 1, conversion)._1
      case (col @ None, tpe: Type, conversion: Conversion) =>
        getModulePath(universe)(None_sym)
    }
  }

  /**
   * Convert a tuple to an AST that represents its creation.
   */
  final lazy val TUPLE: Conversion = {
    object ProductAndClassName {
      val rx = """scala\.(Tuple(\d+))(?:\$.*)?""".r
      def unapply(v: Any): Option[(AnyRef with Product, String, Int)] = v match {
        case p: AnyRef with Product =>
          Option(p.getClass.getName) collect {
            case rx(name, arity) => (p, name, arity.toInt)
          }
        case _ =>
          None
      }
    }

    {
      case (ProductAndClassName(prod, className, arity), tpe: Type, conversion: Conversion) =>
        val syms = symsOf(className, "scala")
        collectionApply(syms, prod, prod.productIterator.toList, tpe, arity, conversion)._1
    }
  }
}
