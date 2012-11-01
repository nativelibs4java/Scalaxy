package scalaxy; package components

object SyntheticTypeTags
{
  import scala.reflect.runtime.currentMirror
  import scala.reflect.runtime.{universe => ru}

  def cast[T](v: Any): T = v.asInstanceOf[T]

  def WeakTypeTag[T](tpe: ru.Type): ru.TypeTag[T] = {
    val u = ru.asInstanceOf[ru.type with scala.reflect.internal.StdCreators]
    val m = currentMirror.asInstanceOf[scala.reflect.api.Mirror[u.type]]

    //cast(u.WeakTypeTag[T](cast(m), u.FixedMirrorTypeCreator(cast(m), cast(tpe))))

    val weakTTag = u.WeakTypeTag[T](cast(m), u.FixedMirrorTypeCreator(cast(m), cast(tpe)))

    val manifest = u.typeTagToManifest[Any](m, cast(weakTTag))
    cast(u.manifestToTypeTag(m, cast(manifest)))
  }
  
  import scala.reflect.api.Universe
  import scala.reflect.api.Mirror

  class SyntheticTypeCreator[T](
      copyIn: Universe => Universe#TypeTag[T])
    extends scala.reflect.api.TypeCreator
  {
    def apply[U <: Universe with Singleton](m: scala.reflect.api.Mirror[U]): U # Type = {
      copyIn(m.universe).asInstanceOf[U # TypeTag[T]].tpe
    }
  }

  class WeakTypeTagImpl[T](val tpec: SyntheticTypeCreator[T]) extends ru.WeakTypeTag[T] {
    lazy val tpe: ru.Type = tpec(currentMirror)
    override val mirror = currentMirror
    def in[U <: Universe with Singleton](otherMirror: scala.reflect.api.Mirror[U]) = {
      val otherMirror1 = otherMirror.asInstanceOf[scala.reflect.api.Mirror[otherMirror.universe.type]]
      otherMirror.universe.TypeTag[T](otherMirror1, tpec)
    }
  }

  class TypeTagImpl[T](tpec: SyntheticTypeCreator[T]) extends WeakTypeTagImpl[T](tpec) with ru.TypeTag[T]

  class SyntheticTypeTag[T](_tpe: ru.Type, copyIn: Universe => Universe#TypeTag[T])
    extends TypeTagImpl[T](new SyntheticTypeCreator[T](copyIn)) {
    override lazy val tpe: ru.Type = _tpe
  }
}
