package io.gitlab.mhammons.slinc

import java.nio.file.Paths

/** Location information about the library you wish to bind
  */
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

trait Location2(loc: Location):
   loc match
      case Location.Absolute(path) => System.load(path)
      case Location.Local(relPath) =>
         System.load(Paths.get(relPath).toAbsolutePath.toString)
      case Location.System(name) => System.loadLibrary(name)
