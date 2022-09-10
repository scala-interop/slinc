package fr.hammons.sffi

import scala.language.dynamics
import scala.compiletime.{erasedValue, error, constValue, summonFrom}

trait PtrI[
    RawMem,
    Allocator, 
    LayoutInfo[A <: AnyKind] <: LayoutInfoI[?]#LayoutInfo[A],
    StructInfo[A] <: StructInfoI[LayoutInfo]#StructInfo[A],
    Deref[A] <: DerefI[RawMem]#Deref[A],
    Assign[A] <: AssignI[RawMem]#Assign[A],
    InTransition[A] <: InTransitionI[Allocator, LayoutInfo]#InTransition[A]
]:
  def alloc[A](layoutInfo: LayoutInfo[A], num: Long)(using Allocator): RawMem

  class Ptr[A](val mem: RawMem, val offset: Long) extends Dynamic:
    def as[B]: Ptr[B] = this.asInstanceOf[Ptr[B]]
    inline def nameExists[S <: Singleton & String, T <: Tuple](s: S): Unit =
      inline erasedValue[T] match
        case _: ((S, ?) *: ?) => ()
        case _: (? *: tail)   => nameExists[S, tail](s)
        case _: EmptyTuple    => error("Field " + s + " doesn't exist")

    inline def selectDynamic[S <: Singleton & String, B <: Tuple](inline s: S)(
        using ptrShape: PtrShape[A, B]
    ): Ptr[Tuple.Elem[Values[B], IndexOf[S, B]]] =
      inline if constValue[KeyExists[S, B]] then
        summonFrom {
          case si: StructInfo[A] =>
            Ptr[Tuple.Elem[Values[B], IndexOf[S, B]]](
              mem,
              si.offsets(constValue[IndexOf[S, B]])
            )
          case _ => error("Not sure how I got here...")
        }
      else
        error(
          "Member " + constValue[S] + " doesn't exist as part of this Pointer."
        )

    inline def applyDynamic[Name <: Singleton & String](inline name: Name)(
        o: Any
    ) =
      inline val reason = inline name match
        case "update"              => "cannot assign to a Ptr of type " + ""
        case "unary_!" | "toArray" => "cannot dereference a Ptr of type " + ""
        case _                     => "called a method that doesn't exist."
      error("Cannot access " + name + ". That's because you " + reason)

  object Ptr:
    def blank[A](using li: LayoutInfo[A])(using Allocator)(
        size: Long = 1
    ): Ptr[A] = Ptr(alloc(li, size), 0)

    extension [A](ptr: Ptr[A])(using assign: Assign[A])
      def update(a: A) = assign.assign(ptr.mem, ptr.offset, a)

    extension [A](ptr: Ptr[A])(using deref: Deref[A])
      def `unary_!` = deref.deref(ptr.mem, ptr.offset)
      def toArray(size: Long) = deref.toArray(ptr.mem, 0, size)

  extension (ptr: Ptr[Byte]) def asString: String

  given ptrInfo: LayoutInfo[Ptr]

  given refinedPtrInfo[A]: LayoutInfo[Ptr[A]] =
    ptrInfo.asInstanceOf[LayoutInfo[Ptr[A]]]

  given ptrInTransition[A]: InTransition[Ptr[A]]

