package fr.hammons.slinc.internal.ast

enum PTypeDescriptor:
  case IntTypeDescriptor
  case FloatTypeDescriptor
  case DoubleTypeDescriptor
  case ShortTypeDescriptor
  case ByteTypeDescriptor
  case LongTypeDescriptor
  case PointerTypeDescriptor
  case StructTypeDescriptor(fieldDescriptors: (String, PTypeDescriptor)*)
  case FixSizedArrayTypeDescriptor(containedType: PTypeDescriptor, number: Int)
  case UnionTypeDescriptor(possibleTypes: PTypeDescriptor*)
  case VoidTypeDescriptor

//allowed c type definitions:
// - predefined
// - alias
// - struct
// - array
// - union
// - pointer
// - void
