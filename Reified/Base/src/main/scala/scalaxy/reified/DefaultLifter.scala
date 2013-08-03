package scalaxy.reified

import scalaxy.reified.internal.Utils._

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.definitions._
import scala.reflect.runtime.currentMirror
import scala.collection.immutable
import scala.reflect.{ ClassTag, Manifest, ClassManifestFactory }

class DefaultLifter extends Lifter {

  /**
   * @return symbol of specified module + symbol of its apply method
   */
  private def symsOf(name: String, pack: String) = {
    val moduleSym = currentMirror.staticModule(pack + "." + name)
    val methodSym = moduleSym.moduleClass.typeSignature.member("apply": TermName).asTerm
    (moduleSym, methodSym)
  }

  /**
   * @return collection creation tree + list of element types (of size 1 for collections
   * bigger for tuples
   */
  private def collectionApply(
    syms: (ModuleSymbol, TermSymbol),
    col: AnyRef,
    elements: List[_],
    tpe: Type,
    tpeArity: Int,
    forceConversion: Boolean): Option[(Tree, List[Type])] = {

    def nTimes[V](n: Int)(v: V): List[V] = (0 until n).map(_ => v).toList

    val (moduleSym, methodSym) = syms
    val (builderTArgs, elementTypes, castToAnyRef) = (tpe, col) match {
      case (TypeRef(_, _, targs), _) if tpe <:< typeOf[Map[_, _]] && targs.size == tpeArity =>
        val builderTArgs @ List(keyTpe, valueTpe) = targs.take(tpeArity)

        val elementType = for (t <- typeOf[(Int, Float)]) yield {
          if (t == typeOf[Int]) keyTpe
          else if (t == typeOf[Float]) valueTpe
          else t
        }
        (builderTArgs, List(elementType), false)
      case (TypeRef(_, _, targs), _) if (tpe <:< typeOf[Iterable[_]] || tpe <:< typeOf[Product]) && targs.size >= tpeArity =>
        val elementTypes = targs.take(tpeArity)
        (elementTypes, elementTypes, false) //elementTypes.exists(_ <:< typeOf[AnyRef]))
      case (_, wa: collection.mutable.WrappedArray[_]) =>
        assert(tpeArity == 1)
        val elementManifest = wa.elemTag.asInstanceOf[Manifest[_]]
        val elementType = manifestToTypeTag(currentMirror, elementManifest).tpe.asInstanceOf[Type]
        (List(elementType), List(elementType), false)
      case (_, _: immutable.Map[_, _]) =>
        (nTimes(tpeArity)(typeOf[AnyRef]), List(typeOf[AnyRef]), false)
      case _ =>
        val elementTypes = nTimes(tpeArity)(typeOf[AnyRef])
        (elementTypes, elementTypes, true)
    }

    val optValues = elements.zipWithIndex.map({
      case (value, i) =>
        for (convertedValue <- lift(value, if (elementTypes.size == 1) elementTypes(0) else elementTypes(i), forceConversion)) yield {
          if (castToAnyRef) {
            val convertedValueExpr = newExpr[Any](convertedValue)
            universe.reify(
              convertedValueExpr.splice.asInstanceOf[AnyRef]
            ).tree
          } else {
            convertedValue
          }
        }
    })
    if (optValues.forall(_ != None)) {
      val values = optValues.map(_.get)
      val tree = {
        Apply(
          TypeApply(
            Select(
              getModulePath(universe)(moduleSym), //Ident(moduleSym),
              methodSym),
            builderTArgs.map(TypeTree(_))),
          values)
      }
      //println(s"col = $col, builderTArgs = $builderTArgs, elementTypes = $elementTypes")
      //println(s"tree = $tree")
      Some(tree, elementTypes)
    } else {
      None
    }
  }

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

  private lazy val Array_syms = (ArrayModule, ArrayModule_overloadedApply)

