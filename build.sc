import mill.define.Target
import mill._, scalalib._, modules._

object core extends ScalaModule {
  def moduleDeps = Seq(jworkaround, polymorphics)
  def scalaVersion = "3.0.2"

  def forkArgs = Seq(
    "--add-modules",
    "jdk.incubator.foreign",
    "--enable-native-access",
    "ALL-UNNAMED"
  )

  override def scalacOptions = Seq("-Xsemanticdb")
}

object jworkaround extends JavaModule {}

object polymorphics extends ScalaModule {
  def scalaVersion = "2.13.6"
}
