package fr.hammons.sffi

import scala.compiletime.summonInline
import scala.compiletime.erasedValue

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.targetName

trait UpcallI:
  self: WBasics & WLayoutInfo & WInTransition & WOutTransition & WAllocatable &
    WTempAllocator & WPtr & WDeref & WAssign & WStructInfo =>

  inline def contextOf[T <: Tuple]: Seq[Context] =
    inline erasedValue[T] match
      case _: (head *: tail) =>
        summonInline[LayoutInfo[head]].context +: contextOf[tail]
      case _: EmptyTuple => Seq.empty[Context]

  inline def contextOfRet[R] = inline erasedValue[R] match
    case _: Unit => None
    case _       => Some(summonInline[LayoutInfo[R]].context)

  inline def handleInput[A](inline a: Object) =
    summonInline[OutTransition[A]].from(a)

  inline def handleOutput[A](inline a: A)(using Allocator) =
    inline a match
      case u: Unit =>
        u
        null
      case _ =>
        summonInline[InTransition[A]].to(a)

  inline def carrierTypes[Output <: Tuple]: Seq[Class[?]] =
    inline erasedValue[Output] match
      case _: (head *: tail) =>
        carrierFromContext(
          summonInline[LayoutInfo[head]].context
        ) +: carrierTypes[tail]
      case _: EmptyTuple => Seq.empty[Class[?]]

  inline def contextOfFn[Fn]: (Seq[Context], Option[Context]) =
    inline erasedValue[Fn] match
      case _: (() => r) =>
        (
          Seq.empty[Context],
          contextOfRet[r]
        )
      case _: (a => r) =>
        (
          contextOf[Tuple1[a]],
          contextOfRet[r]
        )
      case _: ((a, b) => r) =>
        (
          contextOf[(a, b)]: Seq[Context],
          contextOfRet[r]: Option[Context]
        )

      case _: ((a, b, c) => r) =>
        (
          contextOf[(a, b, c)],
          contextOfRet[r]
        )
  inline def methodTypeOf[Outputs <: Tuple, Input]: MethodType =
    inline erasedValue[Input] match
      case _: Unit =>
        inline erasedValue[Outputs] match
          case _: EmptyTuple => VoidHelper.methodTypeV().nn
          case _ =>
            val carriers = carrierTypes[Outputs]
            VoidHelper.methodTypeV(carriers.head, carriers.tail*).nn
      case _ =>
        val returnCarrier = carrierFromContext(
          summonInline[LayoutInfo[Input]].context
        )
        inline erasedValue[Outputs] match
          case _: EmptyTuple => MethodType.methodType(returnCarrier).nn
          case _ =>
            val carriers = carrierTypes[Outputs]
            MethodType
              .methodType(returnCarrier, carriers.head, carriers.tail*)
              .nn

  inline def toMh[Fn](fn: Fn) =
    inline fn match
      case fn0: (() => r) =>
        val wrapper = () => {
          given Allocator = localAllocator()
          val res = handleOutput[r](fn0())
          resetAllocator()
          res
        }
        MethodHandles
          .lookup()
          .nn
          .findVirtual(
            classOf[Function0[?]],
            "apply",
            MethodType.genericMethodType(0)
          )
          .nn
          .bindTo(wrapper)
          .nn
          .asType(methodTypeOf[EmptyTuple, r])
      
      case fn1: ((a) => r) => 
        val wrapper = (a: Object) => {
          given Allocator = localAllocator()
          val res = handleOutput[r](fn1(handleInput(a)))
          resetAllocator()
          res 
        }

        MethodHandles.lookup().nn.findVirtual(
          classOf[Function1[?,?]],
          "apply",
          MethodType.genericMethodType(1)
        ).nn.bindTo(wrapper).nn.asType(methodTypeOf[Tuple1[a], r])


      case fn2: ((a,b) => r) => 
        val wrapper = (a: Object, b: Object) => {
          given Allocator = localAllocator() 
          val res = handleOutput[r](fn2(handleInput[a](a), handleInput[b](b)))
          resetAllocator()
          res
        }

        MethodHandles.lookup().nn.findVirtual(
          classOf[Function2[?,?,?]],
          "apply",
          MethodType.genericMethodType(2)
        ).nn.bindTo(wrapper).nn.asType(methodTypeOf[(a,b), r])


  def upcallGen(
      mh: MethodHandle,
      inputs: Seq[Context],
      ret: Option[Context]
  )(using Scope): RawMem

  inline given fnAllocatable[Fn]: Allocatable[Fn] = new Allocatable[Fn]:
    def apply(a: Fn)(using Scope, Allocator): Pointer[Fn] =
      val (inputs: Seq[Context], output: Option[Context]) = contextOfFn[Fn]
      Ptr[Fn](upcallGen(toMh[Fn](a).nn, inputs, output), 0)

    @targetName("arrayApply")
    def apply(a: Array[Fn])(using Scope, Allocator): Pointer[Fn] = ???

// inline def toUpcall[Fn](fn: Fn) =
//   inline fn match
//     case f: (() => r) =>
//       MethodHandles.lookup().nn.findVirtual(

//       )
