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

   def wrappedMHFromDefDef(using
       q: Quotes
   )(s: q.reflect.Symbol, lookup: Expr[JSymbolLookup]): Expr[Any] =
      import quotes.reflect.{MethodType => MT, *}

      val address = '{ $lookup.lookup(${ Expr(s.name) }).orElseThrow }

      val addressSym = Symbol.newVal(
        Symbol.spliceOwner,
        "addr",
        TypeRepr.of[MemoryAddress],
        Flags.EmptyFlags,
        Symbol.noSymbol
      )

      val addressVd = ValDef(addressSym, Some(address.asTerm))

      val addressRef = Ref(addressSym).asExprOf[MemoryAddress]

      val (inputTypes, returnType) = s.tree match
         case DefDef(name, parameters, returnType, _) =>
            parameters.flatMap(_.params.collect { case ValDef(a, b, _) =>
               b.tpe.asType
            }) -> returnType.tpe.asType
      // val inputTypes = s.signature.paramSigs.collect { case s: String =>
      //    TypeIdent(Symbol.requiredClass(s)).tpe.asType
      // }

      val inputNativeInfo = inputTypes.map { case '[a] =>
         Expr.summonOrError[NativeInfo[a]]
      }

      val inputNISyms = inputNativeInfo.map(_ =>
         Symbol.newVal(
           Symbol.spliceOwner,
           "ni",
           TypeRepr.of[NativeInfo[?]],
           Flags.EmptyFlags,
           Symbol.noSymbol
         )
      )

      val inputNIVds = inputNISyms
         .zip(inputNativeInfo)
         .map((s, rhs) => ValDef(s, Some(rhs.asTerm)))

      val inputNIRefs =
         inputNISyms.map(Ref.apply).map(_.asExprOf[NativeInfo[?]])

      val (inputEmiVds, inputEmiRefs) = inputTypes.map { case '[a] =>
         val emi = Expr.summonOrError[Emigrator[a]]
         val sym = Symbol.newVal(
           Symbol.spliceOwner,
           "emi",
           TypeRepr.of[Emigrator[a]],
           Flags.EmptyFlags,
           Symbol.noSymbol
         )
         val vd = ValDef(sym, Some(emi.asTerm))
         val ref = Ref(sym)
         vd -> ref
      }.unzip

      // val returnType = TypeIdent(
      //   Symbol.requiredClass(s.signature.resultSig)
      // ).tpe

      val returnNativeInfo = returnType match
         case '[Unit] => None
         case '[r]    => Some(Expr.summonOrError[NativeInfo[r]])

      val retSym = returnNativeInfo.map(_ =>
         Symbol.newVal(
           Symbol.spliceOwner,
           "retNI",
           TypeRepr.of[NativeInfo[?]],
           Flags.EmptyFlags,
           Symbol.noSymbol
         )
      )

      val paramNames = LazyList.iterate("a")(a => s"a$a")
      val retValDef = retSym
         .zip(returnNativeInfo)
         .map((s, rhs) => ValDef(s, Some(rhs.asTerm)))

      val (returnImmiVD, returnImmiRef) = returnType match
         case '[r] =>
            val immi = Expr.summonOrError[Immigrator[r]]
            val sym = Symbol.newVal(
              Symbol.spliceOwner,
              "retImmi",
              TypeRepr.of[Immigrator[r]],
              Flags.EmptyFlags,
              Symbol.noSymbol
            )
            val vd = ValDef(sym, Some(immi.asTerm))
            val ref = Ref(sym)

            vd -> ref

      val retNIRef = retSym.map(Ref.apply).map(_.asExprOf[NativeInfo[?]])

      val mh = dc2(addressRef, inputNIRefs, retNIRef)

      val mhSym = Symbol.newVal(
        Symbol.spliceOwner,
        "mh",
        TypeRepr.of[MethodHandle],
        Flags.EmptyFlags,
        Symbol.noSymbol
      )

      val mhValDef = ValDef(mhSym, Some(mh.asTerm))

      val mhRef = Ref(mhSym).asExprOf[MethodHandle]

      val lmb = Lambda(
        Symbol.spliceOwner,
        MT(paramNames.take(inputTypes.size).toList)(
          _ => inputTypes.map { case '[a] => TypeRepr.of[a] },
          _ => returnType match { case '[r] => TypeRepr.of[r] }
        ),
        (owner, trees) =>
           returnType match {
              case '[r] =>
                 val retImmi = returnImmiRef.asExprOf[Immigrator[r]]
                 val callExpr = call[r](
                   mhRef,
                   inputTypes.zip(trees.zip(inputEmiRefs)).map {
                      case ('[a], (t, emi)) =>
                         '{
                            ${ emi.asExprOf[Emigrator[a]] }(${ t.asExprOf[a] })(
                              using localAllocator
                            )
                         }
                   }
                 )
                 '{
                    $retImmi($callExpr)
                 }.asTerm.changeOwner(owner)
           }
      )

      Block(
        List(
          addressVd,
          mhValDef,
          returnImmiVD
        ) ++ retValDef.toList ++ inputEmiVds ++ inputNIVds,
        lmb
      ).asExpr.tap(_.show.tap(report.error))
end MethodHandleMacros
