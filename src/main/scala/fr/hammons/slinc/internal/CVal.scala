package fr.hammons.slinc.internal

import scala.reflect.TypeTest
import fr.hammons.slinc.Platform

opaque type CVal = Matchable

object CVal:
  private[slinc] def apply(a: Matchable): CVal = a
  extension (cval: CVal)
    def certainAs[A, P <: Platform](
        clazz: Class[A]
    )(using P, TypeRelation[P, ?, A]): A = cval.asInstanceOf[A]
    def as[A <: Matchable](using tt: TypeTest[Matchable, A]): LightOption[A] =
      cval match
        case a: A => LightOption(a)
        case _ =>
          println(s"${tt} didn't work on ${cval.getClass()}")
          LightOption.None
