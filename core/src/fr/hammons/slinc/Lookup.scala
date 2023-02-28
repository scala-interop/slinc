package fr.hammons.slinc

import java.nio.file.Paths

trait Lookup(libraryLocation: LibraryLocation):
  def lookup(name: String): Object

  def lookupError(name: String): Error = libraryLocation match
    case LibraryLocation.Standard =>
      Error(s"Failed to load symbol $name from the standard library.")
    case LibraryLocation.Resource(name,candidates) =>
      Error(
        s"Failed to load symbol $name from resource. This could be caused by resource collision. Is the resource name unique enough?"
      )
    case LibraryLocation.Local(location) =>
      val absPath = Paths.get(location).nn.toAbsolutePath.nn
      Error(s"Failed to load symbol $name from local path $absPath")
    case LibraryLocation.Path(libName) =>
      Error(s"Failed to load symbol $name from library $libName")
