scalaVersion := "2.11.2"

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.4-SNAPSHOT")

libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.4-SNAPSHOT" % "provided"

// addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.3.4")

// scalacOptions += "-Xplugin-require:scalaxy-streams"

scalacOptions ++= Seq("-optimise", "-Yclosure-elim", "-Yinline")

// Scalaxy/Streams snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
