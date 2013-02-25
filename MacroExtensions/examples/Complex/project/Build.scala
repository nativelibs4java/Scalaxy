import sbt._
import Keys._

object Build extends Build {
    lazy val root = Project(id = "Root", base = file(".")).aggregate(library, usage)
    
    lazy val library = Project(id = "Library", base = file("Library"))
    
    lazy val usage = Project(id = "Usage", base = file("Usage")).dependsOn(library)
}
