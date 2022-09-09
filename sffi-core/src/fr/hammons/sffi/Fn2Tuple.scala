package fr.hammons.sffi

//translates a Function type into a tuple type
trait Fn2Tuple[Fn, Tuple <: NonEmptyTuple]

object Fn2Tuple:
  given [A]: Fn2Tuple[() => A, Tuple1[A]] with {}
  given [A, B]: Fn2Tuple[A => B, (A, B)] with {}
  given [A, B, C]: Fn2Tuple[(A, B) => C, (A, B, C)] with {}
  given [A, B, C, D]: Fn2Tuple[(A, B, C) => D, (A, B, C, D)] with {}
  given [A, B, C, D, E]: Fn2Tuple[(A, B, C, D) => E, (A, B, C, D, E)] with {}
  given [A, B, C, D, E, F]: Fn2Tuple[(A, B, C, D, E) => F, (A, B, C, D, E, F)]
    with {}
  given [A, B, C, D, E, F, G]
      : Fn2Tuple[(A, B, C, D, E, F) => G, (A, B, C, D, E, F, G)] with {}
  given [A, B, C, D, E, F, G, H]
      : Fn2Tuple[(A, B, C, D, E, F, G) => H, (A, B, C, D, E, F, G, H)] with {}
  given [A, B, C, D, E, F, G, H, I]
      : Fn2Tuple[(A, B, C, D, E, F, G, H) => I, (A, B, C, D, E, F, G, H, I)]
    with {}
  given [A, B, C, D, E, F, G, H, I, J]: Fn2Tuple[
    (A, B, C, D, E, F, G, H, I) => J,
    (A, B, C, D, E, F, G, H, I, J)
  ] with {}
  given [A, B, C, D, E, F, G, H, I, J, K]: Fn2Tuple[
    (A, B, C, D, E, F, G, H, I, J) => K,
    (A, B, C, D, E, F, G, H, I, J, K)
  ] with {}
  given [A, B, C, D, E, F, G, H, I, J, K, L]: Fn2Tuple[
    (A, B, C, D, E, F, G, H, I, J, K) => L,
    (A, B, C, D, E, F, G, H, I, J, K, L)
  ] with {}
  given [A, B, C, D, E, F, G, H, I, J, K, L, M]: Fn2Tuple[
    (A, B, C, D, E, F, G, H, I, J, K, L) => M,
    (A, B, C, D, E, F, G, H, I, J, K, L, M)
  ] with {}
