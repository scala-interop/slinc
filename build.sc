import mill.define.Target
import mill._, scalalib._, modules._

object v {
   val cats = "2.6.1"
   val munit = "1.0.0-M1"
   val jmh = "1.33"
   val jnr = "2.2.3"
   val jna = "5.9.0"
}

object core extends ScalaModule {

   def moduleDeps = Seq(jworkaround, polymorphics)
   def scalaVersion = "3.1.0"
   def publishVersion = "0.0.1"

   def forkArgs = Seq(
     "--add-modules",
     "jdk.incubator.foreign",
     "--enable-native-access",
     "ALL-UNNAMED"
   )

   import v._
   def ivyDeps = Agg(
     ivy"org.typelevel::cats-core:$cats",
     ivy"org.typelevel::cats-free:$cats"
   )

   object test extends Tests {
      def ivyDeps = Agg(ivy"org.scalameta::munit::$munit")

      def testFramework = T("munit.Framework")

      def forkArgs = Seq(
        "--add-modules",
        "jdk.incubator.foreign",
        "--enable-native-access",
        "ALL-UNNAMED"
      )
   }

   override def scalacOptions = Seq("-Xsemanticdb")
}

object jworkaround extends JavaModule {}

object polymorphics extends ScalaModule {
   def scalaVersion = "2.13.6"
}

object benchmarks extends ScalaModule {
   def moduleDeps = Seq(core)
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
