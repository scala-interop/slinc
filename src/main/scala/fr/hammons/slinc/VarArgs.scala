package fr.hammons.slinc

trait VarArgs:
  def get[A](using DescriptorOf[A]): A
  private[slinc] def mem: Mem
  def skip[A](using DescriptorOf[A]): Unit
  def copy(): VarArgs
