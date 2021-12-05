package io.gitlab.mhammons.slinc.components

import io.gitlab.mhammons.slinc.Struckt

enum Location:
   case Local(relativePath: String)
   case System(name: String)
   case Absolute(absolutePath: String)