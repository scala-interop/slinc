package fr.hammons.slinc.fset

import java.nio.file.Path

enum Dependency:
  case LibraryResource(path: Path)
  case CResource(path: Path)