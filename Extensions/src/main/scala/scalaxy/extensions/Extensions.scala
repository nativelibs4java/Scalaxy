// Author: Olivier Chafik (http://ochafik.com)
// Feel free to modify and reuse this for any purpose ("public domain / I don't care").
package scalaxy.extensions

//import scala.reflect.api.Universe
import scala.tools.nsc.Global
import scala.reflect.internal.Flags

trait Extensions
{
  val global: Global
  import global._
  import definitions._

  def termPath(path: String): Tree = {
    termPath(path.split("\\.").toList)
  }
  def termPath(components: List[String]): Tree = {
    components.tail.foldLeft(Ident(components.head: TermName): Tree)((p, n) => Select(p, n: TermName))
  }
  
  def typePath(path: String): Tree = {
    val components = path.split("\\.")
    Select(termPath(components.dropRight(1).toList), components.last: TypeName)
  }
  
  def newEmptyTpt() = TypeTree(null)
  
  def genParamAccessorsAndConstructor(namesAndTypeTrees: List[(String, Tree)]): List[Tree] = {
    (
      namesAndTypeTrees.map { 
        case (name, tpt) =>
          ValDef(Modifiers(Flags.PARAMACCESSOR), name, tpt, EmptyTree)
      }
    ) :+
    DefDef(
      NoMods,
      nme.CONSTRUCTOR,
      Nil,
      List(
        namesAndTypeTrees.map { case (name, tpt) =>
          ValDef(NoMods, name, tpt, EmptyTree)
        }
      ),
      newEmptyTpt(),
      newSuperInitConstructorBody()
    )
  }
  
  def newSelfValDef(): ValDef = {
    ValDef(Modifiers(Flag.PRIVATE), "_": TermName, newEmptyTpt(), EmptyTree)
  }
  
  def newSuperInitConstructorBody(): Tree = {
    Block(
      // super.<init>()
      Apply(Select(Super(This("": TypeName), "": TypeName), nme.CONSTRUCTOR), Nil),
      Literal(Constant(()))
    )
  }
  
  lazy val anyValTypeNames =
    Set("Int", "Long", "Short", "Byte", "Double", "Float", "Char", "Boolean", "AnyVal")
    
  def parentTypeTreeForImplicitWrapper(typeName: Name): Tree = {
    // If the type being extended is an AnyVal, make the implicit class a value class :-)
    typePath(
      if (anyValTypeNames.contains(typeName.toString))
        "scala.AnyVal" 
      else 
        "scala.AnyRef"
    )
  }
  
  // TODO: request some -- official API in scala.reflect.api.FlagSets#FlagOps
  implicit class FlagOps2(flags: FlagSet) {
    def --(others: FlagSet) = {
      //(flags.asInstanceOf[Long] & ~others.asInstanceOf[Long]).asInstanceOf[FlagSet]
      flags & ~others
    }
  }
}
