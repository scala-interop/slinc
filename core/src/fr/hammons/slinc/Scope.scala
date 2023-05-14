package fr.hammons.slinc

sealed trait Scope:
  def apply[A](fn: Allocator ?=> A): A

trait ConfinedScope extends Scope:
  def apply[A](fn: Allocator ?=> A): A

trait TempScope extends Scope:
  def apply[A](fn: Allocator ?=> A): A

trait GlobalScope extends Scope:
  def apply[A](fn: Allocator ?=> A): A

trait SharedScope extends Scope:
  def apply[A](fn: (Allocator) ?=> A): A

trait InferredScope extends Scope:
  def apply[A](fn: Allocator ?=> A): A

object Scope:
  def temp(using t: TempScope): TempScope = t
  def confined(using c: ConfinedScope): ConfinedScope = c
  def global(using g: GlobalScope): GlobalScope = g
  def shared(using s: SharedScope): SharedScope = s
  def inferred(using i: InferredScope): InferredScope = i

class ScopeI(platformSpecific: ScopeI.PlatformSpecific):
  given TempScope = platformSpecific.createTempScope
  given GlobalScope = platformSpecific.createGlobalScope
  given ConfinedScope = platformSpecific.createConfinedScope
  given SharedScope = platformSpecific.createSharedScope
  given InferredScope = platformSpecific.createInferredScope

object ScopeI:
  trait PlatformSpecific:
    def createTempScope: TempScope
    def createGlobalScope: GlobalScope
    def createConfinedScope: ConfinedScope
    def createSharedScope: SharedScope
    def createInferredScope: InferredScope
    def nullPtr[A]: Ptr[A]
