package fr.hammons.slinc

import fr.hammons.slinc.ScopeI.PlatformSpecific


sealed trait Scope:
  def apply[A](fn: Allocator ?=> A): A

trait ConfinedScope extends Scope: 
  def apply[A](fn: Allocator ?=> A): A

trait TempScope extends Scope:
  def apply[A](fn: Allocator ?=> A): A

trait GlobalScope extends Scope:
  def apply[A](fn: Allocator ?=> A): A

object Scope:
  def temp(using t: TempScope): TempScope = t
  def confined(using c: ConfinedScope): ConfinedScope = c
  def global(using g: GlobalScope): GlobalScope = g

class ScopeI(platformSpecific: ScopeI.PlatformSpecific):
  given TempScope = platformSpecific.createTempScope
  given GlobalScope = platformSpecific.createGlobalScope
  given ConfinedScope = platformSpecific.createConfinedScope

object ScopeI:
  trait PlatformSpecific:
    def createTempScope: TempScope
    def createGlobalScope: GlobalScope
    def createConfinedScope: ConfinedScope

trait Allocator:
  def allocate(layout: DataLayout): Mem
  def upcall[Fn](function: Fn): Mem
  def base: Object 
