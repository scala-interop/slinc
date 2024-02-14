package fr.hammons.slinc.internal

import fr.hammons.slinc.Platform
import fr.hammons.slinc.Runtime
import compiletime.summonInline
import compiletime.summonAll
import quoted.*
import scala.deriving.Mirror

opaque type CIntegral <: CVal = CVal

object CIntegral:
  inline def orderingDerivation[A <: CIntegral](using r: Runtime): Ordered[A] =
    r.platform match
      case Platform.WinX64 =>
        val tr: TypeRelation[Platform.WinX64.type, A] = summonInline
        summonInline[Ordered[tr.Real]].asInstanceOf[Ordered[A]]
      case Platform.LinuxX64 =>
        val tr: TypeRelation[Platform.LinuxX64.type, A] = summonInline
        summonInline[Ordered[tr.Real]].asInstanceOf[Ordered[A]]
      case Platform.MacX64 =>
        val tr: TypeRelation[Platform.MacX64.type, A] = summonInline
        summonInline[Ordered[tr.Real]].asInstanceOf[Ordered[A]]

  inline def numericDerivation[A <: CIntegral](using r: Runtime)(using
      m: Mirror.SumOf[Platform]
  ): Numeric[A] =
    val x =
      summonAll[Tuple.Map[m.MirroredElemTypes, [P] =>> TypeRelation[P, A]]]

    ???

  // private def numericDerivationImpl[A <: CIntegral](using r: Runtime)

  private[slinc] def apply[P <: Platform, B](
      l: B
  )(using P, TypeRelation[P, ?] { type Real = B }): CIntegral = CVal(l)

  extension (cintegral: CIntegral)
    private[slinc] def asLong = cintegral.as[Long]

  extension [A <: CIntegral](a: A)
    def +(o: A)(using n: Numeric[A]) = n.plus(a, o)
