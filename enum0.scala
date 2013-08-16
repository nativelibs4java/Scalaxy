package scalaxy.enums

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
//import scala.reflect.runtime.universe

class enum(vals: Array[enum#Type]) {
  class Type(name: String, ordinal: Int)
      extends java.lang.Enum[Type](name, ordinal)

  //lazy val values: Array[Type] = ???//computeValues
  def values: Array[Type] =
    vals.map(_.asInstanceOf[Type])

  def valueOf(name: String): Type = ???

  def build: Array[AnyRef] =
    macro internal.introspectValues[Type]

  // object enum {
  //   def values: Array[Type] =
  //     macro internal.introspectValues[Type]
  // }

  object enum {
    // implicit def vals: Array[enum#Type] = macro internal.vals
    def values: enum#Type = macro internal.value[enum#Type]
  }
}

object enum {
  implicit def vals: Array[enum#Type] = macro internal.vals
}

package object internal {
  def vals(c: Context): c.Expr[Array[enum#Type]] = {
    import c.universe._
    
    println("c.enclosingClass = " + c.enclosingClass)
    println("c.enclosingMethod = " + c.enclosingMethod)

    // def isEnumValue(s: Symbol): Boolean = {
    //   s.isTerm && {
    //     val t = s.asTerm
    //     t.isGetter && 
    //     t.accessed.typeSignature <:< typeTpe
    //   }
    // }
    // val names = singletonType.members.toList.collect {
    //   case m if isEnumValue(m) =>
    //     m.name.toString
    // }
    ???
  }
  def value[T: c.WeakTypeTag](c: Context): c.Expr[T] = {
    import c.universe._

    val t = weakTypeTag[T].tpe
    println("value[" + t + "]")
    println("c.enclosingClass = " + c.enclosingClass)
    println("c.enclosingMethod = " + c.enclosingMethod)

    // ???

    val name = "foo"
    val ordinal = 10

    c.Expr[T](
      Apply(
        Select(
          New(
            TypeTree(t)),
          nme.CONSTRUCTOR
        ),
        List(
          Literal(Constant(name)),
          Literal(Constant(ordinal))
        )
      )
    )
  }
  // def introspectValues[T: c.WeakTypeTag](c: Context): c.Expr[Array[T]] = {
  def introspectValues[T: c.WeakTypeTag](c: Context): c.Expr[Array[AnyRef]] = {
    import c.universe._

    val t = weakTypeTag[T].tpe
    // var Select(th @ This(target), name) = c.prefix.tree
    // val singletonType = th.tpe


    println("c.enclosingClass = " + c.enclosingClass)
    println("c.enclosingMethod = " + c.enclosingMethod)

    val singletonType = c.prefix.tree match {
      case Select(th @ This(target), name) =>
        th.tpe
      case tt =>
        tt.tpe
    }
    // val singletonType = th.tpe.tpe


    val enumTypeName = "Type": TypeName

    // val enumType = th.tpe.member(enumTypeName).asType.toType.widen.normalize
    val enumType = singletonType.member(enumTypeName).asType.toType

    val typeTpe = typeOf[scalaxy.enums.enum#Type]
    def isEnumValue(s: Symbol): Boolean = {
      s.isTerm && {
        val t = s.asTerm
        t.isGetter && 
        t.accessed.typeSignature <:< typeTpe
      }
    }
    val names = singletonType.members.toList.collect {
      case m if isEnumValue(m) =>
        m.name.toString
    }
    val ThisType(singletonSymbol) = singletonType
    // println("sym: " + sym + " -> " + sym.asType.toType.member(enumTypeName).fullName)
    println("singletonType: " + singletonType)
    println("singletonSymbol: " + singletonSymbol.asClass.module)
    println("names: " + names.mkString(", "))
    println("enumType = " + enumType)
    println("enumType.seenFrom = " + enumType.asSeenFrom(NoType, NoSymbol))
    // println("enumType.pre = " + typeRef(enumType.asSeenFrom(NoType, NoSymbol))

    val elemType = rootMirror.staticClass(singletonSymbol.fullName + "." + enumTypeName)
    println("singletonSymbol.fullName = " + singletonSymbol.fullName)
    println("elemType = " + elemType.fullName)

    c.Expr[Array[AnyRef]](
      Apply(
        TypeApply(
          Ident(rootMirror.staticModule("scala.Array")),
          //List(TypeTree(enumType))
          List(TypeTree(typeOf[AnyRef]))//enumType))
        ),
        for ((name, ordinal) <- names.zipWithIndex.toList) yield {
          val e = Apply(
            Select(
              New(
                // Select(
                //   Ident(singletonSymbol.asClass.module),
                //   enumTypeName)),
                // Ident(singletonSymbol.fullName + "." + enumTypeName: TypeName)),
                TypeTree(t)),
                // Select(Ident(singletonSymbol.asClass.module), enumTypeName)),
                // TypeTree(enumType)),
                // Select(
                //   This("enum": TypeName),
                //   enumTypeName)),
                // Ident(enumTypeName)),
              nme.CONSTRUCTOR
            ),
            List(
              Literal(Constant(name)),
              Literal(Constant(ordinal))
            )
          )
          println(e)
          //c.typeCheck(e, enumType)
          e
          // Typed(
          //   e,
          //   Select(
          //     Select(
          //       Ident("enum": TypeName),
          //       "this": TermName),
          //     enumTypeName))
        }
      )
    )
  }
}
