// Only works with 2.10.0+
scalaVersion := "2.10.0"

// Add JavaFX Runtime as an unmanaged dependency, hoping to find it in the JRE's library folder.
unmanagedJars in Compile ++= Seq(new File(System.getProperty("java.home")) / "lib" / "jfxrt.jar")

// This is the bulk of Scalaxy-FX, needed only during compilation (no runtime dependency here).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-fx" % "0.3-SNAPSHOT" % "provided"

// This runtime library contains only one class needed for the `onChange { ... }` syntax.
// You can just remove it if you don't use that syntax.
libraryDependencies += "com.nativelibs4java" %% "scalaxy-fx-runtime" % "0.3-SNAPSHOT" 

// JavaFX doesn't cleanup everything well, need to fork tests / runs.
fork := true

// Scalaxy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
