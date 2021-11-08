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
import scala.jdk.OptionConverters.*
import java.lang.invoke.MethodHandle
import cats.data.Func
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import io.gitlab.mhammons.polymorphics.MethodHandleHandler

object NativePieces:
   def functionDescriptor[A: Type](
       paramTypes: List[Type[?]]
   )(using
       Quotes
   ): Expr[NativeIO[FunctionDescriptor]] =
      val paramLayouts = paramTypes
         .map { case '[p] => type2MemoryLayout[p] }
         .pipe(Expr.ofSeq)
         .pipe(exp => '{ $exp.sequence })
      Type.of[A] match
         case '[Unit] =>
            '{ $paramLayouts.map(FunctionDescriptor.ofVoid(_*)) }
         case '[r] =>
            '{
               for
                  returnLayout <- ${ type2MemoryLayout[r] }
                  rest <- $paramLayouts
               yield FunctionDescriptor.of(returnLayout, rest*)
            }

   def methodType[A: Type](args: Seq[Expr[Class[?]]])(using
       Quotes
   ) =
      import java.lang.invoke.MethodType

      Type.of[A] match
         case '[Unit] =>
            if args.isEmpty then '{ VoidHelper.methodTypeV() }
            else
               '{
                  VoidHelper.methodTypeV(
                    ${ args.head },
                    ${ Varargs(args.tail) }*
                  )
               }
         case '[r] =>
            if args.isEmpty then
               '{ MethodType.methodType(${ type2MethodTypeArg[r] }) }
            else
               '{
                  MethodType.methodType(
                    ${ type2MethodTypeArg[r] },
                    ${ args.head },
                    ${ Varargs(args.tail) }*
                  )
               }

   val paramNames = LazyList.iterate(1)(_ + 1).map("p" + _.toString)

   def functionImpl[A: Type](name: Expr[String])(using Quotes) =
      import quotes.reflect.{MethodType => MT, *}

      TypeRepr.of[A] match
         case a @ AppliedType(outer, subtypes)
             if outer.typeSymbol.name.startsWith(
               "Function"
             ) && outer.typeSymbol.name
                .replace("Function", "")
                .toIntOption
                .isDefined =>
            val outerTypeTree = outer.asType match
               case '[o] => TypeTree.of[o]
            val paramTypes = subtypes.init.map(_.asType)
            val paramTypeTrees = paramTypes.map { case '[a] => TypeTree.of[a] }
            val resultType = subtypes.last.asType

            val methodHandle: Expr[NativeIO[MethodHandle]] = '{
               Free.liftF[NativeOp, MethodHandle](
                 NativeOp.MethodHandleBinding[A](
                   $name,
                   ${ methodHandleBinderImpl[A](name) }
                 )
               )
            }

            val resultNeedsSegmentAllocator = resultType match
               case '[Struct] => true
               case _         => false

            val needsSegmentAllocator =
               resultNeedsSegmentAllocator || paramTypes.exists {
                  case '[String] => true
                  case _         => false
               }

            val newResultTypeTree = resultType match
               case '[r] =>
                  if needsSegmentAllocator then
                     List(
                       TypeTree.of[SegmentAllocator],
                       TypeTree.of[NativeIO[r]]
                     ).pipe(Applied(TypeTree.of[ContextFunction1], _))
                  else TypeTree.of[NativeIO[r]]

            val finalResultType = Applied(
              outerTypeTree,
              paramTypeTrees :+ newResultTypeTree
            ).tpe.asType

            (finalResultType, resultType) match
               case ('[finalResult], '[initialResult]) =>
                  genLambda[initialResult](
                    needsSegmentAllocator,
                    resultNeedsSegmentAllocator,
                    methodHandle,
                    paramTypes
                  ).asExprOf[finalResult]
         case f => // todo: elaborate
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
         case '[Struct] =>
            (a: Expr[Any]) =>
               '{
                  ${ a.asExprOf[Struct] }.$mem
               }
         case _ => (a: Expr[Any]) => a

   def resultModifiers[r: Type](expr: Expr[Any])(using
       Quotes
   ) =
      Type.of[r] match
         case '[Struct] =>
            '{ NativeIO.toStruct[r]($expr.asInstanceOf[MemorySegment]) }
         case '[String] =>
            '{
               NativeIO.pure(
                 CLinker.toJavaString($expr.asInstanceOf[MemoryAddress])
               )
            }.asExprOf[NativeIO[r]]
         case _ => '{ NativeIO.pure($expr.asInstanceOf[r]) }

   def mhCall[A: Type](using
       Quotes
   )(
       ps: List[Expr[Any]]
   ): Expr[MethodHandle => NativeIO[A]] =
      import quotes.reflect.*
      val arity = ps.size
      val helper = Ident(
        TermRef(TypeRepr.of[MethodHandleHandler.type], s"call$arity")
      )
      val call = (mh: Expr[MethodHandle]) =>
         Apply(
           helper,
           mh.asTerm :: ps.map(_.asTerm)
         ).asExprOf[Any]
      '{ mh =>
         ${
            resultModifiers[A](call('mh))
         }
      }

   def trueArgs(
       resultTypeAllocates: Boolean,
       typs: List[Type[?]]
   )(
       segAlloc: Option[Expr[SegmentAllocator]],
       args: List[Expr[Any]]
   )(using Quotes): List[Expr[Any]] = args
      .zip(typs.map(paramModifiers(segAlloc)))
      .map((v, mod) => mod(v))
      .pipe {
         case tArgs if resultTypeAllocates =>
            segAlloc.map(_ :: tArgs).getOrElse(tArgs)
         case tArgs => tArgs
      }

   def genLambda[r: Type](
       needsSegmentAllocator: Boolean,
       resultTypeAllocates: Boolean,
       meth: Expr[NativeIO[MethodHandle]],
       params: List[Type[?]]
   )(using Quotes) =
      import quotes.reflect.*

      val paramReprs = params.map { case '[t] => TypeRepr.of[t] }
      val resultRepr =
         if needsSegmentAllocator then
            TypeRepr.of[SegmentAllocator ?=> NativeIO[r]]
         else TypeRepr.of[NativeIO[r]]
      val args = trueArgs(resultTypeAllocates, params)
      def call(ps: List[Expr[Any]], seg: Option[Expr[SegmentAllocator]]) = '{
         $meth.flatMap(${
            mhCall[r](args(seg, ps))
         })
      }
      def lmb(ps: List[Tree]) =
         val psExpr = ps.map(_.asExpr)
         if needsSegmentAllocator then
            '{ (seg: SegmentAllocator) ?=>
               ${ call(psExpr, Some('seg)) }
            }
         else call(psExpr, None)
      Lambda(
        Symbol.spliceOwner,
        MethodType(paramNames.take(params.size).toList)(
          _ => paramReprs,
          _ => resultRepr
        ),
        (s, ps) => lmb(ps).asTerm.changeOwner(s)
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
            val paramTypes = subtypes.init.map(_.asType)

            val resultType = subtypes.last.asType
            val functionD = resultType match
               case '[r] =>
                  functionDescriptor[r](
                    paramTypes
                  )

            val methodT = resultType match
               case '[r] =>
                  methodType[r](
                    paramTypes
                       .map { case '[a] => type2MethodTypeArg[a] }
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
   end methodHandleBinderImpl

   def typeRepr2MemoryLayout(using Quotes)(
       typeRepr: quotes.reflect.TypeRepr
   ): Expr[NativeIO[MemoryLayout]] =
      typeRepr.asType.pipe { case '[a] => type2MemoryLayout[a] }

   def type2MemoryLayout[A: Type](using
       q: Quotes
   ): Expr[NativeIO[MemoryLayout]] =
      Type.of[A] match
         case '[Int] =>
            '{ NativeIO.pure(CLinker.C_INT) }
         case '[Float]   => '{ NativeIO.pure(CLinker.C_FLOAT) }
         case '[Double]  => '{ NativeIO.pure(CLinker.C_DOUBLE) }
         case '[Boolean] => '{ NativeIO.pure(CLinker.C_CHAR) }
         case '[Char]    => '{ NativeIO.pure(CLinker.C_CHAR) }
         case '[String]  => '{ NativeIO.pure(CLinker.C_POINTER) }
         case '[Short]   => '{ NativeIO.pure(CLinker.C_SHORT) }
         case '[Long]    => '{ NativeIO.pure(CLinker.C_LONG) }
         case '[Fd[a]]   => type2MemoryLayout[a]
         case '[Ptr[a]]  => '{ NativeIO.pure(CLinker.C_POINTER) }
         case '[a]       => '{ NativeIO.layout[a] }

   // todo: rewrite nicer
   def type2MethodTypeArg[A: Type](using Quotes): Expr[Class[?]] =
      import quotes.reflect.*
      Type.of[A] match
         case '[Long]    => '{ classOf[Long] }
         case '[Int]     => '{ classOf[Int] }
         case '[Float]   => '{ classOf[Float] }
         case '[Double]  => '{ classOf[Double] }
         case '[Boolean] => '{ classOf[Boolean] }
         case '[Char]    => '{ classOf[Char] }
         case '[String]  => '{ classOf[MemoryAddress] }
         case '[Short]   => '{ classOf[Short] }
         case '[Struct]  => '{ classOf[MemorySegment] }
         case '[Ptr[t]]  => '{ classOf[MemoryAddress] }
         case '[Fd[t]]   => type2MethodTypeArg[t]
         case '[a] =>
            report.errorAndAbort(s"received unknown type ${Type.show[a]}")

   def refinementDataExtraction[A: Type](using
       Quotes
   ): List[(String, Type[?])] =
      import quotes.reflect.*
      TypeRepr.of[A].dealias match
         case Refinement(ancestor, name, typ) =>
            name -> typ.asType :: ancestor.asType.pipe { case '[t] =>
               refinementDataExtraction[t]
            }
         case TypeRef(repr, name)
             if TypeRepr.of[Struct].typeSymbol.name == name =>
            Nil
         case t =>
            report.errorAndAbort(
              s"Cannot derive a layout for non-struct type ${t.show(using Printer.TypeReprCode)}"
            )

   inline def getStructName[T] = ${
      getStructNameImpl[T]
   }

   def getStructNameImpl[A: Type](using Quotes): Expr[String] =
      Expr(
        refinementDataExtraction[A].reverse
           .map { case (name, '[t]) =>
              s"$name:${Type.show[t]}"
           }
           .mkString(",")
      )

   def deriveLayoutImpl[T: Type](using
       q: Quotes
   ): Expr[NativeIO[MemoryLayout]] =
      val fieldLayouts = refinementDataExtraction[T].reverse
         .map { case (name, '[a]) =>
            '{ ${ type2MemoryLayout[a] }.map(_.withName(${ Expr(name) })) }
         }
         .pipe(Expr.ofSeq)

      '{
         ${ fieldLayouts }.sequence
            .map(
              MemoryLayout
                 .structLayout(_*)
            )
      }

   inline def deriveLayout[T] = ${ deriveLayoutImpl[T] }

   inline def generateVarHandles[A] = ${ generateVarHandlesImpl[A] }

   def generateVarHandlesImpl[A: Type](using q: Quotes) =
      val refinementData = refinementDataExtraction[A]
      val varHandles = (layout: Expr[MemoryLayout]) =>
         refinementData
            .map { case (name, '[a]) =>
               val nameExp = Expr(name)
               '{
                  $nameExp -> $layout.varHandle(
                    ${ type2MethodTypeArg[a] },
                    MemoryLayout.PathElement.groupElement($nameExp)
                  )
               }
            }
            .pipe(Expr.ofSeq)
      '{
         NativeIO.layout[A].map(layout => ${ varHandles('layout) })
      }

   inline def memorySegment2Struct[T](memorySegment: MemorySegment) = ${
      memorySegment2StructImpl[T]('memorySegment)
   }

   def memorySegment2StructImpl[T: Type](memSgmnt: Expr[MemorySegment])(using
       Quotes
   ) =
      '{
         NativeIO
            .varHandles[T]
            .map(varHandles =>
               Struct(
                 varHandles
                    .map((name, vh) =>
                       name -> Fd($memSgmnt, VarHandleHandler(vh))
                    )
                    .toMap
                    .updated("$mem", $memSgmnt)
               )
            )
      }
   end memorySegment2StructImpl

end NativePieces
