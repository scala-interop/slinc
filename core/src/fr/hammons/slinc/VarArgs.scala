package fr.hammons.slinc

trait VarArgs:
  def get[A](using DescriptorOf[A]): A
  def ptr: Ptr[Nothing]
  def skip[A](using DescriptorOf[A]): Unit
  def copy(): VarArgs
