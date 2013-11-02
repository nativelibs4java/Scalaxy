package scalaxy.casbah.internal

import scala.language.dynamics
import scala.language.implicitConversions
import scala.util.matching.Regex

sealed trait Doc extends Dynamic {

  object set extends Dynamic {
    @applyDynNamedFunc("$set")
    def applyDynamicNamed(n: String)(args: (String, Col)*): Col = ???
  }
  object rename extends Dynamic {
    @applyDynNamedFunc("$rename")
    def applyDynamicNamed(n: String)(args: (String, String)*): Col = ???
  }
  object setOnInsert extends Dynamic {
    @applyDynNamedFunc("$setOnInsert")
    def applyDynamicNamed(n: String)(args: (String, Col)*): Col = ???
  }

  @peel def selectDynamic(name: String): Col = ???
  @updateDynFunc("$set") def updateDynamic(name: String)(value: Col): Col = ???
  @func1("$unset") def -=(n: String): Col = ???
}

sealed trait Col extends AnyRef {
  // TODO: add missing update commands: addToSet, pop, pull, pullAll, push, pushAll
  // TODO: add missing geospatial queries.
  @op("$neq") def !=(b: Col): BoolCol = ???
  @op("$lt") def <(b: Col): BoolCol = ???
  @op("$lte") def <=(b: Col): BoolCol = ???
  @op("$gt") def >(b: Col): BoolCol = ???
  @op("$gte") def >=(b: Col): BoolCol = ???
  @op("$all") def all(values: Col*): BoolCol = ???
  @op("$in") def in(values: Col*): BoolCol = ???
  @op("$where") def where(js: String): BoolCol = ???
  @op("$regex") def matches(js: String): BoolCol = ???
  @funcOp("$bit", "and") def &(b: Col): Col = ???
  @funcOp("$bit", "or") def |(b: Col): Col = ???
  @testOp1("$mod") def %(b: Col): Col = ???
  @testOp0("$size") def size(): Col = ???
  @opFunc("$minus$greater", "$inc") def +=(b: Col): Col = ???
}

sealed trait BoolCol extends AnyRef {
  @func2("$and") def &&(b: BoolCol): BoolCol = ???
  @func2("$or") def ||(b: BoolCol): BoolCol = ???
}

trait ColImplicits {
  @peel implicit def booleanCasbahDSLColumn(b: Boolean): BoolCol = ???
  @peel implicit def casbahDSLColumn(a: Any): Col = ???
}
