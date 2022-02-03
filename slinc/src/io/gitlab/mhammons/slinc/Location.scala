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

/** Indicates an absolute location for the library */
trait AbsoluteLocation extends LibraryLocation:
   // override this with the absolute path to the library
   def path: String
   System.load(path)

trait LocalLocation extends LibraryLocation:
   // override this with the relative (to the working directory) path to the library
   def path: String
   System.load(Paths.get(path).toAbsolutePath.toString)

trait SystemLibrary extends LibraryLocation:
   // override this with the library's name (sans lib or .so prefix and suffix)
   def name: String
   System.loadLibrary(name)
