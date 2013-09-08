package scalaxy.json.base

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.Context
import org.json4s._
// import org.json4s.jackson.JsonMethods._
// import org.json4s.native.JsonMethods._
import scala.collection.JavaConversions._

trait MacrosBase {

  private[json] def buildJSONArray(c: Context)
                            (args: List[c.Expr[JValue]]): c.Expr[JArray] = {
    import c.universe._

    val list = c.Expr[List[JValue]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[JValue](null)).tree
      Apply(TypeApply(Select(target, name), tparams), args.map(_.tree))
    })
    reify(JArray(list.splice))
  }

  private[json] def buildJSONObject(c: Context)(args: List[c.Expr[(String, JValue)]]): c.Expr[JObject] = {
    import c.universe._

    val map = c.Expr[List[(String, JValue)]]({
      val Apply(TypeApply(Select(target, name), tparams), _) = reify(List[(String, JValue)](null)).tree
      Apply(TypeApply(Select(target, name), tparams), args.map(_.tree))
    })
    reify(new JObject(map.splice))
  }
}
