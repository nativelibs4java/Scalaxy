package scalaxy.js

import scala.collection.JavaConversions._
import scala.reflect.api.Universe
import com.google.javascript.jscomp._
import com.google.javascript.rhino._
import com.google.javascript.rhino.jstype._

object TreeGenerator {
  private val qualNameRx = """(.*?)\.([^.]+)""".r
  def generateClass(u: Universe)(classVars: ClassVars, externs: ClosureExterns): List[u.Tree] = {

    import u._
    import externs._
    import TypeConversions.convertTypeRef

    val (fullClassName, packageName, simpleClassName) = classVars.className match {
      case fullClassName @ qualNameRx(packageName, simpleClassName) =>
        (fullClassName, packageName, simpleClassName)
      case className =>
        (className, null, className)
    }


    def getParams(doc: JSDocInfo): List[ValDef] = {
      for (paramName <- doc.getParameterNames.toList) yield {
        val paramType: JSType = doc.getParameterType(paramName)
        val convType = convertTypeRef(u)(paramType).asInstanceOf[u.Tree]
        ValDef(NoMods, paramName: TermName, convType, EmptyTree)
      }
    }

    def convertMember(memberVar: Scope.Var): Tree = {
      val memberName: TermName = memberVar.getName.split("\\.").last
      memberVar.getType match {
        case ft: FunctionType =>
          //memberDoc
          val retType = convertTypeRef(u)(ft.getReturnType)
          val vparams = Option(memberVar.getJSDocInfo) match {
            case None if ft.getParameters.isEmpty =>
              Nil
            case None =>
              // println("PARAMS(" + memberVar.getName + "): " + ft.getParameters.toList.map(p => p + ": " + p.getJSType).mkString(", "))
              println("WARNING: " + memberVar.getName + " has no JSDoc")
              ft.getParameters.toList.map(p => {
                ValDef(
                  NoMods,
                  p.getString: TermName,
                  convertTypeRef(u)(p.getJSType).asInstanceOf[u.Tree],
                  EmptyTree)
              })
            case Some(memberDoc) =>
              getParams(memberDoc)
          }
          if (vparams.isEmpty)
            q"def $memberName(): $retType = ???"
          else
            q"def $memberName(..$vparams): $retType = ???"
        case t =>
          val valType = Option(t).map(convertTypeRef(u)(_)).getOrElse(TypeTree(typeOf[Any]))
          q"var $memberName: $valType = _"
      }
    }

    val constructorDoc = classVars.constructor.flatMap(c => Option(c.getJSDocInfo))

    val className = simpleClassName: TypeName
    val companionName = simpleClassName: TermName

    val protoMembers = classVars.protoMembers.map(convertMember(_))
    val staticMembers = classVars.staticMembers.filter(!_.getName.endsWith(".prototype")).map(convertMember(_))

    val companion =
      if (staticMembers.isEmpty)
        Nil
      else
        //def apply(..${getParams(classDoc)}): $className = ???
        q"""
          @scalaxy.js.global
          object $companionName {
            ..$staticMembers
          }
        """ :: Nil

    constructorDoc match {
      case None =>
        q"""
          @scalaxy.js.global
          trait $className {
            ..$protoMembers
          }
        """ :: companion
      case Some(classDoc) =>
        val parents = {
          val interfaces = 
            (classDoc.getExtendedInterfaces.toList ++ classDoc.getImplementedInterfaces.toList)
            .toSet.toList.map((t: JSTypeExpression) =>  convertTypeRef(u)(t: JSType))
          if (interfaces.isEmpty)
            List(TypeTree(typeOf[AnyRef]))
          else
            interfaces
        }
        q"""
          @scalaxy.js.global
          class $className(..${getParams(classDoc)})
              extends ..$parents {
            ..$protoMembers
          }
        """ :: companion
    }
  }
  def generateGlobal(u: Universe)(variable: Scope.Var): List[u.Tree] = {
    Nil
  }
}
