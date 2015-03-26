package scalaxy.native


import scala.language.reflectiveCalls

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.language.dynamics

import scala.annotation.tailrec
import scala.reflect.macros.blackbox.Context

import scala.collection.JavaConversions._

import java.io.File
import org.bridj.Pointer

import scala.collection.JavaConversions._

trait NativeTypeTag[A] {
  def size: Long
  def deserialize(ptr: Ptr[A]): A
  def serialize(ptr: Ptr[A], value: A): Unit
}
class Ptr[A](val peer: Long) extends AnyVal {
  def +(index: Long): Ptr[A] =
    new Ptr[A](peer + index)
  def apply(index: Long)(implicit tt: NativeTypeTag[A]): A =
    macro PtrImpl.applyImpl[A]
  def update(index: Long, value: A)(implicit tt: NativeTypeTag[A]): A =
    macro PtrImpl.updateImpl[A]
}

object PtrImpl {
  def applyImpl[A : c.WeakTypeTag]
               (c: Context)
               (index: c.Expr[Long])
               (tt: c.Expr[NativeTypeTag[A]])
               : c.Expr[A] =
  {
    import c.universe._
    c.Expr[A](q"$tt.deserialize(${c.prefix} + $index)")
  }
  def updateImpl[A : c.WeakTypeTag]
                (c: Context)
                (index: c.Expr[Long], value: c.Expr[A])
                (tt: c.Expr[NativeTypeTag[A]])
                : c.Expr[A] =
  {
    import c.universe._
    c.Expr[A](q"$tt.serialize(${c.prefix} + $index, $value)")
  }
}
