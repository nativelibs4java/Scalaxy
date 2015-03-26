package scalaxy

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.reflect.api.Universe
import scala.reflect.macros.blackbox
import scala.reflect.macros.whitebox
import scala.collection.breakOut

package object flatarray {
  type Fibers = Array[Array[_]]
 
  implicit lazy val IntFlatIO = ScalarFlatIO[Int]()
  implicit lazy val ShortFlatIO = ScalarFlatIO[Short]()
  implicit lazy val LongFlatIO = ScalarFlatIO[Long]()
  implicit lazy val ByteFlatIO = ScalarFlatIO[Byte]()
  implicit lazy val BooleanFlatIO = ScalarFlatIO[Boolean]()
  implicit lazy val FloatFlatIO = ScalarFlatIO[Float]()
  implicit lazy val DoubleFlatIO = ScalarFlatIO[Double]()
  implicit lazy val CharFlatIO = ScalarFlatIO[Char]()
  
  def getMembers(u: Universe)(tpe: u.Type): List[(u.TermName, u.Type)] = {
    import u._
    
    val members = tpe.members.find(_.isConstructor).get.asTerm.asMethod.paramLists.flatten
//    val members = tpe.members.filter(m => m.isTerm && m.asTerm.isCaseAccessor && m.asTerm.isGetter)
    
    (for (m <- members) yield {
      m.name.toTermName -> m.typeSignature
    })(breakOut)
  }
}

