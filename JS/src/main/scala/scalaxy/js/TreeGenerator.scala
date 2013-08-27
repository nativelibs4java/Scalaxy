package scalaxy.js

import scala.collection.JavaConversions._
import scala.reflect.api.Universe
import com.google.javascript.jscomp._
import com.google.javascript.rhino._
import com.google.javascript.rhino.jstype._

object TreeGenerator {
  private val qualNameRx = """(.*?)\.([^.]+)""".r
  def generateClass(u: Universe)(classVars: ClassVars, externs: ClosureExterns, owner: u.Name): List[u.Tree] = {

    import u._
    import externs._
    import TypeConversions.convertTypeRef

    val (fullClassName, packageName, simpleClassName) = classVars.className match {
      case fullClassName @ qualNameRx(packageName, simpleClassName) =>
        (fullClassName, packageName, simpleClassName)
      case className =>
        (className, null, className)
    }

    val resolver = (simpleName: Name) => {
      Select(Ident(owner), simpleName: Name)
    }

    def conv(t: JSType, templateTypeNames: Set[String]): Tree = {
      if (templateTypeNames(t.toString))
        TypeTree(typeOf[Any])
      else {
        println("NOT A TEMPLATE = " + t)
        convertTypeRef(u)(t, resolver).asInstanceOf[u.Tree]
      }
    }
    def getParams(doc: JSDocInfo, templateTypeNames: Set[String], rename: Boolean = false): List[ValDef] = {
      for ((paramName, i) <- doc.getParameterNames.toList.zipWithIndex) yield {
        val paramType: JSType = doc.getParameterType(paramName)
        val convType = conv(paramType, templateTypeNames)
        assert(paramName.trim != "")
        ValDef(
          NoMods,
          (if (rename) paramName + "$" else if (paramName.trim.isEmpty) "param" + i else paramName): TermName,
          convType,
          EmptyTree
        )
      }
    }

    def convertMember(memberVar: Scope.Var): Tree = {
      val templateTypeNames = Option(memberVar.getJSDocInfo).map(_.getTemplateTypeNames().toSet).getOrElse(Set())
      if (!templateTypeNames.isEmpty)
        EmptyTree
      else {
        val memberName: TermName = memberVar.getName.split("\\.").last
        assert(memberName.toString.trim != "")
        memberVar.getType match {
          case ft: FunctionType =>
            //memberDoc
            val retType = conv(ft.getReturnType, templateTypeNames)
            val vparams = Option(memberVar.getJSDocInfo) match {
              case None if ft.getParameters.isEmpty =>
                Nil
              case None =>
                // println("PARAMS(" + memberVar.getName + "): " + ft.getParameters.toList.map(p => p + ": " + p.getJSType).mkString(", "))
                println("WARNING: " + memberVar.getName + " has no JSDoc")
                for ((p, i) <- ft.getParameters.toList.zipWithIndex) yield {
                  val name = p.getString
                  ValDef(
                    NoMods,
                    (if (name.trim.isEmpty) "param" + i else name): TermName,
                    conv(p.getJSType, templateTypeNames),
                    EmptyTree
                  )
                }
              case Some(memberDoc) =>
                getParams(memberDoc, templateTypeNames)
            }
            if (vparams.isEmpty)
              q"def $memberName(): $retType = ???"
            else
              q"def $memberName(..$vparams): $retType = ???"
          case t =>
            val valType = Option(t).map(conv(_, templateTypeNames)).getOrElse(TypeTree(typeOf[Any]))
            q"var $memberName: $valType = _"
        }
      }
    }

    val constructorDoc = classVars.constructor.flatMap(c => Option(c.getJSDocInfo))

    val className = simpleClassName: TypeName
    val companionName = simpleClassName: TermName

    val protoMembers = classVars.protoMembers.map(convertMember(_))
    val staticMembers = classVars.staticMembers.filter(!_.getName.endsWith(".prototype")).map(convertMember(_))

    /*
      Quasiquotes generate a constructor like this:

        def <init>() = {
          super.<init>;
          ()
        };

      This transformer will simply add the () to get super.<init>(); 
    */
    val fixer = new Transformer {
      override def transform(tree: Tree) = tree match {
        case Block(List(Select(target, nme.CONSTRUCTOR)), value) =>
          Block(List(Apply(Select(target, nme.CONSTRUCTOR), Nil)), value)
        case _ =>
          super.transform(tree)
      }
    }
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

    lazy val traitTree =
      q"""
        @scalaxy.js.global
        trait $className {
          ..$protoMembers
        }
      """

    val result = constructorDoc match {
      case None =>
        traitTree :: companion
      case _ if classVars.constructor.get.getType.isInterface =>
        traitTree :: companion
      case Some(classDoc) =>
        // val selfType: JSType = classDoc.getType
        // println("CLASS(" + className + ") selfType = " + selfType + ", thisType = " + classDoc.getThisType + ", const.type = " + classVars.constructor.get.getType)
        // if (!(selfType.isInstanceOf[ObjectType] && selfType.asInstanceOf[ObjectType].getTemplateTypes.isEmpty))
        //   Nil
        // else {
        val templateTypeNames = classDoc.getTemplateTypeNames().toSet

        val parents = {
          val interfaces = 
            (classDoc.getExtendedInterfaces.toList ++ classDoc.getImplementedInterfaces.toList)
            .toSet.toList.map((t: JSTypeExpression) =>  convertTypeRef(u)(t: JSType, resolver))
          if (interfaces.isEmpty)
            List(TypeTree(typeOf[AnyRef]))
          else
            interfaces
        }
        val classDef = q"""
          @scalaxy.js.global
          class $className(..${getParams(classDoc, templateTypeNames, true)})
              extends ..$parents {
            ..$protoMembers
          }
        """
        fixer.transform(classDef) :: companion
      // }
    }
    result.map(fixer.transform(_))
  }
  def generateGlobal(u: Universe)(variable: Scope.Var): List[u.Tree] = {
    Nil
  }
}
