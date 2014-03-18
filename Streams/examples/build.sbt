scalaVersion := "2.11.0-RC1"

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.3-SNAPSHOT")

scalacOptions += "-Xplugin-require:scalaxy-streams"

// Scalaxy/Streams snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
