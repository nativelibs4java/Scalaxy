package scalaxy.streams
import scala.reflect.NameTransformer
import scala.collection.breakOut
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

private[streams] object SideEffectsWhitelists {

  lazy val whitelistedSymbols: Set[String] =
    whitelistedPackages ++
    whitelistedClasses ++
    whitelistedMethods ++
    whitelistedPredefSymbols ++
    whitelistedRuntimeSymbols ++
    whitelistedModuleReferences

  /**
   * Any symbol under this package will be whitelisted!!!
   */
  lazy val whitelistedPackages = Set[String](
    "scala.collection.immutable"
  )

  private[this] lazy val whitelistedMethods = Set[String](
    "scala.Option.option2Iterable", // What about indirect .toString?
    "scala.Array.canBuildFrom",
    "scala.collection.generic.GenericCompanion.apply"
  )

  /**
   * Note: We only whitelist a couple of known modules, by feat that
   * referencing other modules might produce side-effects.
   */
  private[this] lazy val whitelistedModuleReferences = Set[String](
    "scala.Array",
    "scala.Predef"
  )

  private[this] lazy val tupleSymbols: Set[String] = (2 until 22).map("scala.Tuple" + _).toSet

  lazy val trulyImmutableClasses = tupleSymbols ++ Set[String](
    "scala.Long",
    "scala.Int",
    "scala.Short",
    "scala.Byte",
    "scala.Boolean",
    "scala.Char",
    "scala.Double",
    "scala.Float",
    "scala.runtime.RichLong",
    "scala.runtime.RichInt",
    "scala.runtime.RichShort",
    "scala.runtime.RichByte",
    "scala.runtime.RichBoolean",
    "scala.runtime.RichChar",
    "scala.runtime.RichDouble",
    "scala.runtime.RichFloat",
    "java.lang.String",
    "java.lang.Long",
    "java.lang.Integer",
    "java.lang.Short",
    "java.lang.Byte",
    "java.lang.Boolean",
    "java.lang.Character",
    "java.lang.Float",
    "java.lang.Double",
    "java.lang.Class",
    "scala.Predef.Class",
    "scala.reflect.ClassTag"
  )

  /**
   * Any method of these classes is whitelisted.
   */
  lazy val whitelistedClasses = trulyImmutableClasses ++ tupleSymbols ++ Set[String](
    // "scala.collection.TraversableLike",
    // "scala.collection.generic.FilterMonadic",
    // "scala.collection.SetLike",
    // "scala.collection.SeqLike",
    // "scala.collection.generic.GenericCompanion",
    "scala.Option",
    "scala.Some",
    "scala.None",

    "scala.LowPriorityImplicits",
    "scala.Predef.any2stringadd",
    "scala.Predef.ArrayCharSequence",
    "scala.Predef.ArrowAssoc",
    "scala.Predef.DummyImplicit",
    "scala.Predef.Ensuring",
    "scala.Predef.Function",
    "scala.Predef.Map",
    "scala.Predef.Pair,",
    "scala.Predef.RichException",
    "scala.Predef.SeqCharSequence",
    "scala.Predef.Set",
    "scala.Predef.String",
    "scala.Predef.StringCanBuildFrom",
    "scala.Predef.StringFormat",
    "scala.Predef.Triple"
  )

  /**
   * Many of the ScalaRunTime methods call hashCode, equals or toString,
   * which may produce side-effects.
   *
   * The following whitelist is conservative: it only neglects class-loading side-effects.
   */
  private[this] lazy val whitelistedRuntimeSymbols = Set(
    "scala.runtime.ScalaRunTime.isArray",
    "scala.runtime.ScalaRunTime.isValueClass",
    "scala.runtime.ScalaRunTime.isTuple",
    "scala.runtime.ScalaRunTime.isAnyVal",
    "scala.runtime.ScalaRunTime.arrayClass",
    "scala.runtime.ScalaRunTime.arrayElementClass",
    "scala.runtime.ScalaRunTime.anyValClass",
    "scala.runtime.ScalaRunTime.array_apply",
    "scala.runtime.ScalaRunTime.array_length",
    "scala.runtime.ScalaRunTime.array_clone",
    "scala.runtime.ScalaRunTime.toObjectArray",
    "scala.runtime.ScalaRunTime.toArray",
    "scala.runtime.ScalaRunTime.typedProductIterator",
    "scala.runtime.ScalaRunTime.arrayToString",
    "scala.runtime.ScalaRunTime.box"
  )

  private[this] lazy val whitelistedPredefSymbols = Set(
    "scala.Option.apply",
    "scala.Array.apply",
    "scala.Array.length",
    "scala.Predef.classOf",

    "scala.Predef.identity",
    "scala.Predef.implicitly",

    "scala.Predef.augmentString",
    "scala.Predef.unaugmentString",
    "scala.Predef.tuple2ToZippedOps",
    "scala.Predef.tuple3ToZippedOps",

    "scala.Predef.genericArrayOps",
    "scala.Predef.booleanArrayOps",
    "scala.Predef.byteArrayOps",
    "scala.Predef.charArrayOps",
    "scala.Predef.doubleArrayOps",
    "scala.Predef.floatArrayOps",
    "scala.Predef.intArrayOps",
    "scala.Predef.longArrayOps",
    "scala.Predef.refArrayOps",
    "scala.Predef.shortArrayOps",
    "scala.Predef.unitArrayOps",

    "scala.Predef.byte2Byte",
    "scala.Predef.short2Short",
    "scala.Predef.char2Character",
    "scala.Predef.int2Integer",
    "scala.Predef.long2Long",
    "scala.Predef.float2Float",
    "scala.Predef.double2Double",
    "scala.Predef.boolean2Boolean",

    "scala.Predef.Byte2byte",
    "scala.Predef.Short2short",
    "scala.Predef.Character2char",
    "scala.Predef.Integer2int",
    "scala.Predef.Long2long",
    "scala.Predef.Float2float",
    "scala.Predef.Double2double",
    "scala.Predef.Boolean2boolean"
  )
}
