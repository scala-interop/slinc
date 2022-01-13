import os.Path
import $file.benchmark
import $file.publishable
import mill.define.Target
import mill._, scalalib._, modules._, scalalib.publish._

object v {
   val munit = "1.0.0-M1"
   val jmh = "1.33"
   val jnr = "2.2.3"
   val jna = "5.9.0"
}

object slinc
    extends ScalaModule
    with publishable.PublishableModule
    with benchmark.BenchmarksModule {
   def moduleDeps = Seq(polymorphics)
   def scalaVersion = "3.1.0"
   def pomSettings = pomTemplate("SLinC - Scala <-> C Interop")

   def scalacOptions = Seq(
     "-deprecation",
     "-Wunused:all",
     "-unchecked",
     "-Xcheck-macros",
     "-Xprint-suspension",
     "-Xsemanticdb"
   )

   def forkArgs = Seq(
     "--add-modules",
     "jdk.incubator.foreign",
     "--enable-native-access",
     "ALL-UNNAMED"
   )

   object test extends Tests with TestModule.Munit {
      def scalacOptions = Seq(
        "-deprecation",
        "-Wunused:all",
        "-unchecked",
        "-Xcheck-macros",
        "-Xprint-suspension",
        "-Xsemanticdb"
      )

      def forkArgs = Seq(
        "--add-modules",
        "jdk.incubator.foreign",
        "--enable-native-access",
        "ALL-UNNAMED"
      )

      def ivyDeps = Agg(ivy"org.scalameta::munit::${v.munit}")
      def nativeSource = T.sources { millSourcePath / "native" }
      def compileNative = T {
         val nativeFiles = nativeSource().head.path
         val cFiles = os.list(nativeFiles).filter(_.last.endsWith(".c"))
         cFiles
            .flatMap { p =>
               val soLocation =
                  p / os.up / s"lib${p.last.stripSuffix(".c")}.so"
               os.proc(
                 "gcc",
                 "-shared",
                 "-fPIC",
                 "-o",
                 p / os.up / s"lib${p.last.stripSuffix(".c")}.so",
                 p
               ).call()
               List(PathRef(soLocation))
            }
         cFiles.map(PathRef(_))
      }

      override def compile = T {
         compileNative()
         super.compile()
      }
   }

   object bench extends Benchmark {
      def jmhVersion = "1.33"
      override def ivyDeps = super.ivyDeps() ++ Seq(
        ivy"com.github.jnr:jnr-ffi:${v.jnr}",
        ivy"net.java.dev.jna:jna:${v.jna}"
      )
   }
}
object polymorphics extends ScalaModule with publishable.PublishableModule {
   def scalaVersion = "2.13.7"
   def publishVersion = "0.0.1"
   def pomSettings = pomTemplate(
     "Shim to use polymorphic methods from scala 3 <DON'T DEPEND ON ME>"
   )
   def scalacPluginIvyDeps = Agg(ivy"org.scalameta:::semanticdb-scalac:4.4.32")
   def scalacOptions = Seq("-Yrangepos")
}
