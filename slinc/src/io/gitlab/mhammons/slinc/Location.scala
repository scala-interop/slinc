package io.gitlab.mhammons.slinc.components

enum Location:
   case Local(relativePath: String)
   case System(name: String)
   case Absolute(absolutePath: String)
