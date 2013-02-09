package scalaxy

import scala.language.dynamics
import scala.reflect.ClassTag
import scala.reflect.NameTransformer
import scala.reflect.macros.Context

/**
  Syntactic sugar to instantiate Java beans with a very Scala-friendly syntax.

  The following expression:

    import scalaxy.beans

    new MyBean().set(
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
package object beans
{
  implicit def beansExtensions[T](bean: T) = new {
    def set = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): T =
        macro internal.applyDynamicNamedImpl[T]
    }
  }
}

package beans {
  package internal {
    trait BeansMacros {
      def rewriteNamedSetBeanApply[T : c.WeakTypeTag]
        (c: Context)
        (name: c.Expr[String])
        (args: c.Expr[(String, Any)]*) : c.Expr[T] =
      {
        import c.universe._
    
        // Check that the method name is "create".
        name.tree match {
          case Literal(Constant(n)) =>
            if (n != "apply")
              c.error(name.tree.pos, s"Expected 'apply', got '$n'")
        }
    
        // Get the bean.
        val Select(Apply(_, List(bean)), _) = c.typeCheck(c.prefix.tree)
    
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
          
            // Check that all parameters are named.
            if (fieldName == null || fieldName == "")
              c.error(value.pos, "Please use named parameters.")
    
            // Get beans-style setter or Scala-style var setter.
            val setterSymbol =
              getSetter("set" + fieldName.capitalize)
                .orElse(getSetter(NameTransformer.encode(fieldName + "_=")))
    
            if (setterSymbol == NoSymbol)
              c.error(value.pos, s"Couldn't find a setter for property '$fieldName' in type ${bean.tpe}")
    
            val varTpe = getVarTypeFromSetter(setterSymbol)
            Apply(
              Select(
                Ident(beanName), 
                setterSymbol
              ), 
              List(
                transformValue(c)(fieldName, bean.tpe, varTpe, setterSymbol, value)
              )
            )
        }
        // Build a block with the bean declaration, the setter calls and return the bean.
        val res =
          c.Expr[T](Block(Seq(beanDef) ++ setterCalls :+ Ident(beanName): _*))
          
        println(s"res = $res")
        res
      }
      
      // Override this to provide looser type-checks or value transforms.
      def transformValue
        (c: Context)
        (fieldName: String, beanTpe: c.universe.Type, varTpe: c.universe.Type, setterSymbol: c.universe.Symbol, value: c.universe.Tree): c.universe.Tree = 
      {
        import c.universe._
        
        if (!(value.tpe weak_<:< varTpe))
          c.error(value.pos, s"Setter $beanTpe.${setterSymbol.name}($varTpe) does not accept values of type ${value.tpe}")
          
        value
      }
    }
  }
  
  package object internal extends BeansMacros
  {
    // This needs to be public and statically accessible.
    def applyDynamicNamedImpl[T : c.WeakTypeTag]
      (c: Context)
      (name: c.Expr[String])
      (args: c.Expr[(String, Any)]*) : c.Expr[T] =
    {
      rewriteNamedSetBeanApply[T](c)(name)(args: _*)
    }
  }
}
