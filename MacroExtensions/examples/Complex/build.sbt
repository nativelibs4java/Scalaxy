// Only works with 2.10.0+
scalaVersion := "2.10.3"

// Uncomment this to see what's happening:
//scalacOptions ++= Seq("-Xprint:parser", "-Xprint:refchecks")

scalacOptions ++= Seq("-optimise", "-Yinline", "-Yclosure-elim", "-feature", "-deprecation")

libraryDependencies += "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided"

resolvers += Resolver.sonatypeRepo("snapshots")

fork := true
