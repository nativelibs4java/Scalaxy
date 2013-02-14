organization := "com.nativelibs4java"

name := "scalaxy-loops-test"

version := "1.0-SNAPSHOT"

// Only works with 2.10.0+
scalaVersion := "2.10.0"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided"

// Run benchmarks in cold VMs.
fork := true

// Scalaxy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")

scalacOptions ++= Seq("-optimise", "-Yinline", "-Yclosure-elim", "-feature")//, "-Xprint:icode")
