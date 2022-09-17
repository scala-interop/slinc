package fr.hammons.sffi

trait MethodIn[A]:
  def in(a: A)(using Allocator): Object

object MethodIn:
  given MethodIn[Int] with 
    def in(a: Int)(using Allocator) = a.asInstanceOf[Object]
