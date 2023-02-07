package fr.hammons.slinc.modules

import fr.hammons.slinc.TypeDescriptor
import fr.hammons.slinc.Allocator

trait TransitionModule:
  def methodArgument[A](td: TypeDescriptor, value: A): Object
  def methodArgument(a: Allocator): Object
  def methodReturn[A](td: Option[TypeDescriptor], value: A): Object
