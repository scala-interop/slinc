package fr.hammons.slinc

import container.*

type Variadic = Container[DescriptorOf *::: End]

object Variadic:
  inline def apply(inline a: Any): Variadic =
    Container[DescriptorOf *::: End](a)
