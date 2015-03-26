scalaVersion := "2.11.5"

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

scalacOptions += "-target:jvm-1.8"
