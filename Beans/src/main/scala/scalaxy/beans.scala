package scalaxy

import scala.language.dynamics
import scala.reflect.ClassTag
import scala.reflect.NameTransformer
import scala.reflect.macros.Context

/**
  Syntactic sugar to instantiate Java beans with a very Scala-friendly syntax. 

  The following expression:
  
    import scalaxy.beans
    
    beans.create[MyBean](
      foo = 10, 
      bar = 12
    )
    
  Gets replaced (and type-checked) at compile time by:
  
    {
      val bean = new MyBean
      bean.setFoo(10)
      bean.setBar(12)
      bean
    }
    
  Doesn't bring any runtime dependency (macro is self-erasing).
  Don't expect code completion from your IDE as of yet.
*/
object beans extends Dynamic 
{
  def applyDynamicNamedImpl[R : c.WeakTypeTag]
    (c: Context)
    (name: c.Expr[String])(args: c.Expr[(String, Any)]*) : c.Expr[R] = 
  {
    import c.universe._
    
    name.tree match {
      case Literal(Constant(n)) =>
        if (n != "create")
          c.error(name.tree.pos, s"Expected 'create', got '$n'")
      case _ =>
        c.error(name.tree.pos, "Unexpected name structure error")
    }
    
    val beanTpe = weakTypeTag[R].tpe
    
    val beanName = 
      newTermName(c.fresh("bean"))
      
    val beanDef = 
      ValDef(Modifiers(), beanName, TypeTree(beanTpe), New(TypeTree(beanTpe), Nil))

    def getSetter(name: String, valueTpe: Type, valuePos: Position) = {
      beanTpe.member(newTermName(name)).filter { 
        case s => 
          s.isMethod && (
            s.asMethod.paramss.flatten match {
              case Seq(param) =>
                if (!(valueTpe weak_<:< param.typeSignature))
                  c.error(valuePos, s"Value of type $valueTpe cannot be set with $beanTpe.$name(${param.typeSignature})")
                true
              case _ =>
                false
            }
          )
      }
    }

    val setterCalls = args.map(_.tree).map { 
      case Apply(_, List(n @ Literal(Constant(fieldName: String)), v @ value)) =>
        if (fieldName == null || fieldName == "")
          c.error(v.pos, "Please use named parameters.")
        val setterSymbol = 
          getSetter("set" + fieldName.capitalize, value.tpe, v.pos)
            .orElse(getSetter(NameTransformer.encode(fieldName + "_="), value.tpe, v.pos))

        if (setterSymbol == NoSymbol)
          c.error(n.pos, s"Couldn't find a setter for field '$fieldName' in type $beanTpe")

        Apply(Select(Ident(beanName), setterSymbol), List(value)) 
    }
    c.Expr[R](Block(Seq(beanDef) ++ setterCalls :+ Ident(beanName): _*))
  }
  
  def applyDynamicNamed[R](name: String)(args: (String, Any)*): R =
    macro applyDynamicNamedImpl[R]
}
