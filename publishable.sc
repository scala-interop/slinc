import mill._, scalalib._, scalalib.publish._

trait PublishableModule extends PublishModule {
   def pomTemplate(description: String) = PomSettings(
     description = description,
     organization = "io.gitlab.mhammons",
     url = "https://gitlab.io/mhammons/slinc",
     licenses = Seq(License.`Apache-2.0`),
     versionControl = VersionControl.gitlab("mhammons", "slinc"),
     developers = Seq(
       Developer("mhammons", "Mark Hammons", "https://gitlab.io/mhammons")
     )
   )

   override def sonatypeUri: String = "https://ss01.oss.sonatype.org/"
   override def sonatypeSnapshotUri: String = sonatypeUri
   def publishVersion: T[String] = "0.0.1"
}
