package io.gitlab.mhammons.slinc

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
