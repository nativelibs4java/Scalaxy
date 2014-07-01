scalaVersion := "2.11.1"

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.3-SNAPSHOT")

libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.3-SNAPSHOT"

// addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.2.1")

scalacOptions += "-Xplugin-require:scalaxy-streams"

scalacOptions ++= Seq("-optimise", "-Yclosure-elim", "-Yinline")

// Scalaxy/Streams snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
