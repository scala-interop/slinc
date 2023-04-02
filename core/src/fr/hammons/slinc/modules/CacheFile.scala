package fr.hammons.slinc.modules

import java.nio.file.Path

final case class CacheFile(origin: Path, cachePath: Path, updated: Boolean)
