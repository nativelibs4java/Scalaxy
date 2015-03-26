package scalaxy.reified

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._
import scala.collection.generic.CanBuildFrom

import scala.language.implicitConversions

object stream {
  type Foreachable[A] = {
    def foreach(f: A => Unit): Unit
  }

  //  class SeqSource[A](seq: A*) {
  //    
  //  }

  sealed trait OpList[A] {
    def apply[S: TypeTag](source: Reified[S]): Reified[A]
  }
  //  case object NilOpList extends OpList[Unit]
  case class IdOpList[A: TypeTag]() extends OpList[A] {
    override def apply[S: TypeTag](source: Reified[S]) =
      reified(source.value.asInstanceOf[A])
  }
  case class ConsOpList[F: TypeTag, T: TypeTag, O <: Op[F, T]: TypeTag](op: O, from: OpList[F]) extends OpList[T] {
    override def apply[S: TypeTag](source: Reified[S]) =
      op(from.value.apply(source))
  }

  sealed trait Op[F, T] {
    def apply(from: Reified[F]): Reified[T]
  }

  case class MapOp[A: TypeTag, F <: Traversable[A]: TypeTag, B: TypeTag, T <: Traversable[B]: TypeTag](f: Reified[A => B], cbf: Reified[CanBuildFrom[Traversable[A], B, T]]) extends Op[F, T] {
    override def apply(from: Reified[F]) =
      reified(from.value.map[B, T](f)(cbf.value))
  }
  case class FilterOp[A: TypeTag, T <: Traversable[A]: TypeTag](f: Reified[A => Boolean]) extends Op[T, Traversable[A]] {
    override def apply(from: Reified[T]) =
      reified(from.value.filter(f))
  }

  //  trait HasStream {
  //    def stream: 
  //  }
  //trait OpChain[Inner, Outer]

  // S <: Foreachable[F]
  case class Stream[F: TypeTag, S: TypeTag, T: TypeTag](source: S, ops: OpList[T])

  // object Stream {
  //   def apply[F: TypeTag, S: TypeTag, T: TypeTag](source: S) =
  //     new Stream[F, S, T](source, IdOpList[T]())
  // }

  implicit class TraversableStream[F: TypeTag, S: TypeTag, A: TypeTag, T <: Traversable[A]: TypeTag](stream: Stream[F, S, T]) {

    // def map[B: TypeTag, Repr <: Traversable[B]: TypeTag](f: Reified[A => B])(implicit cbf: Reified[CanBuildFrom[T, B, Repr]]): Stream[F, S, Repr] = {
    //   Stream[F, S, Repr](stream.source, ConsOpList(MapOp[A, T, B, Repr](f, cbf), stream.ops))
    // }
  }

  implicit def rcbf[From, Elem, To](cbf: CanBuildFrom[From, Elem, To]): Reified[CanBuildFrom[From, Elem, To]] =
    ???

}

