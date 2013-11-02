package scalaxy

import scala.language.experimental.macros

import com.mongodb.casbah.Imports.MongoDBObject
import scalaxy.casbah.internal._

package object casbah extends ColImplicits {
  def query(f: Doc => BoolCol): MongoDBObject = macro internal.queryImpl[BoolCol]
  def update(f: Doc => Col): MongoDBObject = macro internal.queryImpl[Col]
}
