package io.gitlab.mhammons.slinc

import java.nio.file.Paths

/** Location information about the library you wish to bind
  */
@deprecated("Use one of the LibraryLocation types instead", "v0.1.1")
enum Location:
   /** Local .so file
     * @param relativePath
     *   The path relative to this program where the .so file is.
     */
   case Local(relativePath: String)

   /** System library
     * @param name
     *   The name of the library to bind to
     */
   case System(name: String)

   /** Absolute .so file path
     * @param absolutePath
     *   The absolute path of the .so file being bound
     */
   case Absolute(absolutePath: String)

/** Information about a non-standard library
  */
sealed trait LibraryLocation

/** Indicates an absolute location for the library
  * @param path
  *   The absolute path to the library in question
  */
trait AbsoluteLocation(path: String) extends LibraryLocation:
   // override this with the absolute path to the library
   System.load(path)

/** Indicates a relative location for the library
  * @param path
  *   The path to the library relative to the current working directory of the
  *   program
  */
trait LocalLocation(path: String) extends LibraryLocation:
   System.load(Paths.get(path).toAbsolutePath.toString)

/** Denotes a library that is on the library path of the system
  * @param name
  *   The name of the library, sans lib or .so prefixes/suffixes.
  */
trait SystemLibrary(name: String) extends LibraryLocation:
   System.loadLibrary(name)
