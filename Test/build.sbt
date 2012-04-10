resolvers += "Sonatype OSS Snapshots Repository" at "http://oss.sonatype.org/content/groups/public/"

resolvers += "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

resolvers += Resolver.file("Local Ivy Repository", file(Path.userHome.absolutePath + "/.ivy2/local"))

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy" % "0.3-SNAPSHOT")

scalaVersion := "2.10.0-M2"
