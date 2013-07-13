package scalaxy.dsl

case class Table(name: String)

/*
scalac ReifiedFilterMonadicMacros.scala ReifiedFunction.scala ReifiedFilterMonadic.scala && scalac Select.scala Test.scala && scala Test
*/
object Test extends App 
{
  val table = Table("users")
  val q = for (row <- Select(table)) {
    println(row)
  }
  
  /*
case class ReifiedFunction[A, B](
  function: A => B,
  captures: Map[Symbol, () => Any], 
  tree: Function)
*/
}
