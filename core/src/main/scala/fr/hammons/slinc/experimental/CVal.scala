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
    def extract[P <: Platform](using p: P)(using
        TypeTest[Matchable, PlatformMapping[P]]
    ): PlatformMapping[P] = cval match
      case v: PlatformMapping[P] => v
      case _                     => throw new Error("NOOOOOO")
