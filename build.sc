import os.Path
import $file.auxiliary.benchmark, benchmark.BenchmarksModule
import $file.auxiliary.publishable, publishable.PublishableModule
import $file.auxiliary.facadeGenerator, facadeGenerator.FacadeGenerationModule
import mill._, scalalib._, scalafmt._
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:`
import mill.contrib.buildinfo.BuildInfo
import com.github.lolgab.mill.mima._

import $ivy.`com.lihaoyi::mill-contrib-scoverage:`
import mill.contrib.scoverage.{ScoverageModule, ScoverageReport}

object scoverage extends BaseModule with ScoverageReport

trait BaseModule extends ScoverageModule with ScalafmtModule {
  def scalaVersion = "3.3.0-RC3"
  def scoverageVersion = "2.0.7"

  val munitVersion = "1.0.0-M7"
  val jmhV = "1.33"

  def ivyDeps = Agg(
    ivy"org.scala-lang::scala3-staging:${scalaVersion()}"
  )

  def scalacOptions = Seq(
    "-deprecation",
    "-Wunused:all",
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

  trait BaseTest extends ScoverageTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit:$munitVersion",
      ivy"org.scalameta::munit-scalacheck:$munitVersion"
    )

    val suffix = System.getProperty("os.name").split(" ")(0).toLowerCase match {
      case "windows" => ".dll"
      case _         => ".so"
    }

    def compileLibs = T {
      val destination = os.pwd / "libs" / s"test$suffix"

      T.log.info(s"Compiling ${destination.toString} from libs/test-code.c")

      val r = os.proc(
        "clang",
        "-v",
        "-shared",
        "-fvisibility=default",
        "-Os",
        "-o",
        s"libs/test_x64$suffix",
        "libs/test-code.c"
      ).call()

      if(r.exitCode != 0) {
        T.log.error(r.out.trim())
        throw new Exception("compilation failed!!")
      } else {
        T.log.info(r.out.trim())
      }

      PathRef(destination)
    }

    override def compile = T {
      compileLibs()
      generateResources()
      super.compile()
    }

    def generateResources = T {
      val nativeResources =
        resources().view.map(_.path / "native").filter(os.exists)
      val cFiles = nativeResources.flatMap(f => os.walk(f).filter(_.ext == "c")).toSet

      cFiles.map { file =>
        val destination =
          file / os.up / s"${file.last.stripSuffix(".c")}_x64$suffix"
        T.log.info(s"Compiling ${destination.toString} from ${file.toString}")
        val r = os.proc(
          "clang",
          "-v",
          "-shared",
          "-fvisibility=default",
          "-Os",
          "-o",
          destination.toString,
          file.toString
        ).call()

        if (r.exitCode != 0) {
          T.log.error(r.out.trim())
          throw new Exception("compilation failed")
        } else {
          T.log.info(r.out.trim())
        }
        
        PathRef(destination)
      }
    }

  }
}
object core
    extends BaseModule
    with PublishableModule
    with FacadeGenerationModule
    with BenchmarksModule {
  def javacOptions =
    super.javacOptions() ++ Seq("--release", "17")

  override def scalaDocOptions = T {
    super.scalaDocOptions() ++ Seq(
      // "-project-logo",
      // (millSourcePath / "docs" / "_assets" / "images" / "logo.svg").toString,
      "-project",
      "slinc"
    )
  }

  def pomSettings = pomTemplate("slinc-core")

  def specializationArity = 4

  object test extends BaseTest

  object benchmarks extends BaseModule {
    def moduleDeps = Seq(core)
    override def scalaVersion = core.scalaVersion()
    override def scalacOptions = core.scalacOptions

    object test extends BenchmarkSources {
      def jmhVersion = jmhV
      def forkArgs = super.forkArgs() ++ Seq(
        "--add-modules=jdk.incubator.foreign",
        "--enable-native-access=ALL-UNNAMED"
      )

    }
  }

}

object j17 extends BaseModule with PublishableModule with BenchmarksModule {
  def moduleDeps = Seq(core)
  def pomSettings = pomTemplate("slinc-java-17")

  def javacOptions =
    super.javacOptions() ++ Seq("--add-modules=jdk.incubator.foreign")

  object test extends BaseTest {
    def moduleDeps = super.moduleDeps ++ Seq(core.test)
    def forkArgs = super.forkArgs() ++ Seq(
      "--add-modules=jdk.incubator.foreign",
      "--enable-native-access=ALL-UNNAMED"
    )
  }

  // todo: remove this nasty hack needed for jacoco coverage reports
  object benchmarks extends BaseModule {
    def moduleDeps = Seq(j17)
    override def scalaVersion = j17.scalaVersion
    override def scalacOptions = j17.scalacOptions
    object test extends Benchmarks {
      def moduleDeps = Seq(j17.benchmarks, core.benchmarks.test)
      def jmhVersion = jmhV
      def forkArgs = super.forkArgs() ++ Seq(
        "--add-modules=jdk.incubator.foreign",
        "--enable-native-access=ALL-UNNAMED"
      )
    }
  }

}

object j19 extends BaseModule with PublishableModule with BenchmarksModule {
  def moduleDeps = Seq(core)
  def pomSettings = pomTemplate("slinc-java-19")

  object test extends BaseTest {
    def moduleDeps = super.moduleDeps ++ Seq(core.test)
    def forkArgs = super.forkArgs() ++ Seq(
      "--enable-preview",
      "--enable-native-access=ALL-UNNAMED"
    )
  }

  object benchmarks extends BaseModule {
    def moduleDeps = Seq(j19)
    override def scalaVersion = j19.scalaVersion
    override def scalacOptions = j19.scalacOptions

    object test extends Benchmarks {
      def moduleDeps = Seq(j19.benchmarks, core.benchmarks.test)
      def jmhVersion = jmhV
      def forkArgs = super.forkArgs() ++ Seq(
        "--enable-preview",
        "--enable-native-access=ALL-UNNAMED"
      )
    }

  }
}

object `runtime` extends BaseModule with PublishableModule {

  def pomSettings = pomTemplate("slinc-full")

  override def moduleDeps = Seq(j17, j19)

  object test extends Tests with TestModule.Munit {
    def ivyDeps = Agg(ivy"org.scalameta::munit:$munitVersion")

    def jvm = T.input { System.getProperty("java.version") }
    def moduleDeps = super.moduleDeps ++ Seq(core.test)
    def forkArgs = super.forkArgs() ++ Seq(
      "--enable-preview",
      "--enable-native-access=ALL-UNNAMED"
    ) ++ (if (jvm().startsWith("17")) {
            println("adding")
            Seq("--add-modules=jdk.incubator.foreign")
          } else Seq.empty)
  }
}
