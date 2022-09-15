package fr.hammons.sffi

trait IsFunction[A]

object IsFunction:
  given arity0[Z]: IsFunction[() => Z] with {}
  given arity1[A,Z]: IsFunction[A => Z] with {}
  given arity2[A,B,Z]: IsFunction[(A,B) => Z] with {}
  given arity3[A,B,C,Z]: IsFunction[(A,B,C) => Z] with {}