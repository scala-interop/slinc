package fr.hammons.sffi

class FFI2[Context, Allocator, RawMem, Scope](
    val deref: DerefI[RawMem],
    val assign: AssignI[RawMem],
    val layoutInfo: LayoutInfoI[Context],
    val structInfo: StructInfoI[layoutInfo.LayoutInfo],
    val inTransitionI: InTransitionI[Allocator, layoutInfo.LayoutInfo],
    val ptr: PtrI[
      RawMem,
      Allocator,
      layoutInfo.LayoutInfo,
      structInfo.StructInfo,
      deref.Deref,
      assign.Assign,
      inTransitionI.InTransition
    ],
    val allocatable: AllocatableI[Scope, Allocator, RawMem],
    val Scope: ScopeSingleton[Scope, Allocator]
):
  export allocatable.Allocatable

  def toNative[A](a: A)(using
      allocatable: Allocatable[A]
  )(using Scope, Allocator) = ptr.Ptr[A](allocatable(a),0)


