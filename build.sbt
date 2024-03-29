inThisBuild(
  List(
    organization := "fr.hammons",
    homepage := Some(url("https://slinc.hammons.fr/docs/index.html")),
    licenses := List(
      "LGPL-3.0" -> url("https://www.gnu.org/licenses/lgpl-3.0.en.html")
    ),
    developers := List(
      Developer(
        "markehammons",
        "Mark Edgar Hammons II",
        "markehammons@gmail.com",
        url("http://mark.hammons.fr/index.gmi")
      ),
      Developer(
        "rrramiro",
        "Ramiro Calle",
        "",
        url("https://github.com/rrramiro")
      ),
      Developer(
        "i10416",
        "",
        "contact.110416@gmail.com",
        url("https://github.com/i10416")
      )
    ),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    versionScheme := Some("early-semver"),
    scalaVersion := "3.3.1",
    dynverVTagPrefix := false
  )
)

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

Compile / doc / scalacOptions ++= Seq("-siteroot", "docs")
mimaPreviousArtifacts := previousStableVersion.value
  .map(organization.value %% moduleName.value % _)
  .toSet
