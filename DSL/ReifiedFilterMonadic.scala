package scalaxy.dsl

import scala.language.experimental.macros
import scala.collection.GenTraversableOnce
import scala.collection.generic.CanBuildFrom

trait ReifiedFilterMonadic[A, Repr] {
  self =>
  
  def reifiedForeach[U](
      f: ReifiedFunction[A, U],
      filters: List[ReifiedFunction[A, Boolean]]): Unit
      
  def reifiedFlatMap[B, That](
      f: ReifiedFunction[A, GenTraversableOnce[B]],
      filters: List[ReifiedFunction[A, Boolean]])(
      implicit bf: CanBuildFrom[Repr, B, That]): That
  
  def reifiedFilters: List[ReifiedFunction[A, Boolean]] = Nil
  
  def foreach[U](f: A => U): Unit = 
    macro ReifiedFilterMonadicMacros.foreachImpl[A, Repr, U]
  
  def withFilter(f: A => Boolean): ReifiedFilterMonadic[A, Repr] = 
    macro ReifiedFilterMonadicMacros.withFilterImpl[A, Repr]
  
  def flatMap[B, That](
      f: A => GenTraversableOnce[B])(
      implicit bf: CanBuildFrom[Repr, B, That]): That = 
    macro ReifiedFilterMonadicMacros.flatMapImpl[A, Repr, B, That]
  
  def withFilters(filters: List[ReifiedFunction[A, Boolean]]) = 
    new WithFilters(filters)
  
  class WithFilters(filters: List[ReifiedFunction[A, Boolean]])
      extends ReifiedFilterMonadic[A, Repr] {
    override def reifiedFilters = filters
    override def reifiedForeach[U](
        f: ReifiedFunction[A, U],
        filters: List[ReifiedFunction[A, Boolean]]) {
      self.reifiedForeach(f, filters)  
    }
      
    override def reifiedFlatMap[B, That](
        f: ReifiedFunction[A, GenTraversableOnce[B]],
        filters: List[ReifiedFunction[A, Boolean]])(
        implicit bf: CanBuildFrom[Repr, B, That]): That =
      self.reifiedFlatMap(f, filters)
  }
}
