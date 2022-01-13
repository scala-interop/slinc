import mill._, scalalib._, modules._
import mill.define.Target
import os.Path

trait BenchmarksModule extends Module with ScalaModule {
   private def self = this

   trait Benchmark extends ScalaModule {
      override def scalaVersion: T[String] = self.scalaVersion
      override def moduleDeps = Seq(self)
      override def scalacOptions: Target[Seq[String]] = self.scalacOptions

      override def forkArgs: Target[Seq[String]] = self.forkArgs
      override def defaultCommandName(): String = "jmhRun"
      def ivyDeps =
         Agg(
           ivy"org.openjdk.jmh:jmh-core:${jmhVersion()}"
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
           (runClasspath() ++ generatorDeps())
              .map(_.path.toString)
              .mkString(":"),
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
        T { Agg(ivy"org.openjdk.jmh:jmh-generator-bytecode:${jmhVersion()}") }
      )
      def jmhVersion: Target[String]
   }

}
