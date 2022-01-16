import mill._, scalalib._, scalalib.publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.2-6-e3df5d`
import de.tobiasroeser.mill.vcs.version.VcsVersion

import $ivy.`com.github.lolgab:mill-mima_mill0.10.0-M5_2.13:0.0.8`
import com.github.lolgab.mill.mima._

trait PublishableModule extends PublishModule with Mima {

   def mimaPreviousVersions = Seq("0.0.0-45-0647f5-DIRTY50d251bf")
   override def mimaCheckDirection = CheckDirection.Both

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

   override def sonatypeUri: String =
      "https://s01.oss.sonatype.org/service/local"
   override def sonatypeSnapshotUri: String = sonatypeUri
   def publishVersion: T[String] = VcsVersion.vcsState().format()
}
