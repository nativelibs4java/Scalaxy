[Scala 2.11 introduced name-based extractors](http://hseeberger.github.io/blog/2013/10/04/name-based-extractors-in-scala-2-dot-11/), which means we can avoid creating lots of tuples / boxing lots of primitive values.

This simple experiment makes all case classes use that new trick, thanks to a compiler plugin!

Usage:

    scalaVersion := "2.11.4"

    autoCompilerPlugins := true

    addCompilerPlugin("com.nativelibs4java" %% "scalaxy-fastcaseclasses" % "0.4-SNAPSHOT")

    scalacOptions += "-Xplugin-require:scalaxy-fastcaseclasses"

    resolvers += Resolver.sonatypeRepo("snapshots")

