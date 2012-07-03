scalaVersion := "2.10.0-M4"

scalacOptions += "-Xprint:typer"

libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)

libraryDependencies += "com.novocode" % "junit-interface" % "0.5" % "test->default"
