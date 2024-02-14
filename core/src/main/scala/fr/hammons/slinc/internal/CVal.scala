package fr.hammons.slinc.internal

import scala.reflect.TypeTest
import fr.hammons.slinc.Platform

opaque type CVal = Matchable

object CVal:
  private[slinc] def apply[B, P <: Platform](using P)(using
      tr: TypeRelation[P, B]
  )(a: tr.Real): CVal = a
  extension [B](cval: CVal)
    def certainAs[P <: Platform](using P)(using
        tr: TypeRelation[P, ?]
    ): tr.Real = cval.asInstanceOf[tr.Real]
    def as[A <: Matchable](using tt: TypeTest[Matchable, A]): LightOption[A] =
      cval match
        case a: A => LightOption(a)
        case _ =>
          println(s"${tt} didn't work on ${cval.getClass()}")
          LightOption.None
