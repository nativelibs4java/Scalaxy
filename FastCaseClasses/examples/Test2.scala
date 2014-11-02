package foo

object ClassPath {
}
import ClassPath._


abstract class ClassPath[T] {
  case class ClassRep(binary: Option[T], source: Option[String]) {
  }
}

class JavaClassPath extends ClassPath {

}
