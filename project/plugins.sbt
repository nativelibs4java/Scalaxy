resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.4")

//addSbtPlugin("com.github.retronym" % "sbt-onejar" % "0.8")

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0")

// ls.implicit.ly
addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Opts.resolver.sonatypeReleases
)
