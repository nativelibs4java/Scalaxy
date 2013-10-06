organization := "com.nativelibs4java"

name := "scalaxy-fx-example"

version := "1.0-SNAPSHOT"

// Only works with 2.10.0+
scalaVersion := "2.10.0"

// Add JavaFX Runtime as an unmanaged dependency, hoping to find it in the JRE's library folder.
unmanagedJars in Compile ++= Seq(new File(System.getProperty("java.home")) / "lib" / "jfxrt.jar")

// This is the bulk of Scalaxy/Fx, needed only during compilation (no runtime dependency here).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-fx" % "0.3-SNAPSHOT" % "provided"

// JavaFX doesn't cleanup everything well, need to fork tests / runs.
fork := true

// Scalaxy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
