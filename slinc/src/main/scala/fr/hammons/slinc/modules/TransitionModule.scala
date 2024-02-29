package fr.hammons.slinc.modules

import fr.hammons.slinc.*

type ArgumentTransition[A] = A => Any
type ReturnTransition[A] = Object => A
trait TransitionModule:
  /** Transitions a method argument to the appropriate format
    *
    * @param td
    *   Type Descriptor of the argument
    * @param value
    *   The argument to transition
    * @param alloc
    *   An allocator for allocating memory
    * @return
    *   The transitioned value
    */
  def methodArgument[A](td: TypeDescriptor, value: A, alloc: Allocator): Any

  /** Transitions an allocator into an appropriate format
    *
    * @param a
    *   An allocator
    * @return
    *   The transitioned value
    */
  def methodArgument(a: Allocator): Any

  def methodArgument(m: Mem): Any

  /** Transitions a return value into the Slinc format
    *
    * @param td
    *   Type descriptor of the return
    * @param value
    *   The return value
    * @return
    *   A slinc compatible data object
    */
  def methodReturn[A](td: TypeDescriptor, value: Object): A

  def addressReturn(value: Object): Mem

  def memReturn(value: Object): Mem

  def functionArgument[A](td: TypeDescriptor, value: Object): A =
    methodReturn[A](td, value)

  def functionReturn[A](td: TypeDescriptor, value: A, alloc: Allocator): Any =
    methodArgument[A](td, value, alloc)
