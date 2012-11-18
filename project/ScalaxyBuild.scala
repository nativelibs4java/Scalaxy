import sbt._
import Keys._
import sbtassembly.Plugin._ ; import AssemblyKeys._
import ls.Plugin._

object Scalaxy extends Build
{
  lazy val scalaSettings = Seq(
    //exportJars := true, // use jars in classpath
    scalaVersion := "2.10.0-RC2",
    //scalaVersion := "2.11.0-SNAPSHOT",
    crossScalaVersions := Seq(
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
    seq(lsSettings: _*) ++
    Seq(
      javacOptions ++= Seq("-Xlint:unchecked"),
      scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
      //fork in Test := true,
      parallelExecution in Test := false,
      libraryDependencies += "junit" % "junit" % "4.10" % "test",
      libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test")

  lazy val reflectSettings =
    standardSettings ++
    scalaSettings ++
    Seq(
      scalacOptions ++= Seq("-language:experimental.macros"),
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _))

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

  lazy val shadeSettings =
    assemblySettings ++ 
    addArtifact(artifact in (Compile, assembly), assembly) ++
    Seq(
      publishArtifact in (Compile, packageBin) := false,
      excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
        // Exclude scala-library and al.
        cp filter { _.data.getName.startsWith("scala-") }
      },
      pomPostProcess := { (node: scala.xml.Node) =>
        // Since we publish the assembly (shaded) jar, 
        // remove lagging scalaxy dependencies from pom ourselves.
        import scala.xml._; import scala.xml.transform._
        try {
          new RuleTransformer(new RewriteRule {
            override def transform(n: Node): Seq[Node] ={
              if ((n \ "artifactId" find { _.text.startsWith("scalaxy-") }) != None)
                Array[Node]()
              else
                n
            }
          })(node)
        } catch { case _: Throwable =>
          node
        }
      }
    )
  
  lazy val _scalaxy =
    Project(
      id = "scalaxy",
      base = file("."),
      settings =
        standardSettings ++
        shadeSettings ++
        Seq(
          test in assembly := {},
          artifact in (Compile, assembly) ~= { _.copy(`classifier` = None) },
          scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)
        )).
    dependsOn(plugin, compilets, api).
    aggregate(plugin, compilets, api)
    
  lazy val plugin =
    Project(id = "scalaxy-plugin", base = file("Plugin"), settings = 
      reflectSettings ++
      shadeSettings ++
      Seq(
        artifact in (Compile, assembly) ~= { _.copy(`classifier` = Some("assembly")) },
        publishArtifact in Test := true)).
    dependsOn(api)

  lazy val compilets =
    Project(id = "scalaxy-compilets", base = file("Compilets"), settings = reflectSettings).
    dependsOn(api, plugin % "test->test")

  lazy val api =
    Project(id = "scalaxy-api", base = file("API"), settings = reflectSettings)
    
  //lazy val scalaxySbtPlugin =
  //  Project(id = "sbt-scalaxy", base = file("Sbt"), settings = standardSettings ++ Seq(
  //    scalaVersion := "2.9.2",
  //    //crossSbtVersions := Seq("0.11.3", "0.11.2" ,"0.12" ,"0.12.1"),
  //    sbtPlugin := true
  //    //CrossBuilding.scriptedSettings, // ?
  //  ))
}
