import mill.define.Target
import mill._, scalalib._, modules._, scalalib.publish._

object v {
   val munit = "1.0.0-M1"
   val jmh = "1.33"
   val jnr = "2.2.3"
   val jna = "5.9.0"
}

object slinc extends ScalaModule with PublishModule {

   def moduleDeps = Seq(polymorphics)
   def scalaVersion = "3.1.0"
   def scalacOptions = Seq(
        "-deprecation",
        "-Wunused:all",
        "-unchecked",
        "-Xcheck-macros"
    )
   def publishVersion = "0.0.1"
   def pomSettings = PomSettings(
     description = "SLinC - Scala <-> C Interop",
     organization = "io.gitlab.mhammons",
     url = "https://gitlab.io/mhammons/slinc",
     licenses = Seq(License.`Apache-2.0`),
     versionControl = VersionControl.gitlab("mhammons", "slinc"),
     developers = Seq(
       Developer("mhammons", "Mark Hammons", "https://gitlab.io/mhammons")
     )
   )

   def sonatypeUri = "https://s01.oss.sonatype.org/"

   def forkArgs = Seq(
     "--add-modules",
     "jdk.incubator.foreign",
     "--enable-native-access",
     "ALL-UNNAMED"
   )

   import v._
   object test extends Tests {
      def ivyDeps = Agg(ivy"org.scalameta::munit::$munit")

      def testFramework = T("munit.Framework")

      def forkArgs = Seq(
        "--add-modules",
        "jdk.incubator.foreign",
        "--enable-native-access",
        "ALL-UNNAMED"
      )

      def nativeSource = T.sources { millSourcePath / "native" }
      def compileNative = T {
         val nativeFiles = nativeSource().head.path
         os.list(nativeFiles)
            .filter(_.last.endsWith(".c"))
            .flatMap { p =>
               val soLocation =
                  p / os.up / s"lib${p.last.stripSuffix(".c")}.so"
               os.proc(
                 "gcc",
                 "-shared",
                 "-o",
                 p / os.up / s"lib${p.last.stripSuffix(".c")}.so",
                 p
               ).call()
               List(PathRef(soLocation))
            }
         nativeFiles
      }

      override def compile = T {
         compileNative()
         super.compile()
      }
   }
}
object polymorphics extends ScalaModule with PublishModule {
   def scalaVersion = "2.13.6"
   def publishVersion = "0.0.1"
   def pomSettings = PomSettings(
     description =
        "Shim to use polymorphic methods from scala 3 <DON'T DEPEND ON ME>",
     organization = "io.gitlab.mhammons",
     url = "https://gitlab.io/mhammons/slinc",
     licenses = Seq(License.`Apache-2.0`),
     versionControl = VersionControl.gitlab("mhammons", "slinc"),
     developers = Seq(
       Developer("mhammons", "Mark Hammons", "https://gitlab.io/mhammons")
     )
   )

   def sonatypeUri = "https://gitlab.io/api/v4/projects/28891787/packages/maven"

}

object benchmarks extends ScalaModule {
   def moduleDeps = Seq(slinc)
   def scalaVersion = "3.1.0"

   def forkArgs = Seq(
     "--add-modules",
     "jdk.incubator.foreign",
     "--enable-native-access",
     "ALL-UNNAMED"
   )

   import v._
   def ivyDeps =
      Agg(
        ivy"org.openjdk.jmh:jmh-core:$jmh",
        ivy"com.github.jnr:jnr-ffi:$jnr",
        ivy"net.java.dev.jna:jna:$jna"
      )

   def jmhRun(args: String*) = T.command {
      val (_, resources) = generateBenchmarkSources()
      Jvm.runSubprocess(
        "org.openjdk.jmh.Main",
        classPath = (runClasspath() ++ generatorDeps())
           .map(_.path) ++ Seq(compileGeneratedSources().path, resources),
        mainArgs = args,
        workingDir = T.ctx.dest
      )
   }

   def compileGeneratedSources = T {
      val dest = T.ctx.dest
      val (sourcesDir, _) = generateBenchmarkSources()
      val sources = os.walk(sourcesDir).filter(os.isFile)
      os.proc(
        "javac",
        sources.map(_.toString),
        "-cp",
        (runClasspath() ++ generatorDeps()).map(_.path.toString).mkString(":"),
        "-d",
        dest
      ).call(dest)
      PathRef(dest)
   }

   def generateBenchmarkSources = T {
      val dest = T.ctx.dest

      val sourcesDir = dest / "jmh_sources"
      val resourcesDir = dest / "jmh_resources"

      os.remove.all(sourcesDir)
      os.makeDir.all(sourcesDir)
      os.remove.all(resourcesDir)
      os.makeDir.all(resourcesDir)

      Jvm.runSubprocess(
        "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator",
        (runClasspath() ++ generatorDeps()).map(_.path),
        jvmArgs = forkArgs(),
        mainArgs = Array(
          compile().classes.path,
          sourcesDir,
          resourcesDir,
          "default"
        ).map(_.toString)
      )
      (sourcesDir, resourcesDir)
   }

   def generatorDeps = resolveDeps(
     T { Agg(ivy"org.openjdk.jmh:jmh-generator-bytecode:$jmh") }
   )
}
