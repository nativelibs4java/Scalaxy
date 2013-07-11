package scalaxy.reified

import scalaxy.reified.impl.Utils.{ toolbox, newExpr }

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.definitions._
import scala.reflect.runtime.currentMirror
import scala.collection.immutable

/**
 * Conversions for captured references of common types. 
 */
object CaptureConversions {
  
  type Conversion = PartialFunction[(Any, Type, Any/*full Conversion*/), Tree]
  
  final lazy val DEFAULT: Conversion = {
    CONSTANT orElse 
    REIFIED_VALUE orElse 
    //ARRAY orElse 
    IMMUTABLE_COLLECTION
  }
  
  final lazy val CONSTANT: Conversion = {
    case (value @ (
        (_: Number) | 
        (_: java.lang.Boolean) | 
        (_: String) | 
        (_: java.lang.Character)), tpe: Type, conversion: Conversion) =>
      Literal(Constant(value))
  }
  
  final lazy val REIFIED_VALUE: Conversion = {
    case (value: ReifiedValue[_], tpe: Type, conversion: Conversion) =>
      value.expr(conversion).tree.duplicate
  }
  
  private def collectionApply(
      syms: (ModuleSymbol, TermSymbol), 
      col: Iterable[_], 
      tpe: Type, 
      conversion: Conversion, 
      requiresClassTag: Boolean = false): Tree = {

    val (moduleSym, methodSym) = syms
    val (elementType, castToAnyRef) = tpe match {
      case TypeRef(_, _, elementType :: _) 
          if tpe <:< typeOf[Traversable[_]] =>
        //println(s"GOT ELEMENT TYPE $elementType")
        elementType -> false
      case _ =>
        typeOf[AnyRef] -> true
    }
    
    def getModulePath(moduleSym: ModuleSymbol): Tree = {
      val elements = moduleSym.fullName.split("\\.").toList
      def rec(root: Tree, sub: List[String]): Tree = sub match {
        case Nil => root
        case name :: rest => rec(Select(root, name: TermName), rest)
      }
      rec(Ident(elements.head: TermName), elements.tail)
    }
    val apply = 
      Apply(
        TypeApply(
          Select(
            getModulePath(moduleSym), //Ident(moduleSym),
            methodSym),
          List(TypeTree(elementType))),
        col.map(value => {
          val convertedValue = conversion((value, elementType, conversion))
          if (castToAnyRef) {
            val convertedValueExpr = newExpr[Any](convertedValue)  
            universe.reify(
              convertedValueExpr.splice.asInstanceOf[AnyRef]
            ).tree
          } else {
            convertedValue
          } 
        }).toList
      )
    val res = 
      if (requiresClassTag)
        Apply(
          apply,//toolbox.resetAllAttrs(apply),
          //List(reify(implicitly[reflect.ClassTag[AnyRef]](reflect.ClassTag.AnyRef)).tree))//
          List(
            toolbox.inferImplicitValue(
              typeRef(
                typeOf[reflect.ClassTag[_]], 
                currentMirror.staticClass("scala.reflect.ClassTag"), 
                List(elementType)))))
      else
        apply
      
    //println(s"res = $res")
    res
  }
  
  final lazy val ARRAY: Conversion = {
    lazy val Array_syms = (ArrayModule, ArrayModule_overloadedApply)
    
    {
      case (array: Array[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Array_syms, array, tpe, conversion, requiresClassTag = true)
    }
  }
  
  final lazy val IMMUTABLE_COLLECTION: Conversion = {
    def symsOf(name: String) = {
      val moduleSym = currentMirror.staticModule("scala.collection.immutable." + name)
      val methodSym = moduleSym.moduleClass.typeSignature.member("apply": TermName).asTerm
      (moduleSym, methodSym)
    }
    lazy val Set_syms = symsOf("Set")
    lazy val List_syms = symsOf("List")
    lazy val Vector_syms = symsOf("Vector")
    lazy val Stack_syms = symsOf("Stack")
    lazy val Seq_syms = symsOf("Seq")
      
    {
      case (col: immutable.Set[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Set_syms, col, tpe, conversion)
      case (col: immutable.List[_], tpe: Type, conversion: Conversion) =>
        collectionApply(List_syms, col, tpe, conversion)
      case (col: immutable.Vector[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Vector_syms, col, tpe, conversion)
      case (col: immutable.Stack[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Stack_syms, col, tpe, conversion)
      case (col: immutable.Seq[_], tpe: Type, conversion: Conversion) =>
        collectionApply(Seq_syms, col, tpe, conversion)
      // TODO: Map
      //case (map: immutable.Map[_], tpe: Type, conversion: Conversion) =>      
    }
  }
}
