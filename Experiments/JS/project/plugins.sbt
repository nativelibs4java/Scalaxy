resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Opts.resolver.sonatypeReleases
)

addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.1.2")

