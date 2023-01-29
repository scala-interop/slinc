import mill._, scalalib._, scalalib.publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

import $ivy.`com.github.lolgab::mill-mima_mill0.10:0.0.12`
import com.github.lolgab.mill.mima._
import upickle.default._

trait PublishableModule extends PublishModule with Mima {
   implicit val checkDirectionW: ReadWriter[CheckDirection] =
      upickle.default.macroRW[CheckDirection]

   override def artifactName = T("slinc-" + millModuleSegments.parts.mkString("-"))
   def mimaPreviousVersions = Seq(
     os.proc("git", "describe", "--tags", "--abbrev=0")
        .call(cwd = os.pwd)
        .out
        .trim
   )
   def direction = T.input { System.getProperty("mima.direction") }
   override def mimaCheckDirection: T[CheckDirection] =
      T.input[CheckDirection] {
         if (direction() == "backward") {
            T.ctx.log.info(
              s"Doing backwards compatibility check with ${mimaPreviousVersions().head}"
            )
            CheckDirection.Backward
         } else if (direction() == "forward") {
            T.ctx.log.info(
              s"Doing source compatibility check with ${mimaPreviousVersions().head}"
            )
            CheckDirection.Forward
         } else {
            T.ctx.log.info(
              s"Doing a source and binary compatibility check with ${mimaPreviousVersions().head}"
            )
            CheckDirection.Both
         }
      }

   def pomTemplate(description: String) = PomSettings(
     description = description,
     organization = "fr.hammons",
     url = "https://github.com/markehammons/slinc",
     licenses = Seq(License.`AGPL-3.0-or-later`,License.`LGPL-3.0-or-later`),
     versionControl = VersionControl.github("markehammons", "slinc"),
     developers = Seq(
       Developer("markehammons", "Mark Hammons", "https://github.com/markehammons")
     )
   )

   override def sonatypeUri: String =
      "https://s01.oss.sonatype.org/service/local"
   override def sonatypeSnapshotUri: String = sonatypeUri
   def publishVersion: T[String] = VcsVersion.vcsState().format()
}
