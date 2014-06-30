resolvers += Resolver.sonatypeRepo("snapshots")

addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise_2.10.4" % "2.0.1")

libraryDependencies in ThisBuild <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies += "com.google.javascript" % "closure-compiler" % "v20130722"
