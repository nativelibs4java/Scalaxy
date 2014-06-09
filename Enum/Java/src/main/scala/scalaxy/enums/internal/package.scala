package scalaxy.enums

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.blackbox.Context

package object internal {

  private def getNames(c: Context): List[String] = {
    import c.universe._

    val valueType = typeOf[enum#value]
    def isEnumValue(s: Symbol) = {
      s.isModule &&
        s.asModule.moduleClass.asType.toType <:< valueType ||
      s.isTerm && s.asTerm.isGetter &&
        s.asTerm.accessed.typeSignature.normalize <:< valueType
    }
    // val ModuleDef(_, _, Template(_, _, body)) = 
    //   c.typeCheck(c.enclosingClass, withMacrosDisabled = true)
    // val namesToPos = (body.collect {
    //   case vd @ ValDef(_, name, tpt, _)
    //       if namesSet(name.toString.trim) =>
    //     name.toString.trim -> vd.pos
    // }).toMap

    // println("names = " + names.mkString(", "))
    // println("names to pos = " + namesToPos)
    // println("valueType = " + valueType)
    // println("c.prefix = " + c.prefix)
    // println("c.macroApplication = " + c.macroApplication)
    // println("\tpos = " + c.macroApplication.pos)
    // println("c.enclosingClass = " + c.enclosingClass)
    // println("\tsym = " + c.enclosingClass.symbol)
    // println("c.enclosingMethod = " + c.enclosingMethod)
    // println("c.enclosingPosition = " + c.enclosingPosition)
    // println("c.enclosingImplicits = " + c.enclosingImplicits)

    c.enclosingClass.symbol.typeSignature.members.sorted.toList collect {
      case m if isEnumValue(m) =>
        m.name.toString
    }
  }

  private def newArray(c: Context)(elementType: c.universe.Type, elements: List[c.universe.Tree]): c.universe.Tree = {
    import c.universe._

    val arraySym = rootMirror.staticModule("scala.Array")
    Apply(
      TypeApply(
        Select(
          Ident(arraySym),
          TermName("apply")
        ),
        List(TypeTree(elementType))
      ),
      elements
    )
  }

  private def getModulePath(u: scala.reflect.api.Universe)(moduleSym: u.ModuleSymbol): u.Tree = {
    import u._
    def rec(relements: List[String]): Tree = relements match {
      case name :: Nil =>
        Ident(TermName(name))
      case ("`package`") :: rest =>
        //rec(rest)
        Select(rec(rest), TermName("package"))
      case name :: rest =>
        Select(rec(rest), TermName(name))
    }
    rec(moduleSym.fullName.split("\\.").reverse.toList)
  }

  def enumValueNames[T: c.WeakTypeTag](c: Context): c.Expr[T] = {
    import c.universe._

    val singletonType = c.enclosingClass.symbol.asModule.moduleClass.asType.toType
    // val enumValueType = singletonType.member("EnumValue": TypeName)
    // println("enumValueType = " + enumValueType + " (owner = " + enumValueType.owner + ")")

    val names = getNames(c)
    // println("names = " + names)
    try {
      val res =
        Apply(
          Select(
            New(TypeTree(weakTypeTag[T].tpe)),
            nme.CONSTRUCTOR
          ),
          List(
            newArray(c)(
              typeOf[String],
              names.map(name => Literal(Constant(name)))
            ),
            {
              val paramName = TermName(c.fresh())
              val singletonName = TermName(c.fresh())
              Function(
                List(
                  ValDef(
                    NoMods, 
                    paramName, 
                    TypeTree(typeOf[AnyRef]),
                    EmptyTree
                  )
                ),
                Block(
                  ValDef(
                    NoMods,
                    singletonName,
                    TypeTree(singletonType),
                    TypeApply(
                      Select(
                        Ident(paramName),
                        TermName("asInstanceOf")
                      ),
                      List(
                        TypeTree(singletonType)
                      )
                    )
                  ),
                  newArray(c)(
                    typeOf[AnyRef],
                    names.map(name => {
                      Select(
                        Ident(singletonName),
                        //getModulePath(c.universe)(c.enclosingClass.symbol.asModule),
                        TermName(name)
                      )
                    })
                  )
                )
              )
            }
          )
        )
      // println("Res = " + res)
      c.Expr[T](
        c.typeCheck(res, weakTypeTag[T].tpe)
      )
    } catch { case ex: Throwable =>
      ex.printStackTrace(System.out);
      throw ex
    }
  }

  def enumValueData[T: c.WeakTypeTag](c: Context): c.Expr[T] = {
    import c.universe._

    // reify(enum.this.nextEnumValueData)
    try {
      val res = c.Expr[T](
        // Apply(
        c.typeCheck(
          Ident(TermName("nextEnumValueData")),
          // Select(
          //   Ident(c.enclosingClass.symbol),
          //   // This("enum": TypeName),
          //   "nextEnumValueData": TermName
          // ),
          weakTypeTag[T].tpe
        )
          // Nil
        // )
      )
      // println("res = " + res)
      res
    } catch { case ex: Throwable =>
      ex.printStackTrace(System.out);
      throw ex
    }
  }
}
