package fr.hammons.slinc

class VarArgsBuilder(val vs: Seq[Variadic]):
  def add(additionalVs: Variadic*): VarArgsBuilder = new VarArgsBuilder(
    vs ++ additionalVs
  )
  def build(using alloc: Allocator): VarArgs = alloc.makeVarArgs(this)

object VarArgsBuilder:
  def apply(vs: Variadic*): VarArgsBuilder = new VarArgsBuilder(vs)
