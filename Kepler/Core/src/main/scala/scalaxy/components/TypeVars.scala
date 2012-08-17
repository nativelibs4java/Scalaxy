package scalaxy; package components


import scala.reflect._
import scala.reflect.runtime._
import scala.reflect.runtime.universe._

object TypeVars {
  /*
  class T1
  class T2
  class T3
  class T4
  class T5
  class T6
  class T7
  class T8
  class T9
  class T10
  class T11
  class T12
  class T13
  class T14
  
  private val typeTags = Seq(
    typeTag[T1 ],
    typeTag[T2 ],
    typeTag[T3 ],
    typeTag[T4 ],
    typeTag[T5 ],
    typeTag[T6 ],
    typeTag[T7 ],
    typeTag[T8 ],
    typeTag[T9 ],
    typeTag[T10],
    typeTag[T11],
    typeTag[T12],
    typeTag[T13],
    typeTag[T14]
  )
  */
  
  // Using classes that are in the same classloader as the mirror...
  // TODO ditch this ugly hack and use Tx classes above, or (better) free types.
  ///*
  private val typeTags = Seq(
    typeTag[scala.reflect.api.Universe#TreeApi],
    typeTag[scala.reflect.api.Universe#SelectApi],
    typeTag[scala.reflect.api.Universe#IdentApi],
    typeTag[scala.reflect.api.Universe#ValDefApi],
    typeTag[scala.reflect.api.Universe#DefDefApi],
    typeTag[scala.reflect.api.Universe#TypeTreeApi],
    typeTag[scala.reflect.api.Universe#FunctionApi],
    typeTag[scala.reflect.api.Universe#TypeApplyApi],
    typeTag[scala.reflect.api.Universe#TypedApi],
    typeTag[scala.reflect.api.Universe#TypeDefApi],
    typeTag[scala.reflect.api.Universe#UnApplyApi],
    typeTag[scala.reflect.api.Universe#TryApi],
    typeTag[scala.reflect.api.Universe#TemplateApi],
    typeTag[scala.reflect.api.Universe#MatchApi]
  )
  //*/
  
  private val types = typeTags.map(_.tpe.asInstanceOf[base.Universe#Type]).toSet
  
  def isTypeVar(mirror: base.Universe)(tpe: mirror.Type) =
    types.contains(tpe)
  
  def getNthTypeVarTag(i: Int) =
    try {
      typeTags(i)
    } catch { case _ =>
      throw sys.error("There's currently a maximum of " + typeTags.size + " type variables, cannot get the one at index " + i)
    }
}
