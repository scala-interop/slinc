package fr.hammons.sffi

import java.lang.invoke.MethodHandle

trait WBasics:
  type Context
  type Scope
  type RawMem
  type Allocator
  type Pointer[A]
