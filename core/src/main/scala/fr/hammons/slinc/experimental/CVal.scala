package fr.hammons.slinc.experimental

import fr.hammons.slinc.Platform
import scala.reflect.TypeTest

opaque type CVal[PlatformMapping[_ <: Platform] <: Matchable] = Matchable

object CVal:

  trait Implementor:
    type Mapping[_ <: Platform] <: Matchable

    def cval[P <: Platform](using P)(v: Mapping[P]): CVal[Mapping] = v

  extension [PlatformMapping[_ <: Platform] <: Matchable](
      cval: CVal[PlatformMapping]
  )
    inline def extract[P <: Platform](using P): PlatformMapping[P] =
      if cval.isInstanceOf[PlatformMapping[P]] then
        cval.asInstanceOf[PlatformMapping[P]]
      else throw new Error("blah")
