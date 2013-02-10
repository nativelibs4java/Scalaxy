package scalaxy

import javafx.beans._
import javafx.beans.value._
import javafx.beans.property._
import javafx.beans.binding._
import javafx.event._
import javafx.scene._

import scala.language.dynamics
import scala.reflect.NameTransformer
import scala.reflect.macros.Context

// TODO: rewrite all calls with macros to avoid dependency to this package object?
package object fx extends GenericTypes
{
  // This adds `obj.set(property1 = value1, property2 = value2)` to all object types.
  // Properties of type EventHandler[_] benefit from a special type-check to accommodate 
  // implicit conversions below.
  implicit def fxBeansExtensions[T](bean: T) = new {
    def set = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): T =
        macro internal.applyDynamicNamedImpl[T]
    }
  }
  
  def bind
      [T, J, B <: Binding[J], P <: Property[J]]
      (expression: T)
      (implicit ev: GenericType[T, J, B, P]): B =
    macro internal.bindImpl[T, B]
    
  def bound(expression: Any): GenericBinding[Any, Any] = ???
  
  // Implicit conversion from an event handler function to a JavaFX EventHandler[_].
  implicit def handler[E <: Event](f: E => Unit): EventHandler[E] =
    new FunctionEventHandler(f)
  
  // Implicit conversion from an event handler block to a JavaFX EventHandler[_].
  implicit def handler[E <: Event](block: => Unit): EventHandler[E] =
    new FunctionEventHandler(_ => block)
    
  implicit def doubleProperty(p: DoubleProperty) = 
    macro internal.doublePropertyImpl
    
  implicit def integerProperty(p: IntegerProperty) = 
    macro internal.integerPropertyImpl
    
  implicit def longProperty(p: LongProperty) = 
    macro internal.longPropertyImpl
    
  implicit def floatProperty(p: FloatProperty) = 
    macro internal.floatPropertyImpl
    
  implicit def booleanProperty(p: BooleanProperty) = 
    macro internal.booleanPropertyImpl
}

package fx 
{
  package object internal
  {// This needs to be public and statically accessible.
    def applyDynamicNamedImpl[T : c.WeakTypeTag]
      (c: Context)
      (name: c.Expr[String])
      (args: c.Expr[(String, Any)]*) : c.Expr[T] =
    {
      import c.universe._
  
      //println(s"applyDynamicNamedImpl($name)($args)")
            
      // Check that the method name is "create".
      name.tree match {
        case Literal(Constant(n)) =>
          if (n != "apply")
            c.error(name.tree.pos, s"Expected 'apply', got '$n'")
      }
  
      // Get the bean.
      val Select(Apply(_, List(bean)), _) = c.prefix.tree//c.typeCheck(c.prefix.tree)
  
      // Choose a non-existing name for our bean's val.
      val beanName = newTermName(c.fresh("bean"))
  
      // Create a declaration for our bean's val.
      val beanDef = ValDef(NoMods, beanName, TypeTree(bean.tpe), bean)
  
      // Try to find a setter in the bean type that can take values of the type we've got.
      def getSetter(name: String) = {
        bean.tpe.member(newTermName(name))
          .filter(s => s.isMethod && s.asMethod.paramss.flatten.size == 1)
      }

      // Try to find a setter in the bean type that can take values of the type we've got.
      def getVarTypeFromSetter(s: Symbol) = {
        val Seq(param) = s.asMethod.paramss.flatten
        param.typeSignature
      }

      val values = args.map(_.tree).map {
        // Match Tuple2.apply[String, Any](fieldName, value).
        case Apply(_, List(Literal(Constant(fieldName: String)), value)) =>
          (fieldName, value)
      }
  
      // Forbid duplicates.
      for ((fieldName, dupes) <- values.groupBy(_._1); if dupes.size > 1) {
        for ((_, value) <- dupes.drop(1))
          c.error(value.pos, s"Duplicate value for property '$fieldName'")
      }
      
      // Generate one setter call per argument.
      val setterCalls = values map 
      {
        case (fieldName, value) =>
        
          val valueTpe = value.tpe.normalize//.widen
          
          // Check that all parameters are named.
          if (fieldName == null || fieldName == "")
            c.error(value.pos, "Please use named parameters.")
  
          //println(s"fieldName = $fieldName, valueTpe.typeSymbol = ${valueTpe.typeSymbol}; value = $value")
          if (valueTpe <:< typeOf[GenericBinding[Any, Any]]) {//.typeSymbol == typeOf[GenericBinding[_, _]]) {
            println("FOUND A GENERIC BINDING VALUE")
            val actualValue = value match {
              case Apply(Select(_, n), List(v)) if n.toString == "bound" =>
                v
            }
            
            val propertyName = newTermName(fieldName + "Property")
            val propertySymbol = 
              bean.tpe.member(propertyName)
                .filter(s => s.isMethod && s.asMethod.paramss.flatten.isEmpty)
                
            if (propertySymbol == NoSymbol) {
              c.error(value.pos, s"Couldn't find a property getter $propertyName for property '$fieldName' in type ${bean.tpe}")
            }
            
            Apply(
              Select(
                Select(Ident(beanName), propertyName), 
                "bind"
              ), 
              Nil//bindExpr(c)(actualValue) // TODO
            )
          } else {
            // Get beans-style setter or Scala-style var setter.
            val setterSymbol =
              getSetter("set" + fieldName.capitalize)
                .orElse(getSetter(fieldName))
                  .orElse(getSetter(NameTransformer.encode(fieldName + "_=")))
    
            if (setterSymbol == NoSymbol) {
              c.error(value.pos, s"Couldn't find a setter for property '$fieldName' in type ${bean.tpe}")
            }
            val varTpe = getVarTypeFromSetter(setterSymbol)
            // Implicits will convert functions and blocks to EventHandler.
            if (!(valueTpe weak_<:< varTpe) && !(varTpe <:< typeOf[EventHandler[_]]))
              c.error(value.pos, s"Setter ${bean.tpe}.${setterSymbol.name}($varTpe) does not accept values of type ${valueTpe}")
  
            Apply(Select(Ident(beanName), setterSymbol), List(value))
          }
      }
      // Build a block with the bean declaration, the setter calls and return the bean.
      c.Expr[T](Block(Seq(beanDef) ++ setterCalls :+ Ident(beanName): _*))
    }
    
    def bindImpl
        [T : c.WeakTypeTag, B : c.WeakTypeTag]
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
          Select(ev.tree, "newBinding"),
          reify(() => expression.splice).tree :: observableIdents
        )
      )
    }
    
    def doublePropertyImpl(c: Context)(p: c.Expr[DoubleProperty]): c.Expr[Double] =
      c.universe.reify(p.splice.get)
    
    def floatPropertyImpl(c: Context)(p: c.Expr[FloatProperty]): c.Expr[Float] =
      c.universe.reify(p.splice.get)
    
    def integerPropertyImpl(c: Context)(p: c.Expr[IntegerProperty]): c.Expr[Int] =
      c.universe.reify(p.splice.get)
    
    def longPropertyImpl(c: Context)(p: c.Expr[LongProperty]): c.Expr[Long] =
      c.universe.reify(p.splice.get)
    
    def booleanPropertyImpl(c: Context)(p: c.Expr[BooleanProperty]): c.Expr[Boolean] =
      c.universe.reify(p.splice.get)
      
  }
}
