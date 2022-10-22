package fr.hammons.slinc

import scala.compiletime.ops.int.S

trait LocationInCap[B[_], C <: Capabilities, N <: Int]

inline given [B[_], Caps <: Capabilities]: LocationInCap[B, B *::: Caps, 0] with {}
inline given [B[_], A[_], Caps <: Capabilities, N <: Int](using l: LocationInCap[B, Caps, N]): LocationInCap[B, A *::: Caps, S[N]] with {}
