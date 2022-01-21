import mill._, scalalib._, scalalib.publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

import $ivy.`com.github.lolgab::mill-mima_mill0.10:0.0.9`
import com.github.lolgab.mill.mima._

trait PublishableModule extends PublishModule with Mima {

   def mimaPreviousVersions = Seq("0.0.0-45-0647f5-DIRTY50d251bf")
   override def mimaCheckDirection: T[CheckDirection] = CheckDirection.Both

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
