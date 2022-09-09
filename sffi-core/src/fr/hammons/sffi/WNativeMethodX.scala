package fr.hammons.sffi

import java.lang.invoke.MethodHandle
import scala.compiletime.{
  constValue,
  summonInline,
  erasedValue,
  summonFrom,
  error,
  codeOf,
  asMatchable
}
import scala.compiletime.ops.int.S
import scala.compiletime.ops.any.{!=, ToString}
import scala.collection.immutable.LazyList.cons
import scala.annotation.experimental
import scala.language.experimental.erasedDefinitions
import java.util.concurrent.atomic.AtomicReference

trait NativeMethodI:
  self: WBasics & WLayoutInfo & WOutTransition & WStructInfo & WLayoutInfo &
    WInTransition & WTempAllocator & WLookup =>

  def methodGen(
      name: String,
      inputs: Seq[Context],
      output: Option[Context],
      addendum: Seq[Context]
  ): MethodHandle

  inline def verifyAsA[A](inline a: Any) =
    inline a match
      case r: A => r

  inline def handleOutput[T](inline o: Object | Null): T =
    inline erasedValue[T] match
      case _: Unit =>
        o
        verifyAsA[T](())
      case _ =>
        summonInline[OutTransition[T]].from(o.nn)

  inline def outputContext[T]: Option[Context] = summonFrom {
    case o: LayoutInfo[T] => Some(o.context)
    case _                => Option.empty[Context]
  }

  inline def handleInput[T](inline t: T)(using Allocator): Object =
    inline t match
      case u: Seq[Variadic] =>
        ???
      case _ => summonInline[InTransition[T]].to(t)

  inline def layout[a]: LayoutInfo[a] =
    summonInline[LayoutInfo[a]]

  inline def contextFromTup[T <: Tuple]: Seq[Context] =
    inline erasedValue[T] match
      case _: (head *: tail) => layout[head].context +: contextFromTup[tail]
      case _: EmptyTuple     => Seq.empty[Context]

  transparent inline def methodHandle[T <: Tuple, Output](
      name: String
  ): (Seq[Variadic] => MethodHandle) | MethodHandle =
    inline erasedValue[Tuple.Last[T]] match
      case _: Seq[Variadic] =>
        val oldRef = AtomicReference[(Seq[Context], MethodHandle)]
        (s: Seq[Variadic]) => {
          val thisContext = s.map(_.use[LayoutInfo](l ?=> _ => l.context))
          val old = oldRef.get()
          if old != null && old._1 == thisContext then old._2
          else
            val newMh = methodGen(
              name,
              contextFromTup[Tuple.Init[T]]: Seq[Context],
              outputContext[Output]: Option[Context],
              thisContext
            )
            oldRef.compareAndSet(old, (thisContext, newMh))
            newMh
        }
      case _ =>
        methodGen(
          name,
          contextFromTup[T]: Seq[Context],
          outputContext[Output]: Option[Context],
          Seq.empty
        )

  inline def handlePossiblyVariadic[A, R](
      a: A,
      mh: (Seq[Variadic] => MethodHandle) | MethodHandle,
      inline variadic: Seq[Object],
      inline nonVar: MethodHandle => Object | Null
  )(using Allocator) = inline a match
    case args: Seq[Variadic] =>
      val totalArgs = summonFrom {
        case _: StructInfo[R] => Seq(summon[Allocator])
        case _                => Seq.empty[Object]
      } ++ variadic ++ args.map(_.use[InTransition](_.to))
      MethodHandleFacade.callVariadic(
        mh.asInstanceOf[Seq[Variadic] => MethodHandle](args),
        totalArgs*
      )
    case _ =>
      nonVar(mh.asInstanceOf[MethodHandle])

  inline def fnGen[Fn](name: String): Fn =
    verifyAsA[Fn] {
      inline erasedValue[Fn] match
        case _: (() => r) =>
          val mh = methodGen(
            name,
            Seq.empty,
            outputContext[r]: Option[Context],
            Seq.empty
          )
          () =>
            given Allocator = localAllocator()
            val res = handleOutput[r](
              mhCall[r, 0](mh)()
            )
            resetAllocator()
            res

        case _: ((a) => r) =>
          val mh = methodHandle[Tuple1[a], r](name)
          (a: a) =>
            given Allocator = localAllocator()

            val res = handleOutput[r](
              handlePossiblyVariadic[a, r](
                a,
                mh,
                Seq.empty,
                mhCall[r, 1](_)(handleInput(a))
              )
            )

            resetAllocator()
            res

        case _: ((a, b) => r) =>
          val mh = methodHandle[(a, b), r](name)
          (a: a, b: b) =>
            given Allocator = localAllocator()

            val res = handleOutput[r](
              handlePossiblyVariadic[b, r](
                b,
                mh,
                Seq(handleInput(a)),
                mhCall[r, 2](_)(handleInput(a), handleInput(b))
              )
            )
            resetAllocator()
            res

        case _: ((a, b, c) => r) =>
          val mh = methodHandle[(a, b, c), r](name)
          (a: a, b: b, c: c) =>
            given Allocator = localAllocator()
            val res = handleOutput[r](
              handlePossiblyVariadic(
                c,
                mh,
                Seq(handleInput(a), handleInput(b)),
                mhCall[r, 3](_)(handleInput(a), handleInput(b), handleInput(c))
              )
            )
            resetAllocator()
            res

        case _: ((a, b, c, d) => r) =>
          val mh = methodHandle[(a, b, c, d), r](name)
          (a: a, b: b, c: c, d: d) =>
            given Allocator = localAllocator()
            val res = handleOutput[r](
              handlePossiblyVariadic(
                d,
                mh,
                Seq(handleInput(a), handleInput(b), handleInput(c)),
                mhCall[r, 4](_)(
                  handleInput(a),
                  handleInput(b),
                  handleInput(c),
                  handleInput(d)
                )
              )
            )
    }

  // summonFrom{
  //   case oT: OutTransition[T] => oT.from(o.nn)
  //   case _ => inline erasedValue[T] match
  //     case _: Unit =>
  //       o
  //       ().asInstanceOf[T]
  //     case _ => error("Unsupported return type")
  // }

  type MHCall[Arity <: Int & Singleton] = Arity match
    case 0 => () => Object | Null
    case 1 => (Object) => Object | Null
    case 2 => (Object, Object) => Object | Null
    case 3 => (Object, Object, Object) => Object | Null
    case 4 => (Object, Object, Object, Object) => Object | Null

  inline def mhCall[Return, Size <: Int & Singleton](
      mh: MethodHandle
  )(using Allocator): MHCall[Size] =
    val memSegmentReturn = summonFrom {
      case _: StructInfo[Return] => true
      case _                     => false
    }

    verifyAsA[MHCall[Size]] {
      inline constValue[Size] match
        case 0 =>
          () =>
            if memSegmentReturn then
              MethodHandleFacade.call(mh, summon[Allocator])
            else MethodHandleFacade.call(mh)
        case 1 =>
          if memSegmentReturn then
            MethodHandleFacade.call(mh, summon[Allocator], _)
          else MethodHandleFacade.call(mh, _)
        case 2 =>
          if memSegmentReturn then
            MethodHandleFacade.call(mh, summon[Allocator], _, _)
          else MethodHandleFacade.call(mh, _, _)
        case 3 =>
          if memSegmentReturn then
            MethodHandleFacade.call(mh, summon[Allocator], _, _, _)
          else MethodHandleFacade.call(mh, _, _, _)
        case 4 =>
          if memSegmentReturn then
            MethodHandleFacade.call(mh, summon[Allocator], _, _, _, _)
          else MethodHandleFacade.call(mh, _, _, _, _)

    }

// inline def handleInput[T](inline t: T)(using Allocator): NativeCompat =
//   summonInline[InTransition[T]].to(t)
