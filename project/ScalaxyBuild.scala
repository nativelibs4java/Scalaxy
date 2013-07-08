import sbt._
import Keys._
import sbtassembly.Plugin._ ; import AssemblyKeys._
import ls.Plugin._

object Scalaxy extends Build
{
  lazy val scalaSettings = Seq(
    //exportJars := true, // use jars in classpath
    scalaVersion := "2.10.2",
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
      scalacOptions ++= Seq(
        "-encoding", "UTF-8",
        "-optimise", 
        "-deprecation",
        "-feature",
        "-unchecked"
      ),
      //scalacOptions in Test ++= Seq("-Xprint:typer"),
      //fork in Test := true,
      fork := true,
      parallelExecution in Test := false,
      libraryDependencies += "junit" % "junit" % "4.10" % "test",
      libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test")

  lazy val reflectSettings =
    standardSettings ++
    scalaSettings ++
    Seq(
      //scalacOptions ++= Seq("-language:experimental.macros"),
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
        cp filter { j => {
          val n = j.data.getName
          n.startsWith("scala-") || n.equals("jfxrt.jar")
        } }
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
        Seq(publish := { }))
    .aggregate(compilets, fx, beans, components, debug, extensions)

  lazy val compilets =
    Project(
      id = "scalaxy-compilets",
      base = file("Compilets"),
      settings =
        standardSettings ++
        shadeSettings ++
        Seq(
          // Assembly artifact is just here to accommodate console mode, it's not to be published.
          publish := { },
          test in assembly := {},
          artifact in (Compile, assembly) ~= { _.copy(`classifier` = None) },
          scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)
        )
    )
    .dependsOn(compiletsPlugin, compiletsApi, defaultCompilets)
    .aggregate(compiletsPlugin, compiletsApi, defaultCompilets)

  lazy val compiletsApi =
    Project(
      id = "scalaxy-compilets-api",
      base = file("Compilets/API"),
      settings = reflectSettings ++ Seq(
        scalacOptions ++= Seq(
          "-language:experimental.macros"
        )
      ))

  lazy val components =
    Project(id = "scalaxy-components", base = file("Components"), settings = reflectSettings)

  lazy val compiletsPlugin =
    Project(
      id = "scalaxy-compilets-plugin",
      base = file("Compilets/Plugin"),
      settings = 
        reflectSettings ++
        shadeSettings ++
        Seq(
          artifact in (Compile, assembly) ~= {
            _.copy(`classifier` = None)//Some("assembly"))
          },
          publishArtifact in Test := true))
    .dependsOn(compiletsApi)

  lazy val defaultCompilets =
    Project(
      id = "scalaxy-default-compilets",
      base = file("Compilets/DefaultCompilets"),
      settings = reflectSettings)
    .dependsOn(compiletsApi, compiletsPlugin % "test->test")

  lazy val extensions =
    Project(id = "scalaxy-macro-extensions", base = file("MacroExtensions"), settings = reflectSettings ++ Seq(
      watchSources <+= baseDirectory map { _ / "examples" }
    ))
    .dependsOn(debug)

  lazy val beans =
    Project(id = "scalaxy-beans", base = file("Beans"), settings = reflectSettings)

  lazy val loops =
    Project(id = "scalaxy-loops", base = file("Loops"), settings = reflectSettings)

  lazy val debug =
    Project(id = "scalaxy-debug", base = file("Debug"), settings = reflectSettings)

  lazy val fxSettings = reflectSettings ++ Seq(
    unmanagedJars in Compile ++= Seq(
      new File(System.getProperty("java.home")) / "lib" / "jfxrt.jar"
    )
  )

  lazy val fx =
    Project(
      id = "scalaxy-fx",
      base = file("Fx/Macros"),
      settings = fxSettings)
    .dependsOn(fxRuntime)
    .aggregate(fxRuntime)

  lazy val fxRuntime =
    Project(
      id = "scalaxy-fx-runtime",
      base = file("Fx/Runtime"),
      settings = fxSettings)
}
