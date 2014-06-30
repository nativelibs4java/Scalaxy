// Only works with 2.10.2+
scalaVersion := "2.10.4"

autoCompilerPlugins := true

// Scalaxy/Privacy plugin
addCompilerPlugin("com.nativelibs4java" %% "scalaxy-privacy-plugin" % "0.3-SNAPSHOT")

// Scalaxy/Privacy annotations are only needed to avoid confusing IDEs.
libraryDependencies += "com.nativelibs4java" %% "scalaxy-privacy" % "0.3-SNAPSHOT"

// Ensure Scalaxy/Privacy's plugin is used.
scalacOptions += "-Xplugin-require:scalaxy-privacy"

// Scalaxy/Privacy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
