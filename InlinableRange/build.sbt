scalaVersion := "2.10.0-M4"

//scalacOptions ++= Seq(
//	"-Xprint:typer",
//	"-Ymacro-debug-verbose"
//)

libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)

libraryDependencies += "com.novocode" % "junit-interface" % "0.5" % "test->default"
