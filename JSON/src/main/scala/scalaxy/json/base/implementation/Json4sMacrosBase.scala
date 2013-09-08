package scalaxy.json.base

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._
// import org.json4s.jackson.JsonMethods._
// import org.json4s.native.JsonMethods._
import scala.collection.JavaConversions._

trait Json4sMacrosBase extends MacrosBase {
  type JSONValue = JValue
  type JSONArray = JArray
  type JSONObject = JObject

  private[json] def newJSONArray(c: Context)(values: c.Expr[List[JSONValue]]): c.Expr[JSONArray] = {
    import c.universe._
    reify(JArray(values.splice))
  }

  private[json] def newJSONObject(c: Context)(values: c.Expr[List[(String, JSONValue)]]): c.Expr[JSONObject] = {
    import c.universe._
    reify(JObject(values.splice))
  }
}
