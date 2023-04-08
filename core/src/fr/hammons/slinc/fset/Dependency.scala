package fr.hammons.slinc.fset

import java.nio.file.Path

enum Dependency:
  case LibraryResource(path: String, specific: Boolean)
  case CResource(path: String)
  case PathLibrary(name: String)
  case FilePath(path: Path, specific: Boolean)
