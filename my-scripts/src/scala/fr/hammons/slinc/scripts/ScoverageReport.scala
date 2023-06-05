package fr.hammons.slinc.scripts

import bleep.BleepScript
import bleep.Started
import bleep.Commands
import bleep.rewrites.BuildRewrite
import bleep.PathOps

import scoverage.reporter.CoverageAggregator
import scoverage.reporter.ScoverageXmlWriter
import java.io.File
import java.nio.file.Files
import scala.jdk.CollectionConverters.*
import scala.jdk.StreamConverters.*
import java.nio.file.Paths

object ScoverageReport extends BleepScript("ScoverageReport"):
  override val rewrites: List[BuildRewrite] = List(AddCoverage)
  def run(started: Started, commands: Commands, args: List[String]): Unit =

    val testProjects = started.build.explodedProjects
      .filter((_, p) => p.isTestProject.getOrElse(false))
      .keySet
      .toList

    commands.compile(
      started.build.explodedProjects.keys.toList
    )

    commands.test(
      started.build.explodedProjects
        .filter((_, p) => p.isTestProject.exists(identity))
        .keys
        .toList
    )

    val projectPaths = started.build.explodedProjects
      .filter((_, p) => !p.isTestProject.getOrElse(false))
      .map((cp, p) => started.buildPaths.project(cp, p))


    projectPaths
      .flatMap: p =>
        Files
          .list(p.targetDir / "scoverage-reports")
          .nn
          .toScala(List)
      .filter(_.endsWith("scoverage.coverage"))
      .foreach: df =>
        val mod = Files
          .readAllLines(df)
          .nn
          .asScala
          .map: s =>
            if s.startsWith("../") then 
              started.buildPaths.buildDir.toAbsolutePath().nn.relativize(Paths.get(s.stripPrefix(".."))).nn.toString()
            else s

        Files.write(df, mod.mkString("\n").getBytes())

    val coverage = projectPaths
      .map(p => p.sourcesDirs.all -> p.targetDir / "scoverage-reports")
      .map((sources, data) =>
        (
          sources,
          data,
          CoverageAggregator
            .aggregate(
              Seq(data.toFile().nn),
              started.buildPaths.buildDir.toFile().nn
            )
        )
      )

    coverage.foreach: (sources, dataDir, coverage) =>
      ScoverageXmlWriter(
        sources.map(_.toFile().nn).toSeq,
        dataDir.toFile().nn,
        false,
        None
      ).write(coverage.get)
