package io.gitlab.mhammons.slinc.platforms

import io.gitlab.mhammons.slinc.components.Immigrator
import io.gitlab.mhammons.slinc.components.NativeInfo

trait IntegralType[T, U] {
   extension (t: T) def %(rhs: T): T
   given ni_int: NativeInfo[T]
   given int_Imm: Immigrator[T]
}
