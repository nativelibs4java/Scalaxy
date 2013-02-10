package scalaxy.fx

import scala.language.experimental.macros
import scala.reflect.macros.Context

import javafx.beans._
import javafx.beans.property._
import javafx.beans.value._
import javafx.beans.binding._

// Implementation of macros from GenericTypes.
object BindingMacros
{
  def bindPropertyImpl
      [P : c.WeakTypeTag]
      (c: Context)
      (property: c.Expr[P])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[P] =
    property
  
  def bindExpressionImpl
      [T : c.WeakTypeTag, J : c.WeakTypeTag, B : c.WeakTypeTag, P : c.WeakTypeTag]
      (c: Context)
      (expression: c.Expr[T])
      (ev: c.Expr[GenericType[_, _, _, _]]): c.Expr[B] = 
  {
    import c.universe._
    
    val tpe = weakTypeTag[T].tpe
    val bindingTpe = weakTypeTag[B].tpe
    
    val bindingName = newTermName(c.fresh("binding"))
    
    var observables: List[Tree] = Nil
    (
      new Traverser {
        override def traverse(tree: Tree) = {
          def isObservable(tpe: Type): Boolean =
            tpe <:< typeOf[Observable]
            
          def isStable(sym: Symbol): Boolean = {
            sym.isTerm && sym.asTerm.isStable || 
            sym.isMethod && sym.asMethod.isStable
          }
          
          def handleSelect(sel: Select) {
            def isGetterName(n: String): Boolean =
              n.matches("""(get|is)[\W][\w]*""")
              
            def looksStable(n: String): Boolean = {
              isGetterName(n) ||
              n.matches(".+?Property")
            }
            
            if (isStable(sel.qualifier.symbol)) {
              val n = sel.symbol.name.toString
              if (isObservable(tree.tpe) && (isStable(sel.symbol) || looksStable(n)))
                observables = tree :: observables
              else {
                if (isGetterName(n)) {
                  val propertyGetterName = newTermName(n + "Property")
                  val s = 
                    sel.qualifier.tpe.member(propertyGetterName)
                      .filter(s => s.isMethod && s.asMethod.paramss.flatten.isEmpty)
                  if (s != NoSymbol && isObservable(s.typeSignature))
                    observables = Select(sel.qualifier, propertyGetterName) :: observables
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
                c.error(tree.pos, "Unsupported observable type (" + tree + ": " + tree.getClass.getName + ")")
          }
          super.traverse(tree)
        }
      }
    ).traverse(c.typeCheck(expression.tree))
    
    if (observables.isEmpty)
      c.error(expression.tree.pos, "This expression does not contain any observable property, this is not bindable.")

    val observableIdents: List[Tree] = 
      observables.groupBy(_.symbol).map(_._2.head).toList
    
    c.Expr[B](
      Apply(
        Apply(
          TypeApply(
            Select(Ident(rootMirror.staticPackage("scalaxy.fx")), "newBinding"),
            List(weakTypeTag[T], weakTypeTag[J], weakTypeTag[B], weakTypeTag[P]).map(tt => TypeTree(tt.tpe))
          ),
          (
            expression.tree ::
            observableIdents
          )
        ),
        List(ev.tree)
      )
    )
  }
}