  private val immutablePackage = "scala.collection.immutable"
  private lazy val HashSet_syms = symsOf("HashSet", immutablePackage)
  private lazy val Set_syms = symsOf("Set", immutablePackage)
  private lazy val List_syms = symsOf("List", immutablePackage)
  private lazy val Vector_syms = symsOf("Vector", immutablePackage)
  private lazy val Stack_syms = symsOf("Stack", immutablePackage)
  private lazy val Queue_syms = symsOf("Queue", immutablePackage)
  private lazy val Seq_syms = symsOf("Seq", immutablePackage)
  private lazy val Map_syms = symsOf("Map", immutablePackage)

  private lazy val Some_syms = symsOf("Some", "scala")
  private lazy val None_sym = currentMirror.staticModule("scala.None")

  override def lift(value: Any, tpe: Type, forceConversion: Boolean): Option[Tree] = {

    // TODO: BitSet, TreeSet, SortedSet
    value match {
      // Convert constants.
      case (_: Number) | (_: java.lang.Boolean) | (_: java.lang.Character) =>
        Some(Literal(Constant(value)))

      case _ =>
        if (forceConversion) {
          value match {
            case _: String =>
              Some(Literal(Constant(value)))

            // Convert arrays.
            case array: Array[_] =>
              for ((conv, List(elementType)) <- collectionApply(Array_syms, array: Traversable[_], array.toList, tpe, 1, forceConversion)) yield {
                val classTagType = for (t <- typeOf[ClassTag[Int]]) yield {
                  if (t == typeOf[Int]) elementType
                  else t
                }
                Apply(
                  conv,
                  List(
                    resolveModulePaths(universe)(optimisingToolbox.inferImplicitValue(classTagType))))
              }

            // Convert tuples.
            case ProductAndClassName(prod, className, arity) =>
              val syms = symsOf(className, "scala")
              collectionApply(syms, prod, prod.productIterator.toList, tpe, arity, forceConversion).map(_._1)

            // Convert immutable collections.
            case col: immutable.Range =>
              val start = newExpr[Int](Literal(Constant(col.start)))
              val end = newExpr[Int](Literal(Constant(col.end)))
              val step = newExpr[Int](Literal(Constant(col.step)))
              Some(
                if (col.isInclusive)
                  universe.reify(start.splice to end.splice by step.splice).tree
                else
                  universe.reify(start.splice until end.splice by step.splice).tree
              )
            case col: immutable.HashSet[_] =>
              collectionApply(HashSet_syms, col, col.toList, tpe, 1, forceConversion).map(_._1)
            case col: immutable.Set[_] =>
              collectionApply(Set_syms, col, col.toList, tpe, 1, forceConversion).map(_._1)
            case col: immutable.List[_] =>
              collectionApply(List_syms, col, col.toList, tpe, 1, forceConversion).map(_._1)
            case col: immutable.Vector[_] =>
              collectionApply(Vector_syms, col, col.toList, tpe, 1, forceConversion).map(_._1)
            case col: immutable.Stack[_] =>
              collectionApply(Stack_syms, col, col.toList, tpe, 1, forceConversion).map(_._1)
            case col: immutable.Queue[_] =>
              collectionApply(Queue_syms, col, col.toList, tpe, 1, forceConversion).map(_._1)
            case col: immutable.Seq[_] =>
              collectionApply(Seq_syms, col, col.toList, tpe, 1, forceConversion).map(_._1)
            case col: immutable.Map[_, _] =>
              collectionApply(Map_syms, col, col.toList, tpe, 2, forceConversion).map(_._1)
            // TODO inject ordering and support TreeSet, SortedSet if tpe != AnyRef

            // Convert options.
            case col @ Some(v) =>
              collectionApply(Some_syms, col, List(v), tpe, 1, forceConversion).map(_._1)
            case col @ None =>
              Some(getModulePath(universe)(None_sym))
            case _ =>
              sys.error(s"This type of value is not supported: $value (static type: $tpe)")
          }
        } else {
          None
        }
    }
  }
}

