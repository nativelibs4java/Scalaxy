// Only works with 2.10.0+
scalaVersion := "2.10.4"

autoCompilerPlugins := true

// Scalaxy/MacroExtensions plugin
addCompilerPlugin("com.nativelibs4java" %% "scalaxy-macro-extensions" % "0.3-SNAPSHOT")

// Ensure Scalaxy/MacroExtensions's plugin is used.
scalacOptions += "-Xplugin-require:scalaxy-extensions"

// Uncomment this to see what's happening:
//scalacOptions += "-Xprint:scalaxy-extensions"

// We're compiling macros, reflection library is needed.
libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" %)

libraryDependencies += "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided"

// Scalaxy/MacroExtensions snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
