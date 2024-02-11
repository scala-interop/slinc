package fr.hammons.slinc

import fr.hammons.slinc.internal.ast.PTypeDescriptor
import quoted.*

trait Transformed[A, B <: PType](conversion: A => B) {

  inline def p: PTypeDescriptor
}

trait Transform[A, B]:
  def transform(a: A): B
  def reverse(b: B): A

object Transform:
  given Transform[Boolean, Byte] with
    def transform(a: Boolean): Byte = if a == true then 1 else 0
    def reverse(b: Byte): Boolean = if b != 0 then true else false
