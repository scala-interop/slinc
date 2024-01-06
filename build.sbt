scalaVersion := "3.3.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-Wunused:all",
  "-feature",
  "-unchecked",
  "-Xcheck-macros",
  "-Xprint-suspension",
  "-Xsemanticdb",
  "-Yexplicit-nulls",
  "-Ysafe-init",
  "-source:future",
  "-Ykind-projector",
  "-Vprofile"
)

libraryDependencies += "org.scalameta" %% "munit" % "1.0.0-M10" % Test
libraryDependencies += "org.scalameta" %% "munit-scalacheck" % "1.0.0-M10" % Test
libraryDependencies += "org.scala-lang" %% "scala3-staging" % scalaVersion.value

publishTo := Some(
  "Sonatype Nexus" at "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2"
)
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

Compile / doc / scalacOptions ++= Seq("-siteroot", "docs")
