package fr.hammons.slinc.modules

import java.lang.invoke.MethodHandle
import fr.hammons.slinc.*

trait LinkageModule:
  type CSymbol
  def defaultLookup(name: String): Option[CSymbol]
  def loaderLookup(name: String): Option[CSymbol]
  def getDowncall(
      descriptor: CFunctionDescriptor,
      varArgs: Seq[Variadic]
  ): MethodHandle
  // todo: stop-gap for method handles, to get rid of ASAP
  lazy val tempScope: Scope
