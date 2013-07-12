package scalaxy.reified.base

import scalaxy.reified.impl.Utils._

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

  final lazy val DEFAULT: Conversion = {
    CONSTANT orElse
      REIFIED_VALUE orElse
      ARRAY orElse
      IMMUTABLE_COLLECTION
  }

  final lazy val CONSTANT: Conversion = {
    case (value @ (
      (_: Number) |
      (_: java.lang.Boolean) |
      (_: String) |
      (_: java.lang.Character)), tpe: Type, conversion: Conversion) =>
      Literal(Constant(value))
  }

  final lazy val REIFIED_VALUE: Conversion = {
    case (value: ReifiedValue[_], tpe: Type, conversion: Conversion) =>
      value.expr(conversion).tree.duplicate
  }

  // returns collection.apply + elementType
  private def collectionApply(
    syms: (ModuleSymbol, TermSymbol),
    col: Iterable[_],
    tpe: Type,
    conversion: Conversion): (Tree, Type) = {

    val (moduleSym, methodSym) = syms
    val (elementType, castToAnyRef) = (tpe, col) match {
      case (TypeRef(_, _, elementType :: _), _) if tpe <:< typeOf[Traversable[_]] =>
        //println(s"GOT ELEMENT TYPE $elementType")
        elementType -> false
      case (_, wa: collection.mutable.WrappedArray[_]) =>
        val elementManifest = wa.elemTag.asInstanceOf[Manifest[_]]
        // case _: Array[_] => ClassManifestFactory.classType(col.getClass.getComponentType)
        manifestToTypeTag(currentMirror, elementManifest).tpe.asInstanceOf[Type] -> false
      case _ =>
        typeOf[AnyRef] -> true
    }

    (
      Apply(
        TypeApply(
          Select(
            getModulePath(universe)(moduleSym), //Ident(moduleSym),
            methodSym),
          List(TypeTree(elementType))),
        col.map(value => {
          val convertedValue = conversion((value, elementType, conversion))
          if (castToAnyRef) {
            val convertedValueExpr = newExpr[Any](convertedValue)
            universe.reify(
              convertedValueExpr.splice.asInstanceOf[AnyRef]
            ).tree
          } else {
            convertedValue
          }
        }).toList
      ),
        elementType
    )
  }

  /** @deprecated Still buggy */
  //@deprecated("Still buggy", since = "")
  final lazy val ARRAY: Conversion = {
    lazy val Array_syms = (ArrayModule, ArrayModule_overloadedApply)

    {
      case (array: Array[_], tpe: Type, conversion: Conversion) =>
        val (conv, elementType) = collectionApply(Array_syms, array, tpe, conversion)
        val classTagType = for (t <- typeOf[ClassTag[Int]]) yield {
          if (t == typeOf[Int]) elementType
          else t
        }
        Apply(
          conv,
          List(toolbox.inferImplicitValue(classTagType)))
    }
  }

  final lazy val IMMUTABLE_COLLECTION: Conversion = {
    def symsOf(name: String) = {
      val moduleSym = currentMirror.staticModule("scala.collection.immutable." + name)
      val methodSym = moduleSym.moduleClass.typeSignature.member("apply": TermName).asTerm
      (moduleSym, methodSym)
    }
    //lazy val TreeSet_syms = symsOf("TreeSet")
    //lazy val SortedSet_syms = symsOf("SortedSet")
    lazy val HashSet_syms = symsOf("HashSet")
    //lazy val BitSet_syms = symsOf("BitSet")
    lazy val Set_syms = symsOf("Set")
    lazy val List_syms = symsOf("List")
    lazy val Vector_syms = symsOf("Vector")
    lazy val Stack_syms = symsOf("Stack")
    lazy val Queue_syms = symsOf("Queue")
    lazy val Seq_syms = symsOf("Seq")

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
      //  collectionApply(TreeSet_syms, col, tpe, conversion)._1
      //case (col: immutable.SortedSet[_], tpe: Type, conversion: Conversion)
      //    if tpe != typeOf[AnyRef] && tpe != typeOf[Any] =>
      //  collectionApply(SortedSet_syms, col, tpe, conversion)._1
      case (col: immutable.HashSet[_], tpe: Type, conversion: Conversion) =>
        collectionApply(HashSet_syms, col, tpe, conversion)._1
      case (col: immutable.Set[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Set_syms, col, tpe, conversion)._1
      case (col: immutable.List[_], tpe: Type, conversion: Conversion) =>
        collectionApply(List_syms, col, tpe, conversion)._1
      case (col: immutable.Vector[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Vector_syms, col, tpe, conversion)._1
      case (col: immutable.Stack[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Stack_syms, col, tpe, conversion)._1
      case (col: immutable.Queue[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Queue_syms, col, tpe, conversion)._1
      case (col: immutable.Seq[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Seq_syms, col, tpe, conversion)._1
      // TODO: Map, BitSet
      //case (map: immutable.Map[_], tpe: Type, conversion: Conversion) =>      
    }
  }
}
