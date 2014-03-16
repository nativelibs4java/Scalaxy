scalaVersion := "2.11.0-RC1"

name := "scalaxy-stream"

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.10" % "test"
)

watchSources <++= baseDirectory map { path => (path / "examples" ** "*.scala").get }

//scalacOptions in Test <++= (packageBin in Compile) map(path => Seq("-Xplugin:" + path)

fork in Test := true
