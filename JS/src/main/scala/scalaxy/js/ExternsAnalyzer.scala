/*
echo "window.console.log('yay');" | java -jar build/compiler.jar --warning_level VERBOSE --jscomp_error checkTypes
*/
package scalaxy.js

import scala.collection.mutable
import scala.collection.JavaConversions._
import com.google.javascript.rhino.jstype.FunctionType

import com.google.javascript.rhino.Node
import com.google.javascript.jscomp._

case class ClassVars(
  className: String,
  constructor: Option[Scope.Var],
  staticMembers: List[Scope.Var],
  protoMembers: List[Scope.Var])

case class GlobalVars(
  classes: List[ClassVars],
  globalVars: List[Scope.Var])

object ExternsAnalyzer {

  def analyze(externs: ClosureExterns): GlobalVars = {
    import externs._

    class MembersMap extends mutable.HashMap[String, mutable.ArrayBuffer[Scope.Var]]() {
      override def default(className: String) = {
        val members = mutable.ArrayBuffer[Scope.Var]()
        this(className) = members
        members
      }
    }
    val constructors = mutable.Map[String, Scope.Var]()
    val globalVars = mutable.Map[String, Scope.Var]()
    val staticMembers = new MembersMap
    val protoMembers = new MembersMap

    var memberRx = """(.*?)\.prototype\.([^.]+)""".r
    var staticMemberRx = """(.*?)\.([^.]+)""".r

    for (variable <- scope.getVars) {
      variable.getName match {
        case memberRx(className, memberName) =>
          if (!constructors.contains(className)) {
            println("Unknown class: " + className)
            // constructors(className) = new Scope.Var()
          }
          protoMembers(className) += variable
        case staticMemberRx(className, memberName) if constructors.contains(className) =>
          staticMembers(className) += variable
        case n =>
          variable.getType match {
            case t: FunctionType if t.isConstructor =>
              // println("Constructor for " + n + ": type = " + variable.getType + Option(variable.getType).map(": " + _.getClass.getName).getOrElse(""))
              constructors(n) = variable
            case _ =>
              globalVars(n) = variable
          }
      }
    }

    val classNames = constructors.keys ++ protoMembers.keys

    GlobalVars(
      for (className <- classNames.toList.sorted) yield {
        ClassVars(
          className = className,
          constructor = constructors.get(className),
          staticMembers = if (staticMembers.contains(className)) staticMembers(className).toList else Nil,
          protoMembers = if (protoMembers.contains(className)) protoMembers(className).toList else Nil)
      },
      globalVars.values.toList)
  }
}
