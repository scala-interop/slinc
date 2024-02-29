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

lazy val slinc = project
  .in(file("slinc"))
  .settings(
    name := "slinc",
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
    ),
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0-M10" % Test,
    libraryDependencies += "org.scalameta" %% "munit-scalacheck" % "1.0.0-M10" % Test,
    libraryDependencies += "org.scala-lang" %% "scala3-staging" % scalaVersion.value,
    Compile / doc / scalacOptions ++= Seq("-siteroot", "docs"),
    mimaPreviousArtifacts := previousStableVersion.value
      .map(organization.value %% moduleName.value % _)
      .toSet
  )

lazy val copyNative = TaskKey[Set[File]]("copy shared lib")

lazy val examples = project
  .in(file("slinc-examples"))
  .dependsOn(slinc)
  .settings(
    publishTo := None,
    publishLocal := Def.task(()),
    copyNative := {
      val nativeSrc = (`examples-native` / Compile / nativeCompile).value
      // rename shared lib name so that it complies with SlinC convention(`{lib name}_{arch}.{ext}`).
      val destName = {
        val arch =
          (`examples-native` / nativePlatform).value.takeWhile(_ != '-')
        val (base, ext) = nativeSrc.baseAndExt
        s"${base}_${arch}.${ext}"
      }
      val nativeDest =
        (Compile / resourceDirectory).value / "native" / destName
      IO.copy(Seq(nativeSrc -> nativeDest))
    },
    Compile / resourceGenerators += Def.task {
      copyNative.value.toSeq
    }.taskValue,
    run / fork := true,
    javaOptions ++= Seq(
      "--add-modules=jdk.incubator.foreign",
      "--enable-native-access=ALL-UNNAMED"
    ),
    libraryDependencies += "fr.hammons" %% "slinc-runtime" % "0.6.0"
  )
  .dependsOn(`examples-native`)

/*
 * A project to build native shared library
 */
lazy val `examples-native` = project
  .in(file("slinc-examples-native"))
  .enablePlugins(JniNative)
  .settings(
    publishTo := None,
    publishLocal := Def.task(()),
    nativeCompile / sourceDirectory := sourceDirectory.value
  )

lazy val root = project
  .in(file("."))
  .aggregate(slinc, examples, `examples-native`)
  .settings(
    publishTo := None,
    publishLocal := Def.task(())
  )
