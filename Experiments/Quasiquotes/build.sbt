scalaVersion := "2.11.0-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

//libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test"
