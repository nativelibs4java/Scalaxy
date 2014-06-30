package scalaxy.fx
package impl

import javafx.beans._
import javafx.beans.binding._
import javafx.beans.property._
import javafx.beans.value._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

private[fx] object BindingMacros
{
  private lazy val getterRx = """(?:get|is)([\w]+)""".r
  private def decapitalize(s: String) =
    s.substring(0, 1).toLowerCase + s.substring(1)

  /** TODO extract and test `isStable` method. */
  def bindImpl
      [T : c.WeakTypeTag, J : c.WeakTypeTag, B : c.WeakTypeTag, P : c.WeakTypeTag]
      (c: Context)
      (expression: c.Expr[T])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[B] =
  {
    import c.universe._

    val tpe         = weakTypeOf[T]
    val bindingTpe  = weakTypeOf[B]
    val bindingName = TermName(c.freshName("binding"))
    var observables: List[Tree] = Nil

    val observableCollector = new Traverser {
      override def traverse(tree: Tree) = {
        def isObservable(tpe: Type): Boolean =
          tpe <:< typeOf[Observable]

        def isStable(sym: Symbol): Boolean = sym != null && {
          sym.isTerm && sym.asTerm.isStable ||
          sym.isMethod && sym.asMethod.isStable
        }

        def handleSelect(sel: Select) {

          def isGetterName(n: String): Boolean = n match {
            case getterRx(_) => true
            case _ => false
          }
          def looksStable(n: String) = isGetterName(n) || (n endsWith "Property")

          if (isStable(sel.qualifier.symbol)) {
            val n = sel.symbol.name.toString
            if (isObservable(tree.tpe) && (isStable(sel.symbol) || looksStable(n)))
              observables = tree :: observables
            else {
              n match {
                case getterRx(capitalizedFieldName) =>
                  val propertyGetterName = TermName(decapitalize(capitalizedFieldName) + "Property")
                  val s =
                    sel.qualifier.tpe.member(propertyGetterName)
                      .filter(s => s.isMethod && s.asMethod.paramLists.flatten.isEmpty)
                  if (s != NoSymbol && isObservable(s.asMethod.returnType))
                    observables = Select(sel.qualifier, propertyGetterName) :: observables
                case _ =>
              }
            }
          }
        }

        tree match {
          case Ident(_)
          if isObservable(tree.tpe) && isStable(tree.symbol) =>
            observables = tree :: observables
          case sel @ Select(_, _) =>
            handleSelect(sel)
          case Apply(sel @ Select(_, _), Nil) =>
            handleSelect(sel)
          case _ =>
            if (isObservable(tree.tpe))
              c.error(tree.pos, s"Unsupported observable type (is it a val or a stable path from a val?)\n\ttree = $tree, tpe = ${tree.tpe}, sym = ${tree.symbol}, class = ${tree.getClass.getName}")
        }
        super.traverse(tree)
      }
    }
    observableCollector.traverse(c.typecheck(expression.tree))

    if (observables.isEmpty)
      c.error(expression.tree.pos, "This expression does not contain any observable property, this is not bindable.")

    val observableIdents: List[Tree] =
      observables.groupBy(_.symbol).map(_._2.head).toList

    newBinding[T, B](c)(
      expression,
      observableIdents.map(i => c.Expr[Observable](i)): _*
    )
  }

  private def newBinding[T : c.WeakTypeTag, B : c.WeakTypeTag](c: Context)(value: c.Expr[T], observables: c.Expr[Observable]*): c.Expr[B] = {
    import c.universe._
    val T = weakTypeOf[T]

    val BindingType: Type = (
      if (T =:= typeOf[Int]) typeOf[IntegerBinding]
      else if (T =:= typeOf[Long]) typeOf[LongBinding]
      else if (T =:= typeOf[Float]) typeOf[FloatBinding]
      else if (T =:= typeOf[Double]) typeOf[DoubleBinding]
      else if (T =:= typeOf[String]) typeOf[StringBinding]
      else if (T =:= typeOf[Boolean]) typeOf[BooleanBinding]
      else weakTypeOf[ObjectBinding[T]]
    )
    c.Expr[B](c.typecheck(q"new $BindingType { super.bind(..$observables) ; override def computeValue: $T = $value.asInstanceOf[$T] }"))
  }
}
