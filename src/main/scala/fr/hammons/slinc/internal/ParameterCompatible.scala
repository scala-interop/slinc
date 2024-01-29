package fr.hammons.slinc.internal

import fr.hammons.slinc.Struct

//void cannot be a parameter
//array cannot be a parameter
protected[slinc] trait ParameterCompatible[T]

object ParameterCompatible:
  given [S <: Struct]: ParameterCompatible[S] with {}
