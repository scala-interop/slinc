package fr.hammons.slinc.fnutils

sealed trait Fn[A, B <: Tuple, C]

object Fn:
  given [Z]: Fn[() => Z, EmptyTuple, Z] with {}
  given [A, Z]: Fn[A => Z, Tuple1[A], Z] with {}
  given [A, B, Z]: Fn[(A, B) => Z, (A, B), Z] with {}
  given [A, B, C, Z]: Fn[(A, B, C) => Z, (A, B, C), Z] with {}
  given [A, B, C, D, Z]: Fn[(A, B, C, D) => Z, (A, B, C, D), Z] with {}
  given [A, B, C, D, E, Z]: Fn[(A, B, C, D, E) => Z, (A, B, C, D, E), Z] with {}
  given [A, B, C, D, E, F, Z]
      : Fn[(A, B, C, D, E, F) => Z, (A, B, C, D, E, F), Z] with {}
  given [A, B, C, D, E, F, G, Z]
      : Fn[(A, B, C, D, E, F, G) => Z, (A, B, C, D, E, F, G), Z] with {}
  given [A, B, C, D, E, F, G, H, Z]
      : Fn[(A, B, C, D, E, F, G, H) => Z, (A, B, C, D, E, F, G, H), Z] with {}
  given [A, B, C, D, E, F, G, H, I, Z]
      : Fn[(A, B, C, D, E, F, G, H, I) => Z, (A, B, C, D, E, F, G, H, I), Z]
  with {}

  given [A, B, C, D, E, F, G, H, I, J, Z]: Fn[
    (A, B, C, D, E, F, G, H, I, J) => Z,
    (A, B, C, D, E, F, G, H, I, J),
    Z
  ] with {}

  given [A, B, C, D, E, F, G, H, I, J, K, Z]: Fn[
    (A, B, C, D, E, F, G, H, I, J, K) => Z,
    (A, B, C, D, E, F, G, H, I, J, K),
    Z
  ] with {}
