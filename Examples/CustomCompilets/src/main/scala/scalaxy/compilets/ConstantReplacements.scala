package scalaxy; package compilets

import scala.reflect.runtime.universe._

import matchers._

object ReplaceConstants extends Compilet
{
  def removeDevilConstant = replace(666, 667)

  def replace888Constant(v: Int) =
    when(v)(v) {
      case IntConstant(888) :: Nil =>
        replacement(999)
    }

  /*
  def replaceVarargs(fmt: String, args: Object*) = replace(
    println(String.format(fmt, args:_*)),
    System.out.printf(fmt + "\n", args:_*)
  )
  */
}
