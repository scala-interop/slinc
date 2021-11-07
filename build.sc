import mill.define.Target
import mill._, scalalib._, modules._

object v {
   val cats = "2.6.1"
   val munit = "1.0.0-M1"
}

object core extends ScalaModule {
   def moduleDeps = Seq(jworkaround, polymorphics)
   def scalaVersion = "3.1.0"

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
