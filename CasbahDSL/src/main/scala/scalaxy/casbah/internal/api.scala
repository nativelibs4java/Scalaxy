package scalaxy.casbah.internal

import scala.language.dynamics
import scala.language.implicitConversions
import scala.util.matching.Regex

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.query.dsl._

sealed trait Doc extends Dynamic {

  object set extends Dynamic {
    /** `doc.set(a = b)` => `$set(a -> b)` */
    @applyDynNamedFunc("$set")
    def applyDynamicNamed(n: String)(args: (String, Col)*): Col = ???
  }
  object rename extends Dynamic {
    /** `doc.rename(a = b)` => `$rename(a -> b)` */
    @applyDynNamedFunc("$rename")
    def applyDynamicNamed(n: String)(args: (String, String)*): Col = ???
  }
  object setOnInsert extends Dynamic {
    /** `doc.setOnInsert(a = b)` => `$setOnInsert(a -> b)` */
    @applyDynNamedFunc("$setOnInsert")
    def applyDynamicNamed(n: String)(args: (String, Col)*): Col = ???
  }

  @peel def selectDynamic(name: String): Col

  /** `doc.a = b` => `$set(a -> b)` */
  @updateDynFunc("$set") def updateDynamic(name: String)(value: Col): Col

  /** `doc -= a` => `$unset(a)` */
  @func1("$unset") def -=(n: String): Col

  /**  `doc.contains(k)` => `k $exists true`
   *  `!doc.contains(k)` => `k $exists false` */
  @testMeth1("$exists") def contains(key: String): BoolCol
}

sealed trait Col extends AnyRef with Dynamic {
  // TODO: add missing update commands: addToSet, pop, pull, pullAll, push, pushAll
  // TODO: add missing geospatial queries.
  @op("$neq") def !=(b: Col): BoolCol
  @op("$lt") def <(b: Col): BoolCol
  @op("$lte") def <=(b: Col): BoolCol
  @op("$gt") def >(b: Col): BoolCol
  @op("$gte") def >=(b: Col): BoolCol
  @op("$all") def all(values: Col*): BoolCol
  @op("$in") def in(values: Col*): BoolCol
  @op("$where") def where(js: String): BoolCol
  @op("$regex") def matches(r: String): BoolCol
  @op("$regex") def matches(r: Regex): BoolCol
  /** `a & b` => `$bit(a) and b` */
  @funcOp("$bit", "and") def &(b: Col): Col
  /** `a | b` => `$bit(a) or b` */
  @funcOp("$bit", "or") def |(b: Col): Col
  /** `a % b == c` => `a $mod (b, c)` */
  @testOp1("$mod") def %(b: Col): Col
  /** `a.size == b` => `a $size b` */
  @testOp0("$size") def size(): Col
  /** `a += b` => `$inc(a -> b)` */
  @opFunc("$minus$greater", "$inc") def +=(b: Col): Col
  /** `a.b.c` => `"a.b.c"` */
  @path def selectDynamic(name: String): Col
  /**  `a.exists` => `a $exists true`
   *  `!a.exists` => `a $exists false` */
  @testOp0("$exists") def exists: BoolCol
  // TODO? def ++(b: Col): Col = ???
}

sealed trait BoolCol extends AnyRef {
  /** `a && b` => `$and(a, b)` */
  @func2("$and") def &&(b: BoolCol): BoolCol
  /** `a || b` => `$or(a, b)` */
  @func2("$or") def ||(b: BoolCol): BoolCol
  def unary_! : BoolCol
}

trait ColImplicits {
  @peel implicit def booleanCasbahDSLColumn(b: Boolean): BoolCol = ???
  @peel implicit def casbahDSLColumn(a: Any): Col = ???
  //@peel implicit def casbahDSLColumnToQuery(a: Col): MongoDBObject = ???
  // @peel implicit def casbahDSLColumn(a: Any): Col = ???
}
