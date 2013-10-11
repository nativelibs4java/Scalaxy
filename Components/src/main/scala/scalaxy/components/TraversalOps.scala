/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 21:40
 */
package scalaxy.components

import scala.reflect.api.Universe

trait TraversalOps
    extends CommonScalaNames
    with StreamOps
    with MiscMatchers {
  val global: Universe

  import global._
  import definitions._

  case class TraversalOp(
    op: TraversalOpType,
    collection: Tree,
    resultType: Type,
    mappedCollectionType: Type,
    isLeft: Boolean,
    initialValue: Tree)

  object ReduceName {
    def apply(isLeft: Boolean) = sys.error("not implemented")
    def unapply(name: String) = Option(name) collect {
      case "reduceLeft" => true
      case "reduceRight" => false
    }
  }
  object ScanName {
    def apply(isLeft: Boolean) = sys.error("not implemented")
    def unapply(name: String) = Option(name) collect {
      case "scanLeft" => true
      case "scanRight" => false
    }
  }
  object FoldName {
    def apply(isLeft: Boolean) = sys.error("not implemented")
    def unapply(name: String) = Option(name) collect {
      case "foldLeft" => true
      case "foldRight" => false
    }
  }

  def refineComponentType(componentType: Type, collectionTree: Tree): Type = {
    collectionTree.tpe match {
      case TypeRef(_, _, List(t)) =>
        t
      case _ =>
        componentType
    }
  }

  import TraversalOps._

  def traversalOpWithoutArg(n: String, tree: Tree) = Option(n) collect {
    case "toList" =>
      ToListOp(tree)
    case "toArray" =>
      ToArrayOp(tree)
    case "toSeq" =>
      ToSeqOp(tree) // TODO !!!
    case "toIndexedSeq" =>
      ToIndexedSeqOp(tree)
    case "toVector" =>
      ToVectorOp(tree)
    case "reverse" =>
      ReverseOp(tree) // TODO !!!
  }

  def basicTypeApplyTraversalOp(tree: Tree, collection: Tree, name: String, typeArgs: List[Tree], args: Seq[List[Tree]]): Option[TraversalOp] = {
    (name, typeArgs, args) match {

      case ("sum", List(tpt), List(_)) =>
        Some(new TraversalOp(SumOp(tree), collection, tpt.tpe, null, true, null))
      case ("product", List(tpt), List(_)) =>
        Some(new TraversalOp(ProductOp(tree), collection, tpt.tpe, null, true, null))
      case ("min", List(tpt), List(_)) =>
        Some(new TraversalOp(MinOp(tree), collection, tpt.tpe, null, true, null))
      case ("max", List(tpt), List(_)) =>
        Some(new TraversalOp(MaxOp(tree), collection, tpt.tpe, null, true, null))
      case ("collect", List(mappedComponentType), Seq(List(function @ Func(List(_), _)), List(cb @ CanBuildFromArg()))) =>
        Some(new TraversalOp(CollectOp(tree, function, cb), collection, refineComponentType(mappedComponentType.tpe, tree), null, true, null))
      case // Option.map[B](f)
      (
        "map",
        List(mappedComponentType),
        Seq(
          List(function @ Func(List(_), _))
          )
        ) =>
        Some(new TraversalOp(MapOp(tree, function, null), collection, refineComponentType(mappedComponentType.tpe, tree), null, true, null))
      case // map[B, That](f)(canBuildFrom)
      (
        "map",
        List(mappedComponentType, mappedCollectionType),
        Seq(
          List(function @ Func(List(_), _)),
          List(canBuildFrom @ CanBuildFromArg())
          )
        ) =>
        Some(new TraversalOp(MapOp(tree, function, canBuildFrom), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      case (
        "foreach",
        List(fRetType),
        Seq(
          List(function)
          )
        ) =>
        Some(new TraversalOp(ForeachOp(tree, function), collection, null, null, true, null))

      case // scanLeft, scanRight
      (
        ScanName(isLeft),
        List(functionResultType, mappedArrayType),
        Seq(
          List(initialValue),
          List(function),
          List(canBuildFrom @ CanBuildFromArg())
          )
        ) => //if isLeft =>
        Some(new TraversalOp(ScanOp(tree, function, initialValue, isLeft, canBuildFrom), collection, functionResultType.tpe, null, isLeft, initialValue))
      case // foldLeft, foldRight
      (
        FoldName(isLeft),
        List(functionResultType),
        Seq(
          List(initialValue),
          List(function)
          )
        ) => //if isLeft =>
        Some(new TraversalOp(FoldOp(tree, function, initialValue, isLeft), collection, functionResultType.tpe, null, isLeft, initialValue))
      case // toArray
      (
        "toArray",
        List(functionResultType @ TypeTree()),
        Seq(
          List(manifest)
          )
        ) =>
        Some(new TraversalOp(new ToArrayOp(tree), collection, functionResultType.tpe, null, true, null))
      case // sum, product, min, max
      (
        n @ ("sum" | "product" | "min" | "max"),
        List(functionResultType @ TypeTree()),
        Seq(
          List(isNumeric)
          )
        ) =>
        isNumeric.toString match {
          case "math.this.Numeric.IntIsIntegral" |
            "math.this.Numeric.ShortIsIntegral" |
            "math.this.Numeric.LongIsIntegral" |
            "math.this.Numeric.ByteIsIntegral" |
            "math.this.Numeric.CharIsIntegral" |
            "math.this.Numeric.FloatIsFractional" |
            "math.this.Numeric.DoubleIsFractional" |
            "math.this.Numeric.DoubleAsIfIntegral" |
            "math.this.Ordering.Int" |
            "math.this.Ordering.Short" |
            "math.this.Ordering.Long" |
            "math.this.Ordering.Byte" |
            "math.this.Ordering.Char" |
            "math.this.Ordering.Double" |
            "math.this.Ordering.Float" =>
            traversalOpWithoutArg(n, tree).collect { case op => new TraversalOp(op, collection, functionResultType.tpe, null, true, null) }
          case _ =>
            None
        }
      case // reduceLeft, reduceRight
      (
        ReduceName(isLeft),
        List(functionResultType),
        Seq(
          List(function)
          )
        ) => //if isLeft =>
        Some(new TraversalOp(ReduceOp(tree, function, isLeft), collection, functionResultType.tpe, null, isLeft, null))
      case // zip(col)(canBuildFrom)
      (
        "zip",
        List(mappedComponentType, otherComponentType, mappedCollectionType),
        Seq(
          List(zippedCollection),
          List(canBuildFrom @ CanBuildFromArg())
          )
        ) =>
        Some(new TraversalOp(ZipOp(tree, zippedCollection), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      // zipWithIndex(canBuildFrom)
      case (
        "zipWithIndex",
        List(mappedComponentType, mappedCollectionType),
        Seq(
          List(canBuildFrom @ CanBuildFromArg())
          )
        ) =>
        Some(new TraversalOp(ZipWithIndexOp(tree), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      case _ =>
        //println("Failed basicTypeApplyTraversalOp: " + (name, typeArgs, args)) //showRaw(tree))
        None
    }
  }
  object SomeTraversalOp {

    def unapply(tree: Tree): Option[TraversalOp] = tree match {
      // Option.map[B](f)
      case BasicTypeApply(collection, N(name), typeArgs, args) =>
        // Having a separate matcher helps avoid "jump offset too large for 16 bits integers" errors when generating bytecode
        basicTypeApplyTraversalOp(tree, collection, name, typeArgs, args)
      case TypeApply(Select(collection, N("toSet")), List(resultType)) =>
        Some(new TraversalOp(ToSetOp(tree), collection, resultType.tpe, tree.tpe, true, null))
      // reverse, toList, toSeq, toIndexedSeq
      case Select(collection, N(n @ ("reverse" | "toList" | "toSeq" | "toIndexedSeq" | "toVector"))) =>
        traversalOpWithoutArg(n, tree).collect { case op => new TraversalOp(op, collection, null, null, true, null) }
      //Some(new TraversalOp(Reverse, collection, null, null, true, null))
      // filter, filterNot, takeWhile, dropWhile, forall, exists
      case Apply(Select(collection, N(n)), List(function @ Func(List(param), body))) =>
        Option(n) collect {
          case "withFilter" =>
            FilterOp(tree, function, false) -> collection.tpe
          case "filter" =>
            FilterOp(tree, function, false) -> collection.tpe
          case "filterNot" =>
            FilterOp(tree, function, true) -> collection.tpe

          case "takeWhile" =>
            FilterWhileOp(tree, function, true) -> collection.tpe
          case "dropWhile" =>
            FilterWhileOp(tree, function, false) -> collection.tpe

          case "forall" =>
            AllOrSomeOp(tree, function, true) -> BooleanTpe
          case "exists" =>
            AllOrSomeOp(tree, function, false) -> BooleanTpe

          case "find" =>
            FindOp(tree, function) -> tree.tpe

          case "count" =>
            CountOp(tree, function) -> IntTpe
          // case "update" =>
          //   UpdateAllOp(tree, function) -> collection.tpe
        } map {
          case (op, resType) =>
            new TraversalOp(op, collection, resType, null, true, null)
        }
      case _ =>
        None
    }

  }
}
