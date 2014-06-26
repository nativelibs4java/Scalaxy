resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.10.4"

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

//addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise_2.10.2" % "2.0.0-SNAPSHOT")
