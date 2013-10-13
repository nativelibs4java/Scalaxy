/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 15:20
 */
package scalaxy.components

import scala.reflect.api.Universe

trait StreamSources
    extends Streams
    with StreamSinks
    with CommonScalaNames {
  val global: Universe
  import global._
  import definitions._

  trait AbstractArrayStreamSource extends StreamSource {
    def tree: Tree
    def array: Tree
    def componentType: Type

    override def unwrappedTree = array
    override def privilegedDirection = None
    def emit(direction: TraversalDirection)(implicit loop: Loop) = {
      import loop.{ currentOwner, transform }
      val pos = array.pos

      val skipFirst = false // TODO
      val reverseOrder = direction == FromRight

      val aVal = newVal("array$", transform(array), getArrayType(componentType))
      val nVal = newVal("n$", newArrayLength(aVal()), IntTpe)
      val iVar = newVar("i$",
        if (reverseOrder) {
          if (skipFirst)
            intSub(nVal(), newInt(1))
          else
            nVal()
        } else {
          if (skipFirst)
            newInt(1)
          else
            newInt(0)
        },
        IntTpe
      )

      val itemVal = newVal("item$", newApplyCall(aVal(), iVar()), componentType)

      loop.preOuter += aVal.definition
      loop.preOuter += nVal.definition
      loop.preOuter += iVar.definition
      loop.tests += (
        if (reverseOrder)
          binOp(iVar(), GT, newInt(0))
        else
          binOp(iVar(), LT, nVal())
      )

      loop.preInner += itemVal.definition
      loop.postInner += (
        if (reverseOrder)
          decrementIntVar(iVar, newInt(1))
        else
          incrementIntVar(iVar, newInt(1))
      )
      new StreamValue(
        value = itemVal,
        valueIndex = Some(iVar),
        valuesCount = Some(nVal)
      )
    }
  }
  case class WrappedArrayStreamSource(tree: Tree, array: Tree, componentType: Type)
      extends AbstractArrayStreamSource
      with CanCreateArraySink
      with SideEffectFreeStreamComponent {
    override def isResultWrapped = true
  }

  abstract class ExplicitCollectionStreamSource(val tree: Tree, items: List[Tree], val componentType: Type)
      extends AbstractArrayStreamSource {
    val array = newArrayApply(TypeTree(componentType), items: _*)

    override def analyzeSideEffectsOnStream(analyzer: SideEffectsAnalyzer) =
      analyzer.analyzeSideEffects(tree, items: _*)
  }
  case class ListStreamSource(tree: Tree, componentType: Type)
      extends StreamSource
      with CanCreateListSink
      with SideEffectFreeStreamComponent {
    val list = tree // TODO 

    override def unwrappedTree = list
    override def privilegedDirection = Some(FromLeft)
    def emit(direction: TraversalDirection)(implicit loop: Loop) = {
      import loop.{ currentOwner, transform }
      assert(direction == FromLeft)

      val pos = list.pos

      val skipFirst = false // TODO
      val colTpe = list.tpe

      val aVar = newVar("list$", transform(list), getListType(componentType))
      val itemVar = newVal("item$", newSelect(aVar(), headName), componentType)

      loop.preOuter += aVar.definition
      loop.tests += (
        if ("1" == System.getenv("SCALACL_LIST_TEST_ISEMPTY")) // Safer, but 10% slower
          boolNot(newSelect(aVar(), isEmptyName))
        else
          newIsInstanceOf(aVar(), appliedType(NonEmptyListClass.asType.toType.typeConstructor, List(componentType)))
      )

      loop.preInner += itemVar.definition
      loop.postInner += (
        typeCheck(
          Assign(
            aVar(),
            newSelect(aVar(), tailName)
          ),
          UnitTpe
        )
      )
      new StreamValue(itemVar)
    }
  }

  case class RangeStreamSource(tree: Tree, from: Tree, to: Tree, byValue: Int, isUntil: Boolean, itemTpe: Type)
      extends StreamSource
      with CanCreateVectorSink
      with SideEffectFreeStreamComponent {
    override def privilegedDirection = Some(FromLeft)

    def emit(direction: TraversalDirection)(implicit loop: Loop) = {
      assert(direction == FromLeft)
      import loop.{ currentOwner, transform }
      val pos = tree.pos

      val fromVal = newVal("from$", transform(from), itemTpe)
      val toVal = newVal("to$", transform(to), itemTpe)
      val itemVar = newVar("item$", fromVal(), itemTpe)
      val itemVal = newVal("item$val$", itemVar(), itemTpe)

      val size = {
        val span = intSub(toVal(), fromVal())
        val width = if (isUntil)
          span
        else
          intAdd(span, newInt(1))

        if (byValue == 1)
          width
        else
          intDiv(width, newInt(byValue))
      }
      val sizeVal = newVal("outputSize$", size, itemTpe)
      val iVar = newVar("outputIndex$", newInt(0), itemTpe) //if (reverseOrder) intSub(outputSizeVar(), newInt(1)) else newInt(0))
      val iVal = newVal("i", iVar(), itemTpe) //if (reverseOrder) intSub(outputSizeVar(), newInt(1)) else newInt(0))

      loop.preOuter += fromVal.definition
      loop.preOuter += toVal.definition
      loop.preOuter += itemVar.definition
      loop.preOuter += (sizeVal.defIfUsed _)
      loop.preOuter += (() => if (iVal.identUsed) Some(iVar.definition) else None)
      loop.tests += (
        binOp(
          itemVar(),
          if (isUntil) {
            if (byValue < 0) GT else LT
          } else {
            if (byValue < 0) GE else LE
          },
          toVal()
        )
      )
      loop.preInner += itemVal.definition // it's important to keep a non-mutable local reference !
      loop.preInner += (iVal.defIfUsed _)

      loop.postInner += incrementIntVar(itemVar, newInt(byValue))
      loop.postInner += (() => iVal.ifUsed { incrementIntVar(iVar, newInt(1)) })

      new StreamValue(
        value = itemVal,
        valueIndex = Some(iVal),
        valuesCount = Some(sizeVal)
      )
    }
  }
  case class OptionStreamSource(tree: Tree, componentOption: Option[Tree], onlyIfNotNull: Boolean, componentType: Type)
      extends StreamSource
      with CanCreateOptionSink
      with SideEffectFreeStreamComponent {
    def emit(direction: TraversalDirection)(implicit loop: Loop) = {
      import loop.{ currentOwner, transform }
      val pos = tree.pos

      loop.isLoop = false

      val (valueVar: ValueDef, isDefinedVar: ValueDef, isAlwaysDefined: Boolean) = componentOption match {
        case Some(component) =>
          val valueVar = newVal("value$", transform(component), componentType)
          val (isDefinedValue, isAlwaysDefined) =
            if (onlyIfNotNull && !isAnyVal(component.tpe))
              component match {
                case Literal(Constant(v)) =>
                  val isAlwaysDefined = v != null
                  (newBool(isAlwaysDefined), isAlwaysDefined)
                case _ =>
                  (newIsNotNull(valueVar()), false)
              }
            else
              (newBool(true), true)
          val isDefinedVar = newVal("isDefined$", isDefinedValue, BooleanTpe)
          loop.preOuter += valueVar.definition
          (valueVar, isDefinedVar, isAlwaysDefined)
        case None =>
          val optionVar = newVal("option$", transform(tree), getOptionType(componentType))
          val isDefinedVar = newVal("isDefined$", newSelect(optionVar(), N("isDefined")), BooleanTpe)
          val valueVar = newVal("value$", newSelect(optionVar(), N("get")), componentType)
          loop.preOuter += optionVar.definition
          loop.preInner += valueVar.definition
          (valueVar, isDefinedVar, false)
      }
      if (!isAlwaysDefined) {
        loop.preOuter += isDefinedVar.definition
        loop.tests += isDefinedVar()
      } else
        loop.tests += newBool(true)

      new StreamValue(
        value = valueVar,
        valueIndex = Some(() => newInt(0)),
        valuesCount = Some(() => typed {
          if (isAlwaysDefined)
            newInt(1)
          else
            If(isDefinedVar(), newInt(1), newInt(0))
        })
      )
    }
  }

  case class ArrayApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type)
      extends ExplicitCollectionStreamSource(tree, components, componentType)
      with CanCreateArraySink {
    override def isResultWrapped = false
  }

  case class SeqApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type)
    extends ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateListSink // default Seq implementation is List

  case class IndexedSeqApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type)
    extends ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateVectorSink // default IndexedSeq implementation is Vector

  case class VectorApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type)
    extends ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateVectorSink // default IndexedSeq implementation is Vector

  case class ListApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type)
    extends ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateListSink

  object StreamSource {
    object By {
      def unapply(treeOpt: Option[Tree]) = treeOpt match {
        case None =>
          Some(1)
        case Some(Literal(Constant(v: Int))) =>
          Some(v)
        case _ =>
          None
      }
    }
    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case ArrayApply(components, componentType) =>
        new ArrayApplyStreamSource(tree, components, componentType)
      case SeqApply(components, componentType) =>
        new SeqApplyStreamSource(tree, components, componentType)
      case IndexedSeqApply(components, componentType) =>
        new IndexedSeqApplyStreamSource(tree, components, componentType)
      case ListApply(components, componentType) =>
        new ListApplyStreamSource(tree, components, componentType)
      case WrappedArrayTree(array, componentType) =>
        WrappedArrayStreamSource(tree, array, componentType)
      case ListTree(componentType) =>
        ListStreamSource(tree, componentType)
      case TreeWithType(_, TypeRef(_, c, List(componentType))) if c == ListClass | c == ImmutableListClass =>
        ListStreamSource(tree, componentType)
      case OptionApply(List(component), componentType) =>
        OptionStreamSource(tree, Some(component), onlyIfNotNull = true, component.tpe)
      case OptionTree(componentType) =>
        OptionStreamSource(tree, None, onlyIfNotNull = true, componentType)
      case NumRange(rangeTpe, itemTpe, from, to, By(byValue), isUntil, filters) =>
        assert(filters.isEmpty, "Filters are not empty !!!")
        RangeStreamSource(tree, from, to, byValue, isUntil /*, filters*/ , itemTpe)
    }
    // orElse {
    //   println("Failed: " + showRaw(tree))
    //   None
    // }
  }
}
