scalaVersion := "2.11.4"

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-fastcaseclasses" % "0.4-SNAPSHOT")

scalacOptions += "-Xplugin-require:scalaxy-fastcaseclasses"

scalacOptions += "-Xprint:cleanup"

// Scalaxy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.4-SNAPSHOT" % "provided"

fork := true
