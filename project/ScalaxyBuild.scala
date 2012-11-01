import sbt._
import Keys._
import sbtassembly.Plugin._ ; import AssemblyKeys._
import ls.Plugin._

object Scalaxy extends Build
{
  lazy val scalaSettings = Seq(
    //exportJars := true, // use jars in classpath
    scalaVersion := "2.10.0-RC1",
    //scalaVersion := "2.11.0-SNAPSHOT",
    crossScalaVersions := Seq(
      "2.10.0-RC1", 
      "2.10.0-SNAPSHOT", 
      "2.11.0-SNAPSHOT"))

  lazy val infoSettings = Seq(
    organization := "com.nativelibs4java",
    version := "0.3-SNAPSHOT",
    licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause")),
    homepage := Some(url("https://github.com/ochafik/Scalaxy")),
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <scm>
        <url>git@github.com:ochafik/Scalaxy.git</url>
        <connection>scm:git:git@github.com:ochafik/Scalaxy.git</connection>
      </scm>
      <developers>
        <developer>
          <id>ochafik</id>
          <name>Olivier Chafik</name>
          <url>http://ochafik.com/</url>
        </developer>
      </developers>
    ),
    (LsKeys.docsUrl in LsKeys.lsync) <<= homepage,
    (LsKeys.tags in LsKeys.lsync) := 
       Seq("compiler-plugin", "rewrite", "ast", "transform", "optimization", "optimisation"),
    (description in LsKeys.lsync) :=
      "A scalac compiler plugin that optimizes the code by rewriting for loops on ranges into while loops, avoiding some implicit object creations when using numerics...",
    LsKeys.ghUser := Some("ochafik"),
    LsKeys.ghRepo := Some("Scalaxy"))

  lazy val standardSettings =
    Defaults.defaultSettings ++
    infoSettings ++
    sonatypeSettings ++
    scalaSettings ++
    seq(lsSettings: _*) ++
    Seq(
      javacOptions ++= Seq("-Xlint:unchecked"),
      scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
      scalacOptions ++= Seq("-language:experimental.macros"),
      fork in Test := true,
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies += "junit" % "junit" % "4.10" % "test",
      libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test")

  lazy val sonatypeSettings = Seq(
    publishMavenStyle := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("-SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    })

  override lazy val settings = 
    super.settings ++
    Seq(
      shellPrompt := { s => Project.extract(s).currentProject.id + "> " }
    ) ++ scalaSettings

  lazy val _scalaxy =
    Project(
      id = "scalaxy",
      base = file("."),
      settings =
        standardSettings ++
        assemblySettings ++ 
        addArtifact(artifact in (Compile, assembly), assembly) ++
        Seq(
          test in assembly := {},
          publishArtifact in (Compile, packageBin) := false,
          artifact in (Compile, assembly) ~= { art =>
            art.copy(`classifier` = None) // Some("assembly"))
          },
          excludedJars in assembly <<= (fullClasspath in assembly) map { cp => 
            cp filter { _.data.getName.startsWith("scala-") }
          },
          scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)
        )).
    dependsOn(macros, compilets).
    aggregate(macros, compilets)

  lazy val compilets =
    Project(id = "scalaxy-compilets", base = file("Compilets"), settings = standardSettings).
    dependsOn(macros)

  lazy val macros =
    Project(id = "scalaxy-macros", base = file("Macros"), settings = standardSettings)
}
