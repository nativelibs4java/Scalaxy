scalaVersion := "2.10.0"

unmanagedJars in Compile ++= Seq(new File(System.getProperty("java.home")) / "lib" / "jfxrt.jar")

libraryDependencies += "com.nativelibs4java" %% "scalaxy-fx-runtime" % "0.3-SNAPSHOT" 

libraryDependencies += "com.nativelibs4java" %% "scalaxy-fx" % "0.3-SNAPSHOT" % "provided"

