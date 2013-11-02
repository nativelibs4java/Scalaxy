package scalaxy

import scala.language.implicitConversions
import scala.language.experimental.macros

import com.mongodb.casbah.Imports.MongoDBObject
import com.mongodb.casbah.query.dsl._
import scalaxy.casbah.internal._

package object casbah extends ColImplicits {
  /** Search query. */
  implicit def query(f: Doc => BoolCol): MongoDBObject = macro internal.queryImpl[BoolCol]
  /** Update query. */
  def update(f: Doc => Col): MongoDBObject = macro internal.queryImpl[Col]
}
