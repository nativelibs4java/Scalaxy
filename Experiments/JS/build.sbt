resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies in ThisBuild <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies += "com.google.javascript" % "closure-compiler" % "v20130722"
