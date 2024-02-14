package fr.hammons.slinc.internal

enum FieldGenerator:
  case StandardField(name: String)
  case StructField(name: String, fieldGenerators: Array[FieldGenerator])
