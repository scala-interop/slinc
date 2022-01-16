package io.gitlab.mhammons.slinc.components

/** Provides proof that A is a Function type
  */
trait Fn[A]

object Fn:
   given fn0[A]: Fn[() => A] with {}
   given fn1[A, B]: Fn[B => A] with {}
   given fn2[A, B, C]: Fn[(B, C) => A] with {}
   given fn3[A, B, C, D]: Fn[(B, C, D) => A] with {}
   given fn4[A, B, C, D, E]: Fn[(B, C, D, E) => A] with {}
   given fn5[A, B, C, D, E, F]: Fn[(B, C, D, E, F) => A] with {}
   given fn6[A, B, C, D, E, F, G]: Fn[(B, C, D, E, F, G) => A] with {}
   given fn7[A, B, C, D, E, F, G, H]: Fn[(B, C, D, E, F, G, H) => A] with {}
   given fn8[A, B, C, D, E, F, G, H, I]: Fn[(B, C, D, E, F, G, H, I) => A]
      with {}
   given fn9[A, B, C, D, E, F, G, H, I, J]: Fn[(B, C, D, E, F, G, H, I, J) => A]
      with {}
   given fn10[A, B, C, D, E, F, G, H, I, J, K]
       : Fn[(B, C, D, E, F, G, H, I, J, K) => A] with {}
   given fn11[A, B, C, D, E, F, G, H, I, J, K, L]
       : Fn[(B, C, D, E, F, H, I, J, K, L) => A] with {}
   given fn12[A, B, C, D, E, F, G, H, I, J, K, L, M]
       : Fn[(B, C, D, E, F, G, H, I, J, K, L, M) => A] with {}
   given fn13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]
       : Fn[(B, C, D, E, F, G, H, I, J, K, L, M, N) => A] with {}
   given fn14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]
       : Fn[(B, C, D, E, F, G, H, I, J, K, L, M, N, O) => A] with {}
   given fn15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]
       : Fn[(B, C, D, E, F, G, H, I, J, K, L, M, N, O, P) => A] with {}
   given fn16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]
       : Fn[(B, C, D, E, F, G, H, I, K, L, M, N, O, P, Q) => A] with {}
   given fn17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]
       : Fn[(B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R) => A] with {}
   given fn18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]
       : Fn[(B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) => A] with {}
   given fn19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]
       : Fn[(B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T) => A]
      with {}
   given fn20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]
       : Fn[(B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U) => A]
      with {}
   given fn21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]
       : Fn[
         (B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V) => A
       ] with {}
   given fn22[
       A,
       B,
       C,
       D,
       E,
       F,
       G,
       H,
       I,
       J,
       K,
       L,
       M,
       N,
       O,
       P,
       Q,
       R,
       S,
       T,
       U,
       V,
       W
   ]: Fn[(B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, S, T, U, V, W) => A]
      with {}
