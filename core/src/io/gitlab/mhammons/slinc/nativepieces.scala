package io.gitlab.mhammons.slinc

import scala.quoted.*
import jdk.incubator.foreign.{
   CLinker,
   MemoryLayout,
   FunctionDescriptor,
   MemorySegment,
   MemoryAddress,
   SegmentAllocator
}
import scala.util.chaining.*
import cats.free.Free
import cats.implicits.*
import scala.compiletime.error
import java.lang.invoke.MethodType
import scala.jdk.OptionConverters.*
import java.lang.invoke.MethodHandle
import cats.data.Func

object NativePieces:

   def functionDescriptor(
       typ: Type[?],
       args: Expr[NativeIO[Seq[MemoryLayout]]]
   )(using
       Quotes
   ): Expr[NativeIO[FunctionDescriptor]] =
      typ match
         case '[Unit] =>
            '{ $args.map(FunctionDescriptor.ofVoid(_*)) }
         case r =>
            '{
               for
                  returnLayout <- ${ type2MemoryLayout(r) }
                  rest <- $args
               yield FunctionDescriptor.of(returnLayout, rest*)
            }

   def methodType(typ: Type[?], args: Expr[Seq[Class[?]]])(using
       Quotes
   ) = typ match
      case '[Unit] =>
         '{
            $args.pipe(as =>
               if as.isEmpty then VoidHelper.methodTypeV()
               else if as.size == 1 then VoidHelper.methodTypeV(as.head)
               else VoidHelper.methodTypeV(as.head, as.tail*)
            )
         }
      case r =>
         '{
            $args.pipe(as =>
               if as.isEmpty then
                  MethodType.methodType(${ type2MethodTypeArg(r) })
               else if as.size == 1 then
                  MethodType.methodType(${ type2MethodTypeArg(r) }, as.head)
               else
                  MethodType.methodType(
                    ${ type2MethodTypeArg(r) },
                    as.head,
                    as.tail*
                  )
            )
         }

   def fetchImplicit[A: Type](using Quotes) =
      import quotes.reflect.*

      Implicits.search(TypeRepr.of[A]) match
         case i: ImplicitSearchSuccess => i.tree.asExprOf[A]
         case _ =>
            report.errorAndAbort(
              s"Couldn't find given for ${TypeRepr.of[A].show(using Printer.TypeReprCode)}"
            )

   val paramNames = LazyList.iterate(1)(_ + 1).map("p" + _.toString)

   def functionImpl[A: Type](name: Expr[String])(using Quotes) =
      import quotes.reflect.{MethodType => MT, *}

      TypeRepr.of[A] match
         case a @ AppliedType(t, subtypes)
             if t.typeSymbol.name.startsWith("Function") && t.typeSymbol.name
                .replace("Function", "")
                .toIntOption
                .isDefined =>
            val paramTypeReprs = subtypes.init
            val paramTypes = paramTypeReprs.map(_.asType)

            val parameterLayouts =
               Expr.ofSeq(subtypes.init.map(typeRepr2MemoryLayout))
            val resultTypeRepr = subtypes.last
            val resultType = resultTypeRepr.asType
            val functionD = functionDescriptor(
              resultType,
              '{
                 $parameterLayouts.sequence
              }
            )

            val methodT = methodType(
              resultType,
              Expr.ofSeq(subtypes.init.map(_.asType.pipe(type2MethodTypeArg)))
            )

            val methodHandle: Expr[NativeIO[MethodHandle]] = '{
               Free.liftF[NativeOp, MethodHandle](
                 NativeOp.MethodHandleBinding[A](
                   $name,
                   ${ methodHandleBinderImpl[A](name) }
                 )
               )
            }

            val resultNeedsSegmentAllocator = resultType match
               case '[StructBacking] => true
               case _                => false

            val needsSegmentAllocator =
               resultNeedsSegmentAllocator || paramTypes.exists {
                  case '[String] => true
                  case _         => false
               }

            val newRes = t.asType
               .pipe {
                  case '[t] if needsSegmentAllocator =>
                     Applied(
                       TypeTree.of[t],
                       paramTypes.map { case '[t] =>
                          TypeTree.of[t]
                       } :+ resultType.pipe { case '[t] =>
                          Applied(
                            TypeTree.of[ContextFunction1],
                            List(
                              TypeTree.of[SegmentAllocator],
                              TypeTree.of[NativeIO[t]]
                            )
                          )
                       }
                     )
                  case '[t] =>
                     Applied(
                       TypeTree.of[t],
                       paramTypes.map { case '[t] =>
                          TypeTree.of[t]
                       } :+ resultType
                          .pipe { case '[t] => TypeTree.of[NativeIO[t]] }
                     )
               }
               .tpe
               .asType

            (newRes, resultType).pipe { case ('[l], '[r]) =>
               genLambda[r](
                 needsSegmentAllocator,
                 resultNeedsSegmentAllocator,
                 methodHandle,
                 paramTypes,
                 resultType
               ).asExprOf[l]
            }
         case f =>
            report.errorAndAbort(
              f
                 .show(using Printer.TypeReprStructure)
            )
   end functionImpl

   def paramModifiers(
       segAlloc: Option[Expr[SegmentAllocator]]
   )(param: Type[?])(using Quotes) =
      param match
         case '[String] =>
            val impl = segAlloc.get
            (a: Expr[Any]) =>
               '{
                  CLinker.toCString(${ a.asExprOf[String] }, $impl).address
               }
         case '[StructBacking] =>
            (a: Expr[Any]) =>
               '{
                  ${ a.asExprOf[StructBacking] }.$mem
               }
         case _ => (a: Expr[Any]) => a

   def resultModifiers[r: Type](expr: Expr[Any])(using
       Quotes
   ) =
      Type.of[r] match
         case '[StructBacking] => '{ $expr.asInstanceOf[r] }
         case '[String] =>
            '{ CLinker.toJavaString($expr.asInstanceOf[MemoryAddress]) }
               .asExprOf[r]
         case _ => '{ $expr.asInstanceOf[r] }

   def mhCall[A: Type](using
       q: Quotes
   )(
       segAlloc: Option[Expr[SegmentAllocator]],
       ps: List[Expr[Any]],
       pTyps: List[Type[?]]
   ): Expr[MethodHandle => A] = '{ mh =>
      ${
         resultModifiers[A]('{
            mh.invokeWithArguments(${ Varargs(trueArgs(segAlloc, ps, pTyps)) }*)
         })
      }
   }

   def trueArgs(using
       q: Quotes
   )(
       segAlloc: Option[Expr[SegmentAllocator]],
       args: List[Expr[Any]],
       typs: List[Type[?]]
   ): List[Expr[Any]] = args
      .zip(typs.map(paramModifiers(segAlloc)))
      .map((v, mod) => mod(v))

   def genLambda[r: Type](
       needsSegmentAllocator: Boolean,
       resultTypeAllocates: Boolean,
       meth: Expr[NativeIO[MethodHandle]],
       params: List[Type[?]],
       result: Type[?]
   )(using Quotes) =
      import quotes.reflect.{MethodType => MT, *}

      val paramReprs = params.map { case '[t] => TypeRepr.of[t] }
      val resultRepr =
         if needsSegmentAllocator then
            result.pipe { case '[t] =>
               TypeRepr.of[SegmentAllocator ?=> NativeIO[t]]
            }
         else result.pipe { case '[t] => TypeRepr.of[NativeIO[t]] }
      val args = (ps: List[Expr[Any]]) =>
         if resultTypeAllocates then
            Expr.ofSeq(Expr.summon[SegmentAllocator].get :: ps)
         else Expr.ofSeq(ps)
      val lmb = (s: Symbol, ps: List[Tree]) =>
         if needsSegmentAllocator then
            '{ (seg: SegmentAllocator) ?=>
               $meth.map(${ mhCall[r](Some('seg), ps.map(_.asExpr), params) })
            }.asTerm.changeOwner(s)
         else
            '{
               $meth.map(${ mhCall[r](None, ps.map(_.asExpr), params) })
            }.asTerm.changeOwner(s)
      Lambda(
        Symbol.spliceOwner,
        MT(paramNames.take(params.size).toList)(
          _ => paramReprs,
          _ => resultRepr
        ),
        lmb
      )
   end genLambda

   def methodHandleBinderImpl[A: Type](
       nameExpr: Expr[String]
   )(using Quotes): Expr[CLinker => NativeIO[MethodHandle]] =
      import quotes.reflect.*

      TypeRepr.of[A] match
         case AppliedType(t, subtypes)
             if t.typeSymbol.name.startsWith("Function") && t.typeSymbol.name
                .replace("Function", "")
                .toIntOption
                .isDefined =>
            val paramTypes = subtypes.init
            val parameterTypes =
               Expr.ofSeq(paramTypes.map(typeRepr2MemoryLayout))
            val resultType = subtypes.last.asType
            val functionD = functionDescriptor(
              resultType,
              '{
                 $parameterTypes.sequence
              }
            )

            val methodT = methodType(
              resultType,
              Expr.ofSeq(paramTypes.map(_.asType.pipe(type2MethodTypeArg)))
            )

            '{ (c: CLinker) =>
               $functionD
                  .map(fd =>
                     clookup($nameExpr).pipe(c.downcallHandle(_, $methodT, fd))
                  )
            }
         case f =>
            report.errorAndAbort(
              s"type ${f.show(using Printer.TypeReprCode)} cannot be translated to a method handle"
            )
            report.errorAndAbort(
              f
                 .show(using Printer.TypeReprStructure)
            )
   end methodHandleBinderImpl

   def typeRepr2MemoryLayout(using Quotes)(
       typeRepr: quotes.reflect.TypeRepr
   ): Expr[NativeIO[MemoryLayout]] =
      type2MemoryLayout(typeRepr.asType)

   def type2MemoryLayout(typ: Type[?])(using
       q: Quotes
   ): Expr[NativeIO[MemoryLayout]] =
      typ match
         case '[Int] =>
            '{ NativeIO.pure(CLinker.C_INT) }
         case '[Float]   => '{ NativeIO.pure(CLinker.C_FLOAT) }
         case '[Double]  => '{ NativeIO.pure(CLinker.C_DOUBLE) }
         case '[Boolean] => '{ NativeIO.pure(CLinker.C_CHAR) }
         case '[Char]    => '{ NativeIO.pure(CLinker.C_CHAR) }
         case '[String]  => '{ NativeIO.pure(CLinker.C_POINTER) }
         case '[Short]   => '{ NativeIO.pure(CLinker.C_SHORT) }
         case '[Long]    => '{ NativeIO.pure(CLinker.C_LONG) }
         case '[Fd[t]]   => type2MemoryLayout(Type.of[t])
         case '[Ptr[t]]  => '{ NativeIO.pure(CLinker.C_POINTER) }
         case '[t]       => '{ NativeIO.layout[t] }

   def type2MethodTypeArg(typ: Type[?])(using Quotes): Expr[Class[?]] =
      typ match
         case '[Long]          => '{ classOf[Long] }
         case '[Int]           => '{ classOf[Int] }
         case '[Float]         => '{ classOf[Float] }
         case '[Double]        => '{ classOf[Double] }
         case '[Boolean]       => '{ classOf[Boolean] }
         case '[Char]          => '{ classOf[Char] }
         case '[String]        => '{ classOf[MemoryAddress] }
         case '[Short]         => '{ classOf[Short] }
         case '[StructBacking] => '{ classOf[MemorySegment] }
         case '[Ptr[t]]        => '{ classOf[MemoryAddress] }
         case '[Fd[t]]         => type2MethodTypeArg(Type.of[t])

   def refinementDataExtraction(using Quotes)(
       typ: quotes.reflect.TypeRepr
   ): List[(String, Type[?])] =
      import quotes.reflect.*
      typ.dealias match
         case Refinement(ancestor, name, typ) =>
            name -> typ.asType :: refinementDataExtraction(ancestor)
         case TypeRef(_, "StructBacking") =>
            Nil
         case t =>
            report.errorAndAbort(
              s"Cannot derive a layout for non-struct type ${t.show(using Printer.TypeReprCode)}"
            )

   def deriveLayoutImpl[T: Type](using
       q: Quotes
   ): Expr[(String, () => NativeIO[MemoryLayout])] =
      import quotes.reflect.*

      val typRepr = TypeRepr.of[T]
      val fieldsInfo = refinementDataExtraction(typRepr).reverse
      val fieldLayouts = Expr.ofSeq(
        fieldsInfo.map((name, typ) =>
           '{ ${ type2MemoryLayout(typ) }.map(_.withName(${ Expr(name) })) }
        )
      )

      val name = fieldsInfo
         .map { case (name, '[t]) =>
            s"$name: ${TypeRepr.of[t].show(using Printer.TypeReprCode)}"
         }
         .mkString(",")
      '{
         ${ Expr(name) } -> (() =>
            ${ fieldLayouts }.sequence
               .map(
                 MemoryLayout
                    .structLayout(_*)
               )
         )
      }

   inline def deriveLayout[T] = ${ deriveLayoutImpl[T] }
end NativePieces