package flatarray {
  
  trait FlatIO[A] {
    def classTag: ClassTag[A]

    def buildFibers(length: Int): Fibers
    def set(fibers: Fibers, index: Int, value: A): Unit
    def get(fibers: Fibers, index: Int): A
  }
  
  object FlatIO {
    def of[A <: AnyRef]: FlatIO[A] =
      macro FlatIO.ofImpl[A]
    
    def ofImpl[A <: AnyRef : c.WeakTypeTag](c: blackbox.Context): c.Expr[FlatIO[A]] = {
      import c.universe._
      
      val a = weakTypeOf[A]
      val members = getMembers(c.universe)(a)
  
      c.Expr[FlatIO[A]](q"""
        new FlatIO[${a}] {
          override def classTag = implicitly[scala.reflect.ClassTag[${a}]]
          
          override def buildFibers(length: Int): Fibers =
            Array[Array[_]](..${
              members.map(_._2).map(tpe => q"new Array[${tpe}](length).asInstanceOf[Array[_]]")
            })
            
          override def set(fibers: Fibers, index: Int, value: ${a}): Unit = {
            require(fibers.length == ${members.size})
            ..${members.zipWithIndex.map({
              case ((name, tpe), i) =>
                q"fibers($i).asInstanceOf[Array[${tpe}]](index) = value.$name"
            })}
          }
          
          override def get(fibers: Fibers, index: Int): ${a} = {
            require(fibers.length == ${members.size})
            ${a.typeSymbol.companion}(
              ..${members.zipWithIndex.map({
                case ((name, tpe), i) =>
                  q"fibers($i).asInstanceOf[Array[${tpe}]](index)"
              })}
            )
          }
        }
      """)
    }
  }
  
  case class ScalarFlatIO[A <: AnyVal : ClassTag]() extends FlatIO[A] {
    override def classTag = implicitly[ClassTag[A]]
    
    override def buildFibers(length: Int): Fibers =
      Array[Array[_]](new Array[A](length))
      
    override def set(fibers: Fibers, index: Int, value: A): Unit = {
      var Array(fiber) = fibers
      fibers.asInstanceOf[Array[A]](index) = value
    }
    
    override def get(fibers: Fibers, index: Int): A = {
      var Array(fiber) = fibers
      fibers.asInstanceOf[Array[A]](index)
    }
  }

  class Ghost[A](val flatArray: FlatArray[A], val index: Int) extends Dynamic {
    // TODO: lazy too costly? might be preferable to create same instance concurrently in some cases.      
    lazy val materialized: A =
      flatArray.materialize(index)
      
    def selectDynamic(name: String): Any =
      macro Ghost.selectDynamicImpl[A]
      
    def applyDynamic(name: String)(args: Any*): Any =
      macro Ghost.applyDynamicImpl[A]
      
    override def toString = s"Ghost { $materialized }"
//    override def toString = ???
      
    override def hashCode = ???
      
    override def equals(o: AnyRef) = ???
  }
  
  object Ghost {
    implicit def materialize[A](ghost: Ghost[A]): A =
      macro Ghost.materializeImpl[A]
    
    def materializeImpl[A : c.WeakTypeTag](c: blackbox.Context)(ghost: c.Expr[Ghost[A]]): c.Expr[A] = {
      import c.universe._
 
      c.Expr[A](ghost.tree match {
        case Apply(Select(array, name), List(index)) if name.toString == "apply" =>
          // case q"$array.apply($index)" =>
          q"$array.materialize($index)"
        case _ =>
          c.error(ghost.tree.pos, s"Ghost should not be materialized: $ghost (application: ${c.macroApplication}, prefix = ${c.prefix})")
          // q"$ghost.materialized"
          q"null"
      })
    }
    
    def applyDynamicImpl[A : c.WeakTypeTag](c: whitebox.Context)(name: c.Expr[String])(args: c.Expr[Any]*): c.Tree = {
      import c.universe._
      
      val Seq() = args.toSeq
      val Literal(Constant(nameStr: String)) = name.tree
      q"""
        ${c.prefix}.${TermName(nameStr)} 
      """
    }
    def selectDynamicImpl[A : c.WeakTypeTag](c: whitebox.Context)(name: c.Expr[String]): c.Tree = {
      import c.universe._
      
      val a = weakTypeOf[A]
      val members = getMembers(c.universe)(a)
      
      val Literal(Constant(nameStr: String)) = name.tree

      members.zipWithIndex.find(_._1._1.toString == nameStr).map({
        case ((_, tpe), fiberIndex) =>
          c.prefix.tree match {
            case Apply(Select(array, name), List(index)) if name.toString == "apply" =>
              // case q"$array.apply($index)" =>
              q"$array.fibers($fiberIndex).asInstanceOf[Array[${tpe}]]($index)"
            case ghost =>
              val ghostName = TermName(c.freshName("ghost"))
              q"""
                val $ghostName = $ghost
                $ghostName.flatArray.fibers($fiberIndex).asInstanceOf[Array[${tpe}]]($ghostName.index)
              """
          }
      }).getOrElse {
        c.error(name.tree.pos, "No such method on $a: $name")
        q"null"
      }
    }
  }
  
  class FlatArray[A : FlatIO](val fibers: Fibers, val length: Int)
  {
    def this(length: Int) =
      this(fibers = implicitly[FlatIO[A]].buildFibers(length), length = length)

//    private[this] def writeArray(a: Array[A]): Unit =
//      macro FlatArray.writeArrayImpl[A]
    
    def materialize(index: Int): A =
      implicitly[FlatIO[A]].get(fibers, index)
    
    def apply(index: Int): Ghost[A] =
      new Ghost[A](this, index)
    
//    def update(index: Int, value: A): Unit =
//      macro FlatArray.updateImpl[A]
    
//    def toArray: Array[A] =
//      macro FlatArray.toArrayImpl[A]
  }
  
  object FlatArray {
    def writeArrayImpl[A : c.WeakTypeTag](c: blackbox.Context)(a: c.Expr[Array[A]]): c.Expr[Unit] = {
      import c.universe._
      
      val a = weakTypeOf[A]
      val members = getMembers(c.universe)(a).map({
        case (name, tpe) =>
          (name, tpe, TermName(c.freshName(name.toString)))
      })
      var flatArrayName = c.freshName("flatArray")
      var arrayName = c.freshName("array")
      var lengthName = c.freshName("length")
      var indexName = c.freshName("index")
        
      c.Expr[Unit](q"""
        val $flatArrayName = ${c.prefix}
        val $arrayName = $a
        val $lengthName = $arrayName.length
        require($lengthName == $flatArrayName.length) 
        ..${members.zipWithIndex.map({
          case ((name, tpe, fiberName), i) =>
            q"val $fiberName = fibers($i).asInstanceOf[Array[${tpe}]]"
        })}
        for ($indexName <- 0 until $lengthName) {
          ..${members.zipWithIndex.map({
            case ((name, tpe, fiberName), i) =>
              q"$fiberName(index) = value"
          })}
        }
      """)
    }
//    def updateImpl[A : c.WeakTypeTag](c: blackbox.Context)(index: c.Expr[Int], value: c.Expr[A]): c.Expr[Unit] = {
//      import c.universe._
//  
//      ???
//    }
    def toArrayImpl[A : c.WeakTypeTag](c: blackbox.Context): c.Expr[Array[A]] = {
      import c.universe._
  
      
      val a = weakTypeOf[A]
      val members = getMembers(c.universe)(a).map({
        case (name, tpe) =>
          (name, tpe, TermName(c.freshName(name.toString)))
      })
      var flatArrayName = c.freshName("flatArray")
      var arrayName = c.freshName("array")
      var lengthName = c.freshName("length")
      var indexName = c.freshName("index")
        
      c.Expr[Array[A]](q"""
        val $flatArrayName = ${c.prefix}
        val $lengthName = $flatArrayName.length
        val $arrayName = new Array[${a}]($lengthName) 
        ..${members.zipWithIndex.map({
          case ((name, tpe, fiberName), i) =>
            q"val $fiberName = fibers($i).asInstanceOf[Array[${tpe}]]"
        })}
        for ($indexName <- 0 until $lengthName) {
          $arrayName($indexName) = ${weakTypeOf[A].typeSymbol.companion}(
            ..${members.zipWithIndex.map({
              case ((name, tpe, fiberName), i) =>
                q"$fiberName(index)"
            })}
          )
        }
      """)
    }
  }
}