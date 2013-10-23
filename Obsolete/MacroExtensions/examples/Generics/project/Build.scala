import sbt._
import Keys._

object Build extends Build {
    lazy val root = Project(id = "Test", base = file(".")).aggregate(library).dependsOn(library)
    
    lazy val library = Project(id = "Library", base = file("Library"))
}
