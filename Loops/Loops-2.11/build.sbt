scalaVersion := "2.11.0-RC1"

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.10" % "test",
  "com.novocode" % "junit-interface" % "0.8" % "test"
)
