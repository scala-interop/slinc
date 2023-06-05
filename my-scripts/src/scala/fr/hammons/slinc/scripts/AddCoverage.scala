package fr.hammons.slinc.scripts

import bleep.rewrites.BuildRewrite
import bleep.model.BuildRewriteName
import bleep.model.Build
import bleep.model.CrossProjectName
import bleep.model.Project
import bleep.model.Options
import bleep.PathOps
import java.nio.file.Paths
import java.nio.file.Files

object AddCoverage extends BuildRewrite:
  override val name: BuildRewriteName = BuildRewriteName("add-coverage")

  override protected def newExplodedProjects(
      oldBuild: Build
  ): Map[CrossProjectName, Project] =
    val buildTarget = Paths.get(".bleep").nn / "builds" / name.value / ".bloop"
    oldBuild.explodedProjects
      .map: (crossName, p) =>
        if !p.isTestProject.getOrElse(false) then
          val projectTarget =
            buildTarget / crossName.value / "scoverage-reports"

          Files.createDirectories(projectTarget)
          crossName -> p.copy(scala =
            p.scala.map(s =>
              s.copy(options =
                s.options.union(
                  Options.parse(
                    List(
                      s"-coverage-out:${projectTarget.toFile().nn.getAbsoluteFile().nn.toString()}/"
                    ),
                    None
                  )
                )
              )
            )
          )
        else crossName -> p
