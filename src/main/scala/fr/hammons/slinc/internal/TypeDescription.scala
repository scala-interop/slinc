package fr.hammons.slinc.internal

import fr.hammons.slinc.internal.ast.PTypeDescriptor

private[slinc] trait TypeDescription[T]:
  val descriptor: PTypeDescriptor

object TypeDescription:
  given TypeDescription[Byte] with
    val descriptor = PTypeDescriptor.ByteTypeDescriptor
