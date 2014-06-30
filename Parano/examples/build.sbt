// Only works with 2.10.2+
scalaVersion := "2.10.4"

autoCompilerPlugins := true

// Scalaxy/Parano plugin
addCompilerPlugin("com.nativelibs4java" %% "scalaxy-parano" % "0.3-SNAPSHOT")

// Ensure Scalaxy/Parano's plugin is used.
scalacOptions += "-Xplugin-require:scalaxy-parano"

// Scalaxy/Parano snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
