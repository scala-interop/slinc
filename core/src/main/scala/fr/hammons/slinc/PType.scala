package fr.hammons.slinc

import fr.hammons.slinc.internal.ast.PTypeDescriptor

trait PType {
  inline def typeDescription: PTypeDescriptor
}

class PInt extends PType {
  inline def typeDescription: PTypeDescriptor =
    PTypeDescriptor.IntTypeDescriptor
}
