package scalaxy.js
import ast._

import scala.language.implicitConversions

import scala.reflect.NameTransformer.{ encode, decode }

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.macros.Context
import scala.reflect.api.Universe
// import scala.reflect.api.Universe

trait Matchers {

  val global: Universe
  import global._

  object N {
    def unapply(name: Name): Option[String] =
      Option(name).map(n => decode(n.toString))
  }

  object SuperCall { 
    object SuperLike {
      def unapply(tree: Tree) = tree match {
        case Ident(N("super")) => true
        case Super(qual, mix) => true
        case _ => false
      }
    }
    def unapply(tree: Tree): Option[(/*Tree, Name, */Name, List[Tree])] = Option(tree) collect {
      case Apply(Select(SuperLike(), Ident(methodName)), args) =>
        (/*qual, mix, */methodName, args)
      case Select(SuperLike(), Ident(methodName)) =>
        (/*qual, mix, */methodName, Nil)
    }
  }
}
