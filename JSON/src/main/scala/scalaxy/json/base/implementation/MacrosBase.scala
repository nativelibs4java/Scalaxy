package scalaxy.json.base

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._
// import org.json4s.jackson.JsonMethods._
// import org.json4s.native.JsonMethods._
import scala.collection.JavaConversions._

trait MacrosBase {
  // type JSONValue = JValue
  // type JSONArray = JArray
  // type JSONObject = JObject

  // private[json] newJSONArray(c: Context)(values: c.Expr[List[JSONValue]]): c.Expr[JSONArray]

  // private[json] newJSONObject(c: Context)(values: c.Expr[List[(String, JSONValue)]]): c.Expr[JSONObject]

  private[json] def buildJSONArray(c: Context)(args: List[c.Expr[JValue]]): c.Expr[JArray] = {
    import c.universe._

    val list = c.Expr[List[JValue]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[JValue](null)).tree
      Apply(TypeApply(Select(target, name), tparams), args.map(_.tree))
    })
    //newJSONArray(list)
    reify(JArray(list.splice))
  }

  private[json] def buildJSONObject(c: Context)(args: List[c.Expr[(String, JValue)]]): c.Expr[JObject] = {
    import c.universe._

    val map = c.Expr[List[(String, JValue)]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[(String, JValue)](null)).tree
      Apply(TypeApply(Select(target, name), tparams), args.map(_.tree))
    })
    // newJSONObject(map)
    reify(JObject(map.splice))
  }

  private[json] def reifyByteArray(c: Context)(v: Array[Byte]): c.Expr[Array[Byte]] = {
    import c.universe._

    val Apply(Apply(TypeApply(target, tparams), _), impls) = reify(Array[Byte](0: Byte)).tree
    c.Expr[Array[Byte]](
      Apply(Apply(TypeApply(target, tparams), v.toList.map(c.literal(_).tree)), impls)
    )
  }
}
