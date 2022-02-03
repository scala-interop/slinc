package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{
   FunctionDescriptor,
   CLinker,
   SegmentAllocator,
   MemoryAddress,
   MemoryLayout,
   SymbolLookup => JSymbolLookup
}
import io.gitlab.mhammons.polymorphics.{MethodHandleHandler, VoidHelper}
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

import cats.data.Validated
import cats.implicits.*

object MethodHandleMacros:
   inline def methodTypeForFn[A](using Fn[A]) = ${
      methodTypeForFnImpl[A]
   }

   private def methodTypeForFnImpl[A: Type](using q: Quotes): Expr[MethodType] =
      import quotes.reflect.*

      TypeRepr.of[A] match
         case AppliedType(trepr, args)
             if trepr.typeSymbol.name.startsWith("Function") =>
            val types = args.map(_.asType)
            val inputLayouts = types.init.map { case '[a] =>
               '{ ${ Expr.summonOrError[NativeInfo[a]] }.carrierType }
            }
            val result = types.last.pipe { case '[r] =>
               Expr.summon[NativeInfo[r]].map(ni => '{ $ni.carrierType })
            }
            methodType(result, inputLayouts)

   inline def functionDescriptorForFn[A](using Fn[A]): FunctionDescriptor =
      ${
         functionDescriptorForFnImpl[A]
      }

   private def functionDescriptorForFnImpl[A](using q: Quotes)(using
       Type[A]
   ): Expr[FunctionDescriptor] =
      import quotes.reflect.*

      TypeRepr.of[A] match
         case AppliedType(_, args) =>
            val types = args.map(_.asType)
            val inputs = types.init.map { case '[a] =>
               '{ ${ Expr.summonOrError[NativeInfo[a]] }.layout }
            }
            val retType = types.last.pipe { case '[r] =>
               Expr.summon[NativeInfo[r]].map(e => '{ $e.layout })
            }

            functionDescriptor(retType, inputs)

   def methodType(
       returnCarrierType: Option[Expr[Class[?]]],
       carrierTypes: List[Expr[Class[?]]]
   )(using Quotes) =
      import java.lang.invoke.MethodType

      returnCarrierType
         .map { r =>
            if carrierTypes.isEmpty then '{ MethodType.methodType($r) }
            else
               '{
                  MethodType.methodType(
                    $r,
                    ${ carrierTypes.head },
                    ${ Varargs(carrierTypes.tail) }*
                  )
               }
         }
         .getOrElse {
            if carrierTypes.isEmpty then '{ VoidHelper.methodTypeV() }
            else
               '{
                  VoidHelper.methodTypeV(
                    ${ carrierTypes.head },
                    ${ Varargs(carrierTypes.tail) }*
                  )
               }
         }

   private def functionDescriptor(
       returnLayout: Option[Expr[MemoryLayout]],
       paramLayouts: List[Expr[MemoryLayout]]
   )(using Quotes): Expr[FunctionDescriptor] =
      returnLayout
         .map { r =>
            '{ FunctionDescriptor.of($r, ${ Varargs(paramLayouts) }*) }
         }
         .getOrElse('{ FunctionDescriptor.ofVoid(${ Varargs(paramLayouts) }*) })

   private def paramsToLayoutAndCarrier(params: List[Expr[Any]])(using
       Quotes
   ): (List[Expr[MemoryLayout]], List[Expr[Class[?]]]) =
      params
         .map(_.widen)
         .flatMap { case '{ $z: a } =>
            val a = Expr.summonOrError[NativeInfo[a]]
            Seq('{ $a.layout } -> '{ $a.carrierType })
         }
         .unzip

   private def dc[Ret](
       addr: Expr[MemoryAddress],
       params: List[Expr[Any]]
   )(using Quotes, Type[Ret]): Expr[Allocatee[MethodHandle]] =
      val (returnLayout, returnCarrierType) = Type.of[Ret] match
         case '[Unit] => (None, None)
         case '[r] =>
            Expr
               .summonOrError[NativeInfo[r]]
               .pipe(exp =>
                  Some('{ $exp.layout }) -> Some('{ $exp.carrierType })
               )

      val (layouts, carrierTypes) = paramsToLayoutAndCarrier(params)
      val functionD = functionDescriptor(returnLayout, layouts)
      val methodT = methodType(returnCarrierType, carrierTypes)

      '{
         Linker.linker.downcallHandle($addr, segAlloc, $methodT, $functionD)
      }

   private def dc2(
       addr: Expr[MemoryAddress],
       paramInfo: List[Expr[NativeInfo[?]]],
       returnInfo: Option[Expr[NativeInfo[?]]]
   )(using Quotes): Expr[MethodHandle] =

      val (layouts, carrierTypes) =
         paramInfo.map(i => '{ $i.layout } -> '{ $i.carrierType }).unzip
      val functionD =
         functionDescriptor(returnInfo.map(i => '{ $i.layout }), layouts)
      val methodT =
         methodType(returnInfo.map(i => '{ $i.carrierType }), carrierTypes)

      '{
         Linker.linker.downcallHandle(
           $addr,
           localAllocator,
           $methodT,
           $functionD
         )
      }

   def call[Ret: Type](
       mh: Expr[MethodHandle],
       ps: List[Expr[Any]]
   )(using
       Quotes
   ): Expr[Ret] =
      import quotes.reflect.*

      def callFn(mh: Expr[MethodHandle], args: List[Expr[Any]]) = Apply(
        Ident(
          TermRef(TypeRepr.of[MethodHandleHandler.type], s"call${ps.size}")
        ),
        mh.asTerm +: ps.map(_.asTerm)
      ).asExprOf[Any]

      Expr.betaReduce('{
         given Immigrator[Ret] = ${
            Expr.summonOrError[Immigrator[Ret]]
         }
         immigrator[Ret](${ callFn(mh, ps) })
      })

   def call2(mh: Expr[MethodHandle], ps: List[Expr[Any]])(using
       Quotes
   ): Expr[Any] =
      import quotes.reflect.*
      Apply(
        Ident(
          TermRef(TypeRepr.of[MethodHandleHandler.type], s"call${ps.size}")
        ),
        mh.asTerm +: ps.map(_.asTerm)
      ).asExprOf[Any]

   def binding[Ret: Type](
       address: Expr[MemoryAddress],
       ps: List[Expr[Any]]
   )(using
       Quotes
   ) =
      import quotes.reflect.*
      val methodHandle =
         dc[Ret](
           address,
           ps
         )

      Expr.betaReduce('{
         given SegmentAllocator = localAllocator
         try {
            val mh = $methodHandle
            ${
               call(
                 '{ mh },
                 ps.map(_.widen).map { case '{ $a: a } =>
                    '{ ${ Expr.summonOrError[Emigrator[a]] }($a) }
                 }
               )
            }
         } finally {
            reset()
         }

      })

   def vdAndRefFromExpr[A](expr: Expr[A], name: String)(using
       q: Quotes,
       t: Type[A]
   ): (q.reflect.ValDef, q.reflect.Ref) =
      import quotes.reflect.*
      val sym = Symbol.newVal(
        Symbol.spliceOwner,
        name,
        TypeRepr.of[A],
        Flags.EmptyFlags,
        Symbol.noSymbol
      )

      val vd = ValDef(sym, Some(expr.asTerm.changeOwner(sym)))
      val ref = Ref(sym)
      vd -> ref

   def wrappedMH(
       memoryAddress: Expr[MemoryAddress],
       inputNames: List[Option[String]],
       inputTypes: List[Type[?]],
       returnType: Type[?]
   )(using q: Quotes): Validated[List[String], Expr[Any]] =
      import quotes.reflect.{MethodType => MT, *}
      case class MHState(
          inputNIVds: List[ValDef],
          inputNIRefs: List[Ref],
          inputEmiVds: List[ValDef],
          inputEmiRefs: List[Ref],
          retNIVd: Option[ValDef],
          retNIRef: Option[Ref],
          retImmiVd: ValDef,
          retImmiRef: Ref,
          mhVd: ValDef,
          mhRef: Ref
      )

      val paramNames =
         LazyList.iterate('a')(a => (a + 1).toChar).map(_.toString)

      val inputNIs =
         inputNames
            .zip(inputTypes)
            .zip(paramNames.map(s => s"ni$s"))
            .traverse { case ((maybeInputName, '[a]), name) =>
               Validated
                  .fromOption(
                    Expr.summon[NativeInfo[a]],
                    maybeInputName match
                       case Some(inputName) =>
                          List(
                            s"Could not summon ${Type.show[NativeInfo[a]]} for parameter $inputName. Is it defined?"
                          )
                       case None =>
                          List(
                            s"Could not summon ${Type.show[NativeInfo[a]]} for variadic argument. Is it defined?"
                          )
                  )
                  .map(vdAndRefFromExpr(_, name))
            }
            .map(_.unzip)

      val inputEmis = inputNames
         .zip(inputTypes)
         .zip(paramNames.map(s => s"emi$s"))
         .traverse { case ((maybeInputName, '[a]), name) =>
            Validated
               .fromOption(
                 Expr.summon[Emigrator[a]],
                 maybeInputName match
                    case Some(inputName) =>
                       List(
                         s"Could not summon ${Type.show[Emigrator[a]]} for parameter $inputName. Is it defined?"
                       )
                    case None =>
                       List(
                         s"Could not summon ${Type.show[Emigrator[a]]} for variadic parameter. Is it defined?"
                       )
               )
               .map(vdAndRefFromExpr(_, name))
         }
         .map(_.unzip)

      val retNI = returnType match
         case '[Unit] =>
            Validated.valid((Option.empty[ValDef], Option.empty[Ref]))
         case '[r] =>
            Validated
               .fromOption(
                 Expr.summon[NativeInfo[r]],
                 List(
                   s"Could not summon ${Type.show[NativeInfo[r]]} for the binding return. Is it defined?"
                 )
               )
               .map(
                 vdAndRefFromExpr(_, "retNI")
                    .pipe(t => (Some(t._1), Some(t._2)))
               )

      val retImmi = returnType.match { case '[r] =>
         Validated
            .fromOption(
              Expr.summon[Immigrator[r]],
              List(
                s"Could not summon ${Type.show[NativeInfo[r]]} for the binding return. Is it defined?"
              )
            )
            .map(vdAndRefFromExpr(_, "retImmi"))
      }

      val state = inputNIs.andThen((inputVds, inputRefs) =>
         inputEmis.andThen((emiVDs, emiRefs) =>
            retNI.andThen((retNIVd, retNIRef) =>
               retImmi.map { (immiVd, immiRef) =>
                  val (mhVd, mhRef) = vdAndRefFromExpr(
                    dc2(
                      memoryAddress.asExprOf[MemoryAddress],
                      inputRefs.map(_.asExprOf[NativeInfo[?]]),
                      retNIRef.map(_.asExprOf[NativeInfo[?]])
                    ),
                    "mh"
                  )
                  MHState(
                    inputVds,
                    inputRefs,
                    emiVDs,
                    emiRefs,
                    retNIVd,
                    retNIRef,
                    immiVd,
                    immiRef,
                    mhVd,
                    mhRef
                  )
               }
            )
         )
      )

      def lmb(s: MHState) =
         Lambda(
           Symbol.spliceOwner,
           MT(paramNames.take(inputTypes.size).toList)(
             _ => inputTypes.map { case '[a] => TypeRepr.of[a] },
             _ => returnType match { case '[r] => TypeRepr.of[r] }
           ),
           (owner, trees) =>
              returnType match {
                 case '[r] =>
                    val retImmi = s.retImmiRef.asExprOf[Immigrator[r]]
                    val callExpr = call2(
                      s.mhRef.asExprOf[MethodHandle],
                      inputTypes.zip(trees.zip(s.inputEmiRefs)).map {
                         case ('[a], (t, emi)) =>
                            '{
                               try ${ emi.asExprOf[Emigrator[a]] }(${
                                  t.asExprOf[a]
                               })(
                                 using localAllocator
                               )
                               finally reset()

                            }
                      }
                    )
                    '{
                       $retImmi($callExpr)
                    }.asTerm.changeOwner(owner)
              }
         )

      state.map(s =>
         Block(
           s.inputEmiVds ++ s.inputNIVds ++ s.retNIVd.toList ++
              List(
                s.retImmiVd,
                s.mhVd
              ),
           lmb(s)
         ).asExpr
      )

   def wrappedMHFromDefDef(using
       q: Quotes
   )(
       s: q.reflect.Symbol,
       lookup: Expr[JSymbolLookup]
   ): Validated[List[String], Expr[Any]] =
      import quotes.reflect.{MethodType => MT, *}
      val paramNames =
         LazyList.iterate('a')(a => (a + 1).toChar).map(_.toString)

      val address =
         '{
            $lookup
               .lookup(${ Expr(s.name) })
               .orElseThrow(() =>
                  new Exception(s"Could not lookup ${${ Expr(s.name) }}")
               )
         }

      val ((inputNames, inputTypes), returnType) = s.tree match
         case DefDef(name, parameters, returnType, _) =>
            parameters
               .flatMap(_.params.collect { case ValDef(a, b, _) =>
                  Some(a) -> b.tpe.asType
               })
               .unzip -> returnType.tpe.asType

      wrappedMH(address, inputNames, inputTypes, returnType)

   end wrappedMHFromDefDef
end MethodHandleMacros
